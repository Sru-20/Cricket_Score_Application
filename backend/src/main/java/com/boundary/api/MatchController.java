package com.boundary.api;

import com.boundary.api.MatchDtos.Commentary;
import com.boundary.api.MatchDtos.MatchView;
import com.boundary.api.MatchDtos.Scorecard;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/matches")
public class MatchController {
    private final ScoreService scores;

    public MatchController(ScoreService scores) {
        this.scores = scores;
    }

    @GetMapping
    public List<MatchView> matches() {
        return scores.getMatches();
    }

    @GetMapping("/{id}")
    public MatchView match(@PathVariable String id) {
        return scores.getMatch(id);
    }

    @GetMapping("/{id}/scorecard")
    public Scorecard scorecard(@PathVariable String id) {
        return scores.getScorecard(id);
    }

    @GetMapping("/{id}/commentary")
    public List<Commentary> commentary(@PathVariable String id) {
        return scores.getCommentary(id);
    }

    @PostMapping("/{id}/balls")
    public MatchView recordBall(
            @PathVariable String id,
            @RequestParam @Min(0) @Max(6) int runs,
            @RequestParam(defaultValue = "false") boolean wicket
    ) {
        return scores.recordBall(id, runs, wicket);
    }
}
