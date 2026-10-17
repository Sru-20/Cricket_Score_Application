package com.boundary.api;

import com.boundary.api.MatchDtos.BatterLine;
import com.boundary.api.MatchDtos.BowlerLine;
import com.boundary.api.MatchDtos.Commentary;
import com.boundary.api.MatchDtos.MatchView;
import com.boundary.api.MatchDtos.Player;
import com.boundary.api.MatchDtos.Scorecard;
import com.boundary.api.MatchDtos.Team;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ScoreService {
    private final ConcurrentHashMap<String, MatchState> matches = new ConcurrentHashMap<>();
    private final MatchUpdatePublisher updates;
    private final MatchSnapshotRepository snapshots;
    private final DateTimeFormatter clockFormat = DateTimeFormatter.ofPattern("h:mm a");

    public ScoreService(MatchUpdatePublisher updates, MatchSnapshotRepository snapshots) {
        this.updates = updates;
        this.snapshots = snapshots;
    }

    @PostConstruct
    public void loadMatches() {
        List<MatchView> saved = snapshots.findAll();
        if (saved.isEmpty()) {
            seedMatches();
            matches.values().forEach(match -> snapshots.save(match.toView()));
            return;
        }
        saved.stream().map(MatchState::fromView).forEach(match -> matches.put(match.id, match));
    }

    public List<MatchView> getMatches() {
        return matches.values().stream().map(MatchState::toView)
                .sorted(Comparator.comparing(MatchView::status).thenComparing(MatchView::id))
                .toList();
    }

    public MatchView getMatch(String id) {
        return requireMatch(id).toView();
    }

    public Scorecard getScorecard(String id) {
        MatchState match = requireMatch(id);
        synchronized (match) {
            String battingTeam = match.battingTeam.equals(match.home.code) ? match.home.name : match.away.name;
            List<BatterLine> batters = List.of(
                    new BatterLine(match.activeBatter.name, match.activeBatter.runs, match.activeBatter.balls, match.activeBatter.fours, match.activeBatter.sixes, "not out"),
                    new BatterLine(match.otherBatter.name, match.otherBatter.runs, match.otherBatter.balls, match.otherBatter.fours, match.otherBatter.sixes, "not out")
            );
            List<BowlerLine> bowlers = List.of(new BowlerLine(
                    match.bowler.name, match.bowlerOversText, 0, match.bowlerRuns,
                    match.bowlerWickets, match.bowlerEconomy
            ));
            return new Scorecard(match.id, battingTeam, batters, bowlers);
        }
    }

    public List<Commentary> getCommentary(String id) {
        return requireMatch(id).toView().commentary();
    }

    public MatchView recordBall(String id, int runs, boolean wicket) {
        if (runs < 0 || runs > 6 || (runs == 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Runs must be between 0 and 6, excluding 5.");
        }
        MatchState match = requireMatch(id);
        synchronized (match) {
            applyBall(match, runs, wicket);
            MatchView update = match.toView();
            snapshots.saveDelivery(update, runs, wicket, match.commentary.peekFirst());
            updates.send("/topic/matches/" + id, update);
            updates.send("/topic/matches", getMatches());
            return update;
        }
    }

    @Scheduled(fixedDelayString = "${boundary.simulation.interval-ms:8000}")
    public void simulateBall() {
        matches.values().stream()
                .filter(match -> "LIVE".equals(match.status))
                .forEach(match -> {
                    MatchView update;
                    synchronized (match) {
                        int[] outcomes = {0, 1, 1, 1, 2, 2, 3, 4, 4, 6};
                        int runs = outcomes[ThreadLocalRandom.current().nextInt(outcomes.length)];
                        boolean wicket = ThreadLocalRandom.current().nextInt(22) == 0;
                        applyBall(match, runs, wicket);
                        update = match.toView();
                        snapshots.saveDelivery(update, runs, wicket, match.commentary.peekFirst());
                    }
                    updates.send("/topic/matches/" + match.id, update);
                });
        updates.send("/topic/matches", getMatches());
    }

    private void applyBall(MatchState match, int runs, boolean wicket) {
        if (!"LIVE".equals(match.status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Scoring is only available for live matches.");
        }
        int completedBalls = match.completedBalls + 1;
        match.completedBalls = completedBalls;
        match.runs += runs;
        match.bowlerRuns += runs;
        PlayerState batter = match.activeBatter;
        batter.balls++;
        batter.runs += runs;
        if (runs == 4) batter.fours++;
        if (runs == 6) batter.sixes++;
        if (wicket && match.wickets < 10) {
            match.wickets++;
            match.bowlerWickets++;
            String out = batter.name;
            if (match.wickets < 10) {
                match.activeBatter = match.otherBatter;
                match.otherBatter = new PlayerState("Batter " + (match.wickets + 3), 0, 0, 0, 0);
            }
            match.commentary.addFirst(new Commentary(overLabel(completedBalls),
                    match.wickets == 10 ? "All out! " + out + " is out." :
                            "Wicket! " + out + " is out. " + match.activeBatter.name + " comes to the crease.",
                    "wicket", now()));
        } else {
            String event = runs == 6 ? "SIX! " + batter.name + " sends it over the rope." :
                    runs == 4 ? "FOUR! " + batter.name + " finds the boundary." :
                            runs == 0 ? "No run, good delivery." : batter.name + " takes " + runs + ".";
            match.commentary.addFirst(new Commentary(overLabel(completedBalls), event, runs >= 4 ? "boundary" : "normal", now()));
        }
        while (match.commentary.size() > 30) match.commentary.removeLast();
        match.lastBall = runs == 0 ? "0" : Integer.toString(runs);
        match.recentBalls.addLast(match.lastBall);
        while (match.recentBalls.size() > 6) match.recentBalls.removeFirst();
        if (completedBalls % 6 == 0) {
            match.bowlerOvers++;
            match.bowlerRunsCurrentOver = 0;
        } else {
            match.bowlerRunsCurrentOver += runs;
        }
        match.bowlerEconomy = match.bowlerOvers == 0 ? match.bowlerRunsCurrentOver : (double) match.bowlerRuns / (match.bowlerOvers + (completedBalls % 6) / 6.0);
        match.bowlerOversText = match.bowlerOvers + "." + (completedBalls % 6);
        match.updatedAt = now();
        int inningsLimit = match.format.contains("T20") ? 120 :
                match.format.contains("ODI") ? 300 : Integer.MAX_VALUE;
        if (match.wickets >= 10 || completedBalls >= inningsLimit) match.status = "COMPLETED";
    }

    private MatchState requireMatch(String id) {
        MatchState match = matches.get(id);
        if (match == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found: " + id);
        return match;
    }

    private String now() {
        return LocalTime.now().format(clockFormat);
    }

    private String overLabel(int balls) {
        return (balls - 1) / 6 + "." + ((balls - 1) % 6 + 1);
    }

    private void seedMatches() {
        MatchState india = new MatchState("ind-aus-2026", "LIVE", "3rd ODI · 50 overs",
                "Narendra Modi Stadium, Ahmedabad", "AUSTRALIA TOUR OF INDIA",
                new TeamState("India", "IND", "🇮🇳", 287, 4, 260),
                new TeamState("Australia", "AUS", "🇦🇺", 0, 0, 0), "IND", "India elected to bat",
                new PlayerState("Virat Kohli", 86, 74, 8, 2), new PlayerState("KL Rahul", 42, 35, 3, 1),
                new PlayerState("Josh Hazlewood", 0, 0, 0, 0));
        india.completedBalls = 260;
        india.bowlerRuns = 48;
        india.bowlerWickets = 1;
        india.bowlerOvers = 9;
        india.bowlerRunsCurrentOver = 2;
        india.bowlerEconomy = 5.14;
        india.bowlerOversText = "9.2";
        india.runRate = 6.62;
        india.recentBalls.addAll(List.of("1", "4", "1", "6", "0", "2"));
        india.commentary.addAll(List.of(
                new Commentary("43.2", "Kohli opens the face and guides it past point for a single. A measured knock, inching toward a hundred.", "normal", now()),
                new Commentary("43.1", "Rahul gets hold of that one! Sails over deep midwicket for SIX.", "boundary", now()),
                new Commentary("42.6", "A tidy over from Hazlewood. Just five from it.", "normal", now())
        ));
        matches.put(india.id, india);

        MatchState england = new MatchState("eng-sa-2026", "LIVE", "1st Test · Day 2", "Lord’s, London",
                "ENGLAND V SOUTH AFRICA", new TeamState("England", "ENG", "🏴", 196, 6, 352),
                new TeamState("South Africa", "SA", "🇿🇦", 312, 10, 553), "ENG",
                "South Africa elected to bat", new PlayerState("Ben Stokes", 38, 72, 4, 0),
                new PlayerState("Jamie Smith", 21, 34, 2, 0), new PlayerState("Kagiso Rabada", 0, 0, 0, 0));
        england.completedBalls = 352;
        england.bowlerRuns = 42;
        england.bowlerWickets = 2;
        england.bowlerOvers = 15;
        england.bowlerEconomy = 2.8;
        england.bowlerOversText = "15.0";
        england.runRate = 3.34;
        england.recentBalls.addAll(List.of("0", "1", "0", "0", "4", "0"));
        england.commentary.add(new Commentary("58.4", "Stokes nudges it into the leg side for one. England are 116 behind.", "normal", now()));
        matches.put(england.id, england);

        MatchState pakistan = new MatchState("pak-wi-2026", "UPCOMING", "2nd T20I · 7:30 PM",
                "Gaddafi Stadium, Lahore", "WEST INDIES TOUR OF PAKISTAN",
                new TeamState("Pakistan", "PAK", "🇵🇰", 0, 0, 0),
                new TeamState("West Indies", "WI", "🏴‍☠️", 0, 0, 0), "PAK", "Match starts at 7:30 PM",
                new PlayerState("Match preview", 0, 0, 0, 0), new PlayerState("", 0, 0, 0, 0),
                new PlayerState("", 0, 0, 0, 0));
        matches.put(pakistan.id, pakistan);
    }

    private static final class MatchState {
        private final String id;
        private String status;
        private final String format;
        private final String venue;
        private final String series;
        private final TeamState home;
        private final TeamState away;
        private final String battingTeam;
        private final String toss;
        private PlayerState activeBatter;
        private PlayerState otherBatter;
        private final PlayerState bowler;
        private final Deque<String> recentBalls = new ArrayDeque<>();
        private final Deque<Commentary> commentary = new ArrayDeque<>();
        private int runs;
        private int wickets;
        private int completedBalls;
        private int bowlerRuns;
        private int bowlerWickets;
        private int bowlerOvers;
        private int bowlerRunsCurrentOver;
        private double bowlerEconomy;
        private double runRate;
        private String bowlerOversText = "0.0";
        private String updatedAt = "Just now";
        private String lastBall = "0";

        private MatchState(String id, String status, String format, String venue, String series,
                           TeamState home, TeamState away, String battingTeam, String toss,
                           PlayerState batterOne, PlayerState batterTwo, PlayerState bowler) {
            this.id = id;
            this.status = status;
            this.format = format;
            this.venue = venue;
            this.series = series;
            this.home = home;
            this.away = away;
            this.battingTeam = battingTeam;
            this.toss = toss;
            this.activeBatter = batterOne;
            this.otherBatter = batterTwo;
            this.bowler = bowler;
            TeamState batting = battingTeam.equals(home.code) ? home : away;
            this.runs = batting.runs;
            this.wickets = batting.wickets;
            this.completedBalls = batting.balls;
            this.runRate = batting.balls == 0 ? 0 : (double) batting.runs / (batting.balls / 6.0);
        }

        private static MatchState fromView(MatchView view) {
            TeamState home = fromTeam(view.homeTeam());
            TeamState away = fromTeam(view.awayTeam());
            PlayerState batterOne = fromPlayer(view.batterOne());
            PlayerState batterTwo = fromPlayer(view.batterTwo());
            PlayerState bowler = fromPlayer(view.bowler());
            MatchState match = new MatchState(view.id(), view.status(), view.format(), view.venue(), view.series(),
                    home, away, view.battingTeam(), view.toss(), batterOne, batterTwo, bowler);
            TeamState batting = view.battingTeam().equals(home.code) ? home : away;
            match.runs = batting.runs;
            match.wickets = batting.wickets;
            match.completedBalls = batting.balls;
            match.runRate = view.runRate();
            match.bowlerRuns = bowler.runs;
            match.bowlerWickets = view.bowler().wickets() == null ? 0 : view.bowler().wickets();
            match.bowlerOversText = view.bowler().overs() == null ? "0.0" : view.bowler().overs();
            match.bowlerOvers = completedBalls(view.bowler().overs()) / 6;
            match.bowlerRunsCurrentOver = match.bowlerOversText.contains(".")
                    ? Integer.parseInt(match.bowlerOversText.substring(match.bowlerOversText.indexOf('.') + 1)) : 0;
            match.bowlerEconomy = view.bowler().economy() == null ? 0 : view.bowler().economy();
            match.updatedAt = view.updatedAt();
            match.recentBalls.addAll(view.recentBalls());
            match.commentary.addAll(view.commentary());
            return match;
        }

        private static TeamState fromTeam(Team team) {
            return new TeamState(team.name(), team.code(), team.flag(), team.runs(), team.wickets(), completedBalls(team.overs()));
        }

        private static PlayerState fromPlayer(Player player) {
            return new PlayerState(player.name(), player.runs(), player.balls(), player.fours(), player.sixes());
        }

        private static int completedBalls(String overs) {
            if (overs == null || overs.isBlank()) return 0;
            String[] parts = overs.split("\\.", 2);
            return Integer.parseInt(parts[0]) * 6 + (parts.length == 2 ? Integer.parseInt(parts[1]) : 0);
        }

        private synchronized MatchView toView() {
            TeamState batting = battingTeam.equals(home.code) ? home : away;
            batting.runs = runs;
            batting.wickets = wickets;
            batting.balls = completedBalls;
            double crr = completedBalls == 0 ? 0 : (double) runs / (completedBalls / 6.0);
            runRate = crr;
            Player batterOne = toPlayer(activeBatter);
            Player batterTwo = toPlayer(otherBatter);
            Player bowlerView = new Player(bowler.name, bowlerRuns, 0, 0, 0,
                    bowlerEconomy, bowlerWickets, bowlerOversText);
            return new MatchView(id, status, format, venue, series,
                    toTeam(home), toTeam(away), battingTeam, 0, crr, 0, toss,
                    batterOne, batterTwo, bowlerView, List.copyOf(recentBalls),
                    updatedAt, List.copyOf(commentary));
        }

        private static Player toPlayer(PlayerState player) {
            return new Player(player.name, player.runs, player.balls, player.fours, player.sixes, null, null, null);
        }

        private static Team toTeam(TeamState team) {
            String overs = (team.balls / 6) + "." + (team.balls % 6);
            return new Team(team.name, team.code, team.flag, team.runs, team.wickets, overs);
        }
    }

    private static final class TeamState {
        private final String name;
        private final String code;
        private final String flag;
        private int runs;
        private int wickets;
        private int balls;

        private TeamState(String name, String code, String flag, int runs, int wickets, int balls) {
            this.name = name;
            this.code = code;
            this.flag = flag;
            this.runs = runs;
            this.wickets = wickets;
            this.balls = balls;
        }
    }

    private static final class PlayerState {
        private final String name;
        private int runs;
        private int balls;
        private int fours;
        private int sixes;

        private PlayerState(String name, int runs, int balls, int fours, int sixes) {
            this.name = name;
            this.runs = runs;
            this.balls = balls;
            this.fours = fours;
            this.sixes = sixes;
        }
    }
}
