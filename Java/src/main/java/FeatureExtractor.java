import java.util.*;

/**
 * Converts matches into numerical feature vectors for machine learning
 */
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

    /**
     * Extract features for a player pair (treating player1 as potential winner)
     */
    public FeatureVector extractFeatures(Match match, boolean player1IsWinner) {
        Player player1 = player1IsWinner ? match.getWinner() : match.getLoser();
        Player player2 = player1IsWinner ? match.getLoser() : match.getWinner();
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

        // Date features (convert YYYYMMDD to more useful features)
        addDateFeatures(features, featureNames, match.getTourneyDate());

        // Player 1 features
        addPlayerFeatures(features, featureNames, player1, "p1_", match.getSurface());

        // Player 2 features
        addPlayerFeatures(features, featureNames, player2, "p2_", match.getSurface());

        // Head-to-head features
        addHeadToHeadFeatures(features, featureNames, player1.getPlayerId(), player2.getPlayerId());

        // Match statistics (if available)
        if (stats1 != null && stats2 != null) {
            addMatchStatsFeatures(features, featureNames, stats1, stats2);
        }

        // Ranking comparison features
        addRankingFeatures(features, featureNames, player1.getRank(), player2.getRank());

        return new FeatureVector(features, featureNames);
    }

    private void addFeature(List<Double> features, List<String> names, String name, Double value) {
        features.add(value != null ? value : Double.NaN);
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
            addFeature(features, names, "year", null);
            addFeature(features, names, "month", null);
            addFeature(features, names, "day_of_month", null);
        }
    }

    private void addPlayerFeatures(List<Double> features, List<String> names, Player player, String prefix, String surface) {
        addFeature(features, names, prefix + "seed", safeDouble(player.getSeed()));
        addFeature(features, names, prefix + "entry", encodeEntry(player.getEntry()));
        addFeature(features, names, prefix + "hand", encodeHand(player.getHand()));
        addFeature(features, names, prefix + "height", safeDouble(player.getHeight()));
        addFeature(features, names, prefix + "age", player.getAge());
        addFeature(features, names, prefix + "rank", safeDouble(player.getRank()));
        addFeature(features, names, prefix + "rank_points", safeDouble(player.getRankPoints()));

        // Historical performance features
        PlayerHistory history = historyManager.getPlayerHistory(player.getPlayerId());
        if (history != null) {
            addFeature(features, names, prefix + "recent_form_5", history.getRecentForm(5));
            addFeature(features, names, prefix + "recent_form_10", history.getRecentForm(10));
            addFeature(features, names, prefix + "surface_form_10", history.getSurfaceForm(surface, 10));
            addFeature(features, names, prefix + "surface_elo", history.getSurfaceElo(surface));
            addFeature(features, names, prefix + "total_matches", (double) history.getTotalMatches());
            addFeature(features, names, prefix + "career_win_rate",
                    history.getTotalMatches() > 0 ? (double) history.getTotalWins() / history.getTotalMatches() : 0.5);
        } else {
            // Default values for new players
            addFeature(features, names, prefix + "recent_form_5", 0.5);
            addFeature(features, names, prefix + "recent_form_10", 0.5);
            addFeature(features, names, prefix + "surface_form_10", 0.5);
            addFeature(features, names, prefix + "surface_elo", 1500.0);
            addFeature(features, names, prefix + "total_matches", 0.0);
            addFeature(features, names, prefix + "career_win_rate", 0.5);
        }
    }

    private void addHeadToHeadFeatures(List<Double> features, List<String> names, String player1Id, String player2Id) {
        HeadToHeadRecord h2h = historyManager.getHeadToHeadRecord(player1Id, player2Id);
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
        addFeature(features, names, "p1_aces", safeDouble(stats1.getAces()));
        addFeature(features, names, "p1_double_faults", safeDouble(stats1.getDoubleFaults()));
        addFeature(features, names, "p1_first_serve_pct", stats1.getFirstServePercentage());
        addFeature(features, names, "p1_first_serve_win_pct", stats1.getFirstServeWinPercentage());
        addFeature(features, names, "p1_second_serve_win_pct", stats1.getSecondServeWinPercentage());
        addFeature(features, names, "p1_ace_rate", stats1.getAceRate());
        addFeature(features, names, "p1_df_rate", stats1.getDoubleFaultRate());
        addFeature(features, names, "p1_bp_save_pct", stats1.getBreakPointSavePercentage());

        // Player 2 serving stats
        addFeature(features, names, "p2_aces", safeDouble(stats2.getAces()));
        addFeature(features, names, "p2_double_faults", safeDouble(stats2.getDoubleFaults()));
        addFeature(features, names, "p2_first_serve_pct", stats2.getFirstServePercentage());
        addFeature(features, names, "p2_first_serve_win_pct", stats2.getFirstServeWinPercentage());
        addFeature(features, names, "p2_second_serve_win_pct", stats2.getSecondServeWinPercentage());
        addFeature(features, names, "p2_ace_rate", stats2.getAceRate());
        addFeature(features, names, "p2_df_rate", stats2.getDoubleFaultRate());
        addFeature(features, names, "p2_bp_save_pct", stats2.getBreakPointSavePercentage());
    }

    private void addRankingFeatures(List<Double> features, List<String> names, Integer rank1, Integer rank2) {
        if (rank1 != null && rank2 != null && rank1 > 0 && rank2 > 0) {
            // Ranking difference (log scale for better distribution)
            double rankDiff = Math.log(rank2 + 1) - Math.log(rank1 + 1);
            addFeature(features, names, "rank_diff_log", rankDiff);

            // Ranking ratio
            double rankRatio = (double) rank2 / (rank1 + 1);
            addFeature(features, names, "rank_ratio", rankRatio);

            // Average ranking quality (inverse of geometric mean)
            double avgRankQuality = 2.0 / (Math.sqrt(rank1) + Math.sqrt(rank2));
            addFeature(features, names, "avg_rank_quality", avgRankQuality);
        } else {
            addFeature(features, names, "rank_diff_log", null);
            addFeature(features, names, "rank_ratio", null);
            addFeature(features, names, "avg_rank_quality", null);
        }
    }

    // Encoding helper methods
    private Double encodeSurface(String surface) {
        Integer encoded = surfaceEncoding.get(surface);
        return encoded != null ? encoded.doubleValue() : null;
    }

    private Double encodeHand(String hand) {
        Integer encoded = handEncoding.get(hand);
        return encoded != null ? encoded.doubleValue() : null;
    }

    private Double encodeTourneyLevel(String level) {
        Integer encoded = tourneyLevelEncoding.get(level);
        return encoded != null ? encoded.doubleValue() : null;
    }

    private Double encodeRound(String round) {
        Integer encoded = roundEncoding.get(round);
        return encoded != null ? encoded.doubleValue() : null;
    }

    private Double encodeEntry(String entry) {
        Integer encoded = entryEncoding.get(entry != null ? entry : "");
        return encoded != null ? encoded.doubleValue() : null;
    }

    private Double safeDouble(Integer value) {
        return value != null ? value.doubleValue() : null;
    }
}
