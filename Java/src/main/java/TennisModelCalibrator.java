import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive calibration system for tennis match predictions
 * Addresses the key issues identified in the feature analysis
 */
public class TennisModelCalibrator {

    // Calibration parameters - adjust these based on analysis
    private static final double RANK_SENSITIVITY_FACTOR = 0.8;
    private static final double SURFACE_IMPACT_FACTOR = 0.15;
    private static final double H2H_DECAY_FACTOR = 0.95;
    private static final double FORM_WEIGHT_RECENT = 0.7;
    private static final double FORM_WEIGHT_SURFACE = 0.3;

    // ELO parameters
    private static final double BASE_ELO = 1500.0;
    private static final double K_FACTOR_HIGH_RANK = 20.0;
    private static final double K_FACTOR_LOW_RANK = 32.0;
    private static final double ELO_FLOOR = 1000.0;
    private static final double ELO_CEILING = 2500.0;

    private final PlayerHistoryManager historyManager;
    private final Map<String, Double> surfaceAdjustments;

    public TennisModelCalibrator(PlayerHistoryManager historyManager) {
        this.historyManager = historyManager;
        this.surfaceAdjustments = initializeSurfaceAdjustments();
    }

    private Map<String, Double> initializeSurfaceAdjustments() {
        Map<String, Double> adjustments = new HashMap<>();
        adjustments.put("Hard", 1.0);    // Baseline
        adjustments.put("Clay", 1.2);    // More specialist surface
        adjustments.put("Grass", 1.3);   // Most specialist surface
        adjustments.put("Carpet", 1.1);  // Rare surface
        return adjustments;
    }

    /**
     * Main calibration method - returns calibrated win probability
     */
    public double getCalibratedPrediction(Match match, boolean player1IsWinner) {
        Player player1 = player1IsWinner ? match.getWinner() : match.getLoser();
        Player player2 = player1IsWinner ? match.getLoser() : match.getWinner();

        // Get base probability from ranking difference
        double baseProbability = calculateRankingProbability(player1, player2);

        // Apply surface-specific adjustments
        double surfaceAdjustedProb = applySurfaceCalibration(baseProbability, player1, player2, match.getSurface());

        // Apply form adjustments
        double formAdjustedProb = applyFormCalibration(surfaceAdjustedProb, player1, player2, match.getSurface());

        // Apply head-to-head adjustments
        double h2hAdjustedProb = applyHeadToHeadCalibration(formAdjustedProb, player1, player2);

        // Apply tournament context adjustments
        double finalProbability = applyTournamentCalibration(h2hAdjustedProb, match);

        // Ensure probability is within reasonable bounds
        return Math.max(0.05, Math.min(0.95, finalProbability));
    }

    /**
     * Calculate base probability from ranking difference with improved sensitivity
     */
    private double calculateRankingProbability(Player player1, Player player2) {
        Integer rank1 = player1.getRank();
        Integer rank2 = player2.getRank();

        // Handle missing ranks
        if (rank1 == null || rank1 <= 0) rank1 = 500;
        if (rank2 == null || rank2 <= 0) rank2 = 500;

        // Use logarithmic scale for ranking difference with calibrated sensitivity
        double logRank1 = Math.log(rank1);
        double logRank2 = Math.log(rank2);
        double rankDiff = (logRank2 - logRank1) * RANK_SENSITIVITY_FACTOR;

        // Convert to probability using sigmoid function
        return 1.0 / (1.0 + Math.exp(-rankDiff));
    }

    /**
     * Apply surface-specific calibration
     */
    private double applySurfaceCalibration(double baseProbability, Player player1, Player player2, String surface) {
        if (surface == null) surface = "Hard";

        PlayerHistory history1 = getPlayerHistory(player1);
        PlayerHistory history2 = getPlayerHistory(player2);

        if (history1 == null || history2 == null) {
            return baseProbability;
        }

        // Get surface-specific ELO ratings
        double elo1 = history1.getSurfaceElo(surface);
        double elo2 = history2.getSurfaceElo(surface);

        // Calculate expected probability from surface ELO
        double eloDiff = elo1 - elo2;
        double eloExpected = 1.0 / (1.0 + Math.pow(10, -eloDiff / 400.0));

        // Get surface adjustment factor
        double surfaceWeight = surfaceAdjustments.getOrDefault(surface, 1.0) * SURFACE_IMPACT_FACTOR;

        // Blend base probability with surface-specific probability
        return baseProbability * (1 - surfaceWeight) + eloExpected * surfaceWeight;
    }

