import java.util.*;

public class FeatureExtractor {
    private final PlayerHistoryManager historyManager;
    private final Map<String, Integer> surfaceEncoding = Map.of(
            "Hard", 0, "Clay", 1, "Grass", 2, "Carpet", 3
    );
    private final Map<String, Integer> handEncoding = Map.of(
            "R", 0, "L", 1, "U", 2
    );
    private final Map<String, Integer> tourneyLevelEncoding = Map.of(
            "G", 0, "M", 1, "A", 2, "B", 3, "F", 4, "D", 5
    );
    private final Map<String, Integer> roundEncoding = Map.of(
            "R128", 0, "R64", 1, "R32", 2, "R16", 3, "QF", 4, "SF", 5, "F", 6
    );
    private final Map<String, Integer> entryEncoding = Map.of(
            "Q", 0, "WC", 1, "LL", 2, "SE", 3, "", 4
    );

    public FeatureExtractor(PlayerHistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    public FeatureVector extractFeatures(Match match, boolean player1IsWinner) {
        if (match == null) {
            throw new IllegalArgumentException("Match cannot be null");
        }

        Player player1 = player1IsWinner ? match.getWinner() : match.getLoser();
        Player player2 = player1IsWinner ? match.getLoser() : match.getWinner();

        if (player1 == null || player2 == null) {
            throw new IllegalArgumentException("Match must have both winner and loser");
        }

        MatchStats stats1 = player1IsWinner ? match.getWinnerStats() : match.getLoserStats();
        MatchStats stats2 = player1IsWinner ? match.getLoserStats() : match.getWinnerStats();

        List<Double> features = new ArrayList<>();
        List<String> featureNames = new ArrayList<>();

        // Tournament features
        addFeature(features, featureNames, "surface", encodeSurface(match.getSurface()));
        addFeature(features, featureNames, "draw_size", safeDouble(match.getDrawSize()));
        addFeature(features, featureNames, "tourney_level", encodeTourneyLevel(match.getTourneyLevel()));
        addFeature(features, featureNames, "best_of", safeDouble(match.getBestOf()));
        addFeature(features, featureNames, "round", encodeRound(match.getRound()));

        // Date features
        addDateFeatures(features, featureNames, match.getTourneyDate());

        // Player features
        addPlayerFeatures(features, featureNames, player1, "p1_", match.getSurface());
        addPlayerFeatures(features, featureNames, player2, "p2_", match.getSurface());

        // Head-to-head features
        String p1Id = player1.getPlayerId();
        String p2Id = player2.getPlayerId();
        if (p1Id != null && p2Id != null) {
            addHeadToHeadFeatures(features, featureNames, p1Id, p2Id);
        } else {
            addFeature(features, featureNames, "h2h_win_rate", 0.5);
            addFeature(features, featureNames, "h2h_total_matches", 0.0);
        }

        // Match statistics (if available)
        if (stats1 != null && stats2 != null) {
            addMatchStatsFeatures(features, featureNames, stats1, stats2);
        }

        // Ranking comparison features
        addRankingFeatures(features, featureNames, player1.getRank(), player2.getRank());

        return new FeatureVector(features, featureNames);
    }

    private void addFeature(List<Double> features, List<String> names, String name, Double value) {
        Double safeValue = value;
        if (value == null || value.isNaN() || value.isInfinite()) {
            safeValue = 0.0; // Default value for missing data
        }
        features.add(safeValue);
        names.add(name);
    }

    private void addDateFeatures(List<Double> features, List<String> names, Integer tourneyDate) {
        if (tourneyDate != null && tourneyDate > 0) {
            int year = tourneyDate / 10000;
            int month = (tourneyDate / 100) % 100;
            int day = tourneyDate % 100;

            addFeature(features, names, "year", (double) year);
            addFeature(features, names, "month", (double) month);
            addFeature(features, names, "day_of_month", (double) day);
        } else {
            addFeature(features, names, "year", 2020.0); // Default year
            addFeature(features, names, "month", 6.0);   // Default month
            addFeature(features, names, "day_of_month", 15.0); // Default day
        }
    }

    private void addPlayerFeatures(List<Double> features, List<String> names, Player player, String prefix, String surface) {
        if (player == null) {
            // Add default values for all player features
            addFeature(features, names, prefix + "seed", 0.0);
            addFeature(features, names, prefix + "entry", 4.0);
            addFeature(features, names, prefix + "hand", 0.0);
            addFeature(features, names, prefix + "height", 180.0);
            addFeature(features, names, prefix + "age", 25.0);
            addFeature(features, names, prefix + "rank", 100.0);
            addFeature(features, names, prefix + "rank_points", 1000.0);
            addFeature(features, names, prefix + "recent_form_5", 0.5);
            addFeature(features, names, prefix + "recent_form_10", 0.5);
            addFeature(features, names, prefix + "surface_form_10", 0.5);
            addFeature(features, names, prefix + "surface_elo", 1500.0);
            addFeature(features, names, prefix + "total_matches", 0.0);
            addFeature(features, names, prefix + "career_win_rate", 0.5);
            return;
        }

        // Basic player attributes
        addFeature(features, names, prefix + "seed", player.getSeed() != null ? player.getSeed().doubleValue() : 0.0);
        addFeature(features, names, prefix + "entry", encodeEntry(player.getEntry()));

        String hand = player.getHand();
        if (hand == null || hand.isEmpty()) hand = "R";
        addFeature(features, names, prefix + "hand", encodeHand(hand));

        addFeature(features, names, prefix + "height", player.getHeight() != null ? player.getHeight().doubleValue() : 180.0);
        addFeature(features, names, prefix + "age", player.getAge() != null ? player.getAge() : 25.0);
        addFeature(features, names, prefix + "rank", player.getRank() != null ? player.getRank().doubleValue() : 100.0);
        addFeature(features, names, prefix + "rank_points", player.getRankPoints() != null ? player.getRankPoints().doubleValue() : 1000.0);

        // Historical performance features
        String playerId = player.getPlayerId();
        PlayerHistory history = null;
        if (playerId != null && historyManager != null) {
            history = historyManager.getPlayerHistory(playerId);
        }

        if (history != null) {
            addFeature(features, names, prefix + "recent_form_5", history.getRecentForm(5));
            addFeature(features, names, prefix + "recent_form_10", history.getRecentForm(10));
            addFeature(features, names, prefix + "surface_form_10", history.getSurfaceForm(surface, 10));
            addFeature(features, names, prefix + "surface_elo", history.getSurfaceElo(surface));
            addFeature(features, names, prefix + "total_matches", (double) history.getTotalMatches());
            double winRate = history.getTotalMatches() > 0 ? (double) history.getTotalWins() / history.getTotalMatches() : 0.5;
            addFeature(features, names, prefix + "career_win_rate", winRate);
        } else {
            // Default values for new players or missing history
            addFeature(features, names, prefix + "recent_form_5", 0.5);
            addFeature(features, names, prefix + "recent_form_10", 0.5);
            addFeature(features, names, prefix + "surface_form_10", 0.5);
            addFeature(features, names, prefix + "surface_elo", 1500.0);
            addFeature(features, names, prefix + "total_matches", 0.0);
            addFeature(features, names, prefix + "career_win_rate", 0.5);
        }
    }

    private void addHeadToHeadFeatures(List<Double> features, List<String> names, String player1Id, String player2Id) {
        HeadToHeadRecord h2h = null;
        if (historyManager != null) {
            h2h = historyManager.getHeadToHeadRecord(player1Id, player2Id);
        }

        if (h2h != null && h2h.getTotalMatches() > 0) {
            addFeature(features, names, "h2h_win_rate", h2h.getWinRate());
            addFeature(features, names, "h2h_total_matches", (double) h2h.getTotalMatches());
        } else {
            addFeature(features, names, "h2h_win_rate", 0.5);
            addFeature(features, names, "h2h_total_matches", 0.0);
        }
    }

    private void addMatchStatsFeatures(List<Double> features, List<String> names, MatchStats stats1, MatchStats stats2) {
        // Player 1 serving stats
        addFeature(features, names, "p1_aces", stats1.getAces() != null ? stats1.getAces().doubleValue() : 0.0);
        addFeature(features, names, "p1_double_faults", stats1.getDoubleFaults() != null ? stats1.getDoubleFaults().doubleValue() : 0.0);
        addFeature(features, names, "p1_first_serve_pct", stats1.getFirstServePercentage() != null ? stats1.getFirstServePercentage() : 0.6);
        addFeature(features, names, "p1_first_serve_win_pct", stats1.getFirstServeWinPercentage() != null ? stats1.getFirstServeWinPercentage() : 0.7);
        addFeature(features, names, "p1_second_serve_win_pct", stats1.getSecondServeWinPercentage() != null ? stats1.getSecondServeWinPercentage() : 0.5);
        addFeature(features, names, "p1_ace_rate", stats1.getAceRate() != null ? stats1.getAceRate() : 0.05);
        addFeature(features, names, "p1_df_rate", stats1.getDoubleFaultRate() != null ? stats1.getDoubleFaultRate() : 0.03);
        addFeature(features, names, "p1_bp_save_pct", stats1.getBreakPointSavePercentage() != null ? stats1.getBreakPointSavePercentage() : 0.6);

        // Player 2 serving stats
        addFeature(features, names, "p2_aces", stats2.getAces() != null ? stats2.getAces().doubleValue() : 0.0);
        addFeature(features, names, "p2_double_faults", stats2.getDoubleFaults() != null ? stats2.getDoubleFaults().doubleValue() : 0.0);
        addFeature(features, names, "p2_first_serve_pct", stats2.getFirstServePercentage() != null ? stats2.getFirstServePercentage() : 0.6);
        addFeature(features, names, "p2_first_serve_win_pct", stats2.getFirstServeWinPercentage() != null ? stats2.getFirstServeWinPercentage() : 0.7);
        addFeature(features, names, "p2_second_serve_win_pct", stats2.getSecondServeWinPercentage() != null ? stats2.getSecondServeWinPercentage() : 0.5);
        addFeature(features, names, "p2_ace_rate", stats2.getAceRate() != null ? stats2.getAceRate() : 0.05);
        addFeature(features, names, "p2_df_rate", stats2.getDoubleFaultRate() != null ? stats2.getDoubleFaultRate() : 0.03);
        addFeature(features, names, "p2_bp_save_pct", stats2.getBreakPointSavePercentage() != null ? stats2.getBreakPointSavePercentage() : 0.6);
    }

    private void addRankingFeatures(List<Double> features, List<String> names, Integer rank1, Integer rank2) {
        if (rank1 != null && rank2 != null && rank1 > 0 && rank2 > 0) {
            double rankDiff = Math.log(rank1 + 1) - Math.log(rank2 + 1);
            double rankRatio = (double) rank2 / (rank1 + 1);
            double avgRankQuality = 2.0 / (Math.sqrt(rank1) + Math.sqrt(rank2));

            addFeature(features, names, "rank_diff_log", rankDiff);
            addFeature(features, names, "rank_ratio", rankRatio);
            addFeature(features, names, "avg_rank_quality", avgRankQuality);
        } else {
            addFeature(features, names, "rank_diff_log", 0.0);
            addFeature(features, names, "rank_ratio", 1.0);
            addFeature(features, names, "avg_rank_quality", 0.1);
        }
    }

    // Encoding helper methods
    private Double encodeSurface(String surface) {
        if (surface == null || surface.isEmpty()) surface = "Hard";
        return surfaceEncoding.getOrDefault(surface, 0).doubleValue();
    }

    private Double encodeHand(String hand) {
        return handEncoding.getOrDefault(hand, 0).doubleValue();
    }

    private Double encodeTourneyLevel(String level) {
        if (level == null || level.isEmpty()) level = "G";
        return tourneyLevelEncoding.getOrDefault(level, 0).doubleValue();
    }

    private Double encodeRound(String round) {
        if (round == null || round.isEmpty()) round = "R128";
        return roundEncoding.getOrDefault(round, 0).doubleValue();
    }

    private Double encodeEntry(String entry) {
        Integer encoded = entryEncoding.get(entry != null ? entry : "");
        return encoded != null ? encoded.doubleValue() : 4.0;
    }

    private Double safeDouble(Integer value) {
        return value != null ? value.doubleValue() : 0.0;
    }
}