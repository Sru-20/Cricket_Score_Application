package com.boundary.api;

import java.util.List;

public final class MatchDtos {
    private MatchDtos() {}

    public record Team(String name, String code, String flag, int runs, int wickets, String overs) {}

    public record Player(
            String name,
            int runs,
            int balls,
            int fours,
            int sixes,
            Double economy,
            Integer wickets,
            String overs
    ) {}

    public record Commentary(String over, String text, String kind, String time) {}

    public record MatchView(
            String id,
            String status,
            String format,
            String venue,
            String series,
            Team homeTeam,
            Team awayTeam,
            String battingTeam,
            int target,
            double runRate,
            double requiredRunRate,
            String toss,
            Player batterOne,
            Player batterTwo,
            Player bowler,
            List<String> recentBalls,
            String updatedAt,
            List<Commentary> commentary
    ) {}

    public record BatterLine(String name, int runs, int balls, int fours, int sixes, String dismissal) {}

    public record BowlerLine(String name, String overs, int maidens, int runs, int wickets, double economy) {}

    public record Scorecard(String matchId, String battingTeam, List<BatterLine> batters, List<BowlerLine> bowlers) {}
}