    /**
     * Apply form-based calibration
     */
    private double applyFormCalibration(double baseProbability, Player player1, Player player2, String surface) {
        PlayerHistory history1 = getPlayerHistory(player1);
        PlayerHistory history2 = getPlayerHistory(player2);

        if (history1 == null || history2 == null) {
            return baseProbability;
        }

        // Get recent form and surface form
        double recentForm1 = history1.getRecentForm(10);
        double recentForm2 = history2.getRecentForm(10);
        double surfaceForm1 = history1.getSurfaceForm(surface, 10);
        double surfaceForm2 = history2.getSurfaceForm(surface, 10);

        // Weighted combination of recent and surface form
        double combinedForm1 = recentForm1 * FORM_WEIGHT_RECENT + surfaceForm1 * FORM_WEIGHT_SURFACE;
        double combinedForm2 = recentForm2 * FORM_WEIGHT_RECENT + surfaceForm2 * FORM_WEIGHT_SURFACE;

        // Convert form difference to probability adjustment
        double formDiff = combinedForm1 - combinedForm2;
        double formAdjustment = Math.tanh(formDiff * 2.0) * 0.1; // Max 10% adjustment

        return Math.max(0.05, Math.min(0.95, baseProbability + formAdjustment));
    }

    /**
     * Apply head-to-head calibration with decay
     */
    private double applyHeadToHeadCalibration(double baseProbability, Player player1, Player player2) {
        if (player1.getPlayerId() == null || player2.getPlayerId() == null) {
            return baseProbability;
        }

        HeadToHeadRecord h2h = historyManager.getHeadToHeadRecord(player1.getPlayerId(), player2.getPlayerId());

        if (h2h == null || h2h.getTotalMatches() == 0) {
            return baseProbability;
        }

        // Apply decay based on number of matches (more matches = more reliable)
        double reliability = Math.min(1.0, h2h.getTotalMatches() / 10.0);
        double decayedWeight = reliability * Math.pow(H2H_DECAY_FACTOR, h2h.getTotalMatches());

        // Blend with base probability
        double h2hProbability = h2h.getWinRate();
        return baseProbability * (1 - decayedWeight) + h2hProbability * decayedWeight;
    }

    /**
     * Apply tournament-specific calibration
     */
    private double applyTournamentCalibration(double baseProbability, Match match) {
        String tourneyLevel = match.getTourneyLevel();
        String round = match.getRound();

        // Adjust for tournament level (Grand Slams have different dynamics)
        double levelAdjustment = 0.0;
        if ("G".equals(tourneyLevel)) {
            levelAdjustment = 0.02; // Slight favorite bias in Grand Slams
        }

        // Adjust for late rounds (upsets less likely)
        double roundAdjustment = 0.0;
        if ("SF".equals(round) || "F".equals(round)) {
            // Less volatility in later rounds
            roundAdjustment = (baseProbability - 0.5) * 0.1;
        }

        return baseProbability + levelAdjustment + roundAdjustment;
    }

    /**
     * Enhanced ELO update with adaptive K-factor
     */
    public void updatePlayerELO(Match match) {
        String winnerId = match.getWinner().getPlayerId();
        String loserId = match.getLoser().getPlayerId();
        String surface = match.getSurface();

        if (winnerId == null || loserId == null) return;

        PlayerHistory winnerHistory = getPlayerHistory(match.getWinner());
        PlayerHistory loserHistory = getPlayerHistory(match.getLoser());

        if (winnerHistory == null || loserHistory == null) return;

        double winnerElo = winnerHistory.getSurfaceElo(surface);
        double loserElo = loserHistory.getSurfaceElo(surface);

        // Calculate expected probabilities
        double expectedWinner = 1.0 / (1.0 + Math.pow(10, (loserElo - winnerElo) / 400.0));
        double expectedLoser = 1.0 - expectedWinner;

        // Adaptive K-factor based on player ranking and match importance
        double kFactorWinner = calculateKFactor(match.getWinner(), match);
        double kFactorLoser = calculateKFactor(match.getLoser(), match);

        // Update ELO ratings
        double newWinnerElo = Math.min(ELO_CEILING, Math.max(ELO_FLOOR,
                winnerElo + kFactorWinner * (1.0 - expectedWinner)));
        double newLoserElo = Math.min(ELO_CEILING, Math.max(ELO_FLOOR,
                loserElo + kFactorLoser * (0.0 - expectedLoser)));

        // Update the surface ELO in player histories
        winnerHistory.updateSurfaceElo(surface, newWinnerElo);
        loserHistory.updateSurfaceElo(surface, newLoserElo);
    }

    private double calculateKFactor(Player player, Match match) {
        Integer rank = player.getRank();
        if (rank == null || rank <= 0) rank = 500;

        // Lower K-factor for higher-ranked players
        double baseK = rank <= 50 ? K_FACTOR_HIGH_RANK : K_FACTOR_LOW_RANK;

        // Increase K-factor for important tournaments
        if ("G".equals(match.getTourneyLevel())) {
            baseK *= 1.2;
        } else if ("M".equals(match.getTourneyLevel())) {
            baseK *= 1.1;
        }

        return baseK;
    }

    private PlayerHistory getPlayerHistory(Player player) {
        if (player == null || player.getPlayerId() == null) return null;
        return historyManager.getPlayerHistory(player.getPlayerId());
    }

    /**
     * Calibration validation method
     */
    public CalibrationMetrics validateCalibration(List<Match> testMatches) {
        List<Double> predictions = new ArrayList<>();
        List<Boolean> outcomes = new ArrayList<>();

        for (Match match : testMatches) {
            double prediction = getCalibratedPrediction(match, true);
            predictions.add(prediction);
            outcomes.add(true); // Since we're predicting winner
        }

        return new CalibrationMetrics(predictions, outcomes);
    }

    /**
     * Metrics class for calibration analysis
     */
    public static class CalibrationMetrics {
        private final double brierScore;
        private final double logLoss;
        private final double calibrationError;

        public CalibrationMetrics(List<Double> predictions, List<Boolean> outcomes) {
            this.brierScore = calculateBrierScore(predictions, outcomes);
            this.logLoss = calculateLogLoss(predictions, outcomes);
            this.calibrationError = calculateCalibrationError(predictions, outcomes);
        }

        private double calculateBrierScore(List<Double> predictions, List<Boolean> outcomes) {
            double sum = 0.0;
            for (int i = 0; i < predictions.size(); i++) {
                double pred = predictions.get(i);
                double actual = outcomes.get(i) ? 1.0 : 0.0;
                sum += Math.pow(pred - actual, 2);
            }
            return sum / predictions.size();
        }

        private double calculateLogLoss(List<Double> predictions, List<Boolean> outcomes) {
            double sum = 0.0;
            for (int i = 0; i < predictions.size(); i++) {
                double pred = Math.max(1e-15, Math.min(1 - 1e-15, predictions.get(i)));
                double actual = outcomes.get(i) ? 1.0 : 0.0;
                sum += -(actual * Math.log(pred) + (1 - actual) * Math.log(1 - pred));
            }
            return sum / predictions.size();
        }

        private double calculateCalibrationError(List<Double> predictions, List<Boolean> outcomes) {
            // Simplified calibration error calculation
            Map<Integer, List<Double>> bins = new HashMap<>();
            Map<Integer, List<Boolean>> binOutcomes = new HashMap<>();

            for (int i = 0; i < predictions.size(); i++) {
                int bin = (int) Math.floor(predictions.get(i) * 10);
                bins.computeIfAbsent(bin, k -> new ArrayList<>()).add(predictions.get(i));
                binOutcomes.computeIfAbsent(bin, k -> new ArrayList<>()).add(outcomes.get(i));
            }

            double totalError = 0.0;
            int totalSamples = 0;

            for (Integer bin : bins.keySet()) {
                List<Double> binPreds = bins.get(bin);
                List<Boolean> binOuts = binOutcomes.get(bin);

                double avgPred = binPreds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double avgOutcome = binOuts.stream().mapToDouble(b -> b ? 1.0 : 0.0).average().orElse(0.0);

                totalError += binPreds.size() * Math.abs(avgPred - avgOutcome);
                totalSamples += binPreds.size();
            }

            return totalSamples > 0 ? totalError / totalSamples : 0.0;
        }

        public double getBrierScore() { return brierScore; }
        public double getLogLoss() { return logLoss; }
        public double getCalibrationError() { return calibrationError; }

        @Override
        public String toString() {
            return String.format("Calibration Metrics:\n" +
                    "Brier Score: %.4f\n" +
                    "Log Loss: %.4f\n" +
                    "Calibration Error: %.4f", brierScore, logLoss, calibrationError);
        }
    }
}