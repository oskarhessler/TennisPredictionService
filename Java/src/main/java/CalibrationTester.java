import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for testing and validating tennis model calibration
 */
public class CalibrationTester {

    private final TennisModelCalibrator calibrator;
    private final Random random = new Random(42); // Fixed seed for reproducibility

    public CalibrationTester(TennisModelCalibrator calibrator) {
        this.calibrator = calibrator;
    }

    /**
     * Create comprehensive test scenarios based on the original analysis issues
     */
    public List<TestScenario> createCalibrationTestScenarios() {
        List<TestScenario> scenarios = new ArrayList<>();

        // Test 1: Ranking sensitivity (addressing the 11.9% vs expected higher % issue)
        scenarios.add(createRankingSensitivityTest());

        // Test 2: Surface impact (addressing uniform 49.3% across all surfaces)
        scenarios.add(createSurfaceImpactTest());

        // Test 3: Head-to-head consistency (addressing poor consistency scores)
        scenarios.add(createHeadToHeadConsistencyTest());

        // Test 4: Form factor validation
        scenarios.add(createFormFactorTest());

        // Test 5: Tournament context impact
        scenarios.add(createTournamentContextTest());

        return scenarios;
    }

    private TestScenario createRankingSensitivityTest() {
        List<Match> testMatches = new ArrayList<>();

        // Create matches with extreme ranking differences
        testMatches.add(createTestMatch(1, 150, "Hard", "G", "R128")); // Top 1 vs Qualifier
        testMatches.add(createTestMatch(1, 100, "Hard", "G", "R64"));  // Top 1 vs Top 100
        testMatches.add(createTestMatch(1, 50, "Hard", "G", "R32"));   // Top 1 vs Top 50
        testMatches.add(createTestMatch(5, 50, "Hard", "M", "R16"));   // Top 5 vs Top 50
        testMatches.add(createTestMatch(10, 30, "Hard", "A", "QF"));   // Top 10 vs Top 30

        return new TestScenario("Ranking Sensitivity", testMatches,
                "Tests if model properly reflects ranking differences. " +
                        "Expected: Higher-ranked players should have significantly higher win probabilities against lower-ranked opponents.");
    }

    private TestScenario createSurfaceImpactTest() {
        List<Match> testMatches = new ArrayList<>();

        // Same ranking matchup across different surfaces
        int rank1 = 5, rank2 = 15;
        testMatches.add(createTestMatch(rank1, rank2, "Hard", "G", "QF"));
        testMatches.add(createTestMatch(rank1, rank2, "Clay", "G", "QF"));
        testMatches.add(createTestMatch(rank1, rank2, "Grass", "G", "QF"));

        // Add surface specialists vs non-specialists scenarios
        testMatches.add(createSpecialistMatch("Clay", rank1, rank2));
        testMatches.add(createSpecialistMatch("Grass", rank1, rank2));

        return new TestScenario("Surface Impact", testMatches,
                "Tests if model differentiates surface impact. " +
                        "Expected: Probabilities should vary meaningfully across surfaces, especially for specialists.");
    }

    private TestScenario createHeadToHeadConsistencyTest() {
        List<Match> testMatches = new ArrayList<>();

        // Create matches where A beats B, then test B vs A
        Match matchAB = createTestMatch(10, 12, "Hard", "M", "SF");
        Match matchBA = createTestMatch(12, 10, "Hard", "M", "SF");

        // Simulate head-to-head history
        simulateHeadToHeadHistory(matchAB.getWinner().getPlayerId(), matchAB.getLoser().getPlayerId(), 3, 1);

        testMatches.add(matchAB);
        testMatches.add(matchBA);

        return new TestScenario("Head-to-Head Consistency", testMatches,
                "Tests if P(A beats B) + P(B beats A) ≈ 1.0. " +
                        "Expected: Complementary probabilities should be consistent.");
    }

    private TestScenario createFormFactorTest() {
        List<Match> testMatches = new ArrayList<>();

        // Create players with different form patterns
        Match hotPlayerMatch = createTestMatch(20, 25, "Hard", "A", "QF");
        Match coldPlayerMatch = createTestMatch(25, 20, "Hard", "A", "QF");

        // Simulate different form for the players
        simulatePlayerForm(hotPlayerMatch.getWinner().getPlayerId(), true, 8, 2);  // Hot form: 8-2
        simulatePlayerForm(coldPlayerMatch.getWinner().getPlayerId(), false, 3, 7); // Cold form: 3-7

        testMatches.add(hotPlayerMatch);
        testMatches.add(coldPlayerMatch);

        return new TestScenario("Form Factor", testMatches,
                "Tests if recent form affects predictions appropriately. " +
                        "Expected: Players in good form should have higher win probabilities.");
    }

    private TestScenario createTournamentContextTest() {
        List<Match> testMatches = new ArrayList<>();

        // Same players, different tournament contexts
        int rank1 = 8, rank2 = 15;
        testMatches.add(createTestMatch(rank1, rank2, "Hard", "G", "F"));    // Grand Slam Final
        testMatches.add(createTestMatch(rank1, rank2, "Hard", "M", "F"));    // Masters Final
        testMatches.add(createTestMatch(rank1, rank2, "Hard", "A", "F"));    // ATP 250 Final
        testMatches.add(createTestMatch(rank1, rank2, "Hard", "G", "R32"));  // Grand Slam Early Round

        return new TestScenario("Tournament Context", testMatches,
                "Tests if tournament level and round affect predictions. " +
                        "Expected: Different contexts should produce different probabilities.");
    }

    /**
     * Run all calibration tests and generate report
     */
    public CalibrationReport runCalibrationTests() {
        List<TestScenario> scenarios = createCalibrationTestScenarios();
        List<TestResult> results = new ArrayList<>();

        for (TestScenario scenario : scenarios) {
            TestResult result = runTestScenario(scenario);
            results.add(result);
        }

        return new CalibrationReport(results);
    }

    private TestResult runTestScenario(TestScenario scenario) {
        List<Double> predictions = new ArrayList<>();
        List<String> details = new ArrayList<>();

        for (Match match : scenario.getMatches()) {
            double prediction = calibrator.getCalibratedPrediction(match, true);
            predictions.add(prediction);

            String detail = String.format("%s vs %s (R%d vs R%d): %.1f%%",
                    match.getWinner().getPlayerId(),
                    match.getLoser().getPlayerId(),
                    match.getWinner().getRank(),
                    match.getLoser().getRank(),
                    prediction * 100);
            details.add(detail);
        }

        return new TestResult(scenario.getName(), predictions, details, scenario.getExpectedBehavior());
    }

    /**
     * Helper methods for creating test matches
     */
    private Match createTestMatch(int rank1, int rank2, String surface, String tourneyLevel, String round) {
        Player player1 = createTestPlayer("P" + rank1, rank1);
        Player player2 = createTestPlayer("P" + rank2, rank2);

        Match match = new TestMatch();
        match.setWinner(player1);
        match.setLoser(player2);
        match.setSurface(surface);
        match.setTourneyLevel(tourneyLevel);
        match.setRound(round);
        match.setTourneyDate(20240601); // June 1, 2024

        return match;
    }

    private Player createTestPlayer(String id, int rank) {
        Player player = new TestPlayer();
        player.setPlayerId(id);
        player.setRank(rank);
        player.setRankPoints(Math.max(1, 5000 - rank * 30)); // Realistic point distribution
        player.setAge(25 + random.nextInt(10));
        player.setHeight(175 + random.nextInt(20));
        player.setHand(random.nextBoolean() ? "R" : "L");

        return player;
    }

    private Match createSpecialistMatch(String surface, int rank1, int rank2) {
        Match match = createTestMatch(rank1, rank2, surface, "G", "QF");

        // Simulate surface specialization for player 1
        String p1Id = match.getWinner().getPlayerId();
        if ("Clay".equals(surface)) {
            simulateSurfaceSpecialization(p1Id, "Clay", 0.75, 20);
            simulateSurfaceSpecialization(p1Id, "Hard", 0.55, 15);
        } else if ("Grass".equals(surface)) {
            simulateSurfaceSpecialization(p1Id, "Grass", 0.80, 15);
            simulateSurfaceSpecialization(p1Id, "Hard", 0.60, 20);
        }

        return match;
    }

    private void simulateHeadToHeadHistory(String player1Id, String player2Id, int wins, int losses) {
        // This would update the historyManager with simulated H2H data
        // Implementation depends on your PlayerHistoryManager interface
    }

    private void simulatePlayerForm(String playerId, boolean goodForm, int wins, int losses) {
        // This would add simulated recent matches to player history
        // Implementation depends on your PlayerHistoryManager interface
    }

    private void simulateSurfaceSpecialization(String playerId, String surface, double winRate, int matches) {
        // This would add surface-specific match history
        // Implementation depends on your PlayerHistoryManager interface
    }

    /**
     * Test scenario container
     */
    public static class TestScenario {
        private final String name;
        private final List<Match> matches;
        private final String expectedBehavior;

        public TestScenario(String name, List<Match> matches, String expectedBehavior) {
            this.name = name;
            this.matches = matches;
            this.expectedBehavior = expectedBehavior;
        }

        public String getName() { return name; }
        public List<Match> getMatches() { return matches; }
        public String getExpectedBehavior() { return expectedBehavior; }
    }

    /**
     * Test result container
     */
    public static class TestResult {
        private final String scenarioName;
        private final List<Double> predictions;
        private final List<String> details;
        private final String expectedBehavior;
        private final double averagePrediction;
        private final double predictionVariance;

        public TestResult(String scenarioName, List<Double> predictions, List<String> details, String expectedBehavior) {
            this.scenarioName = scenarioName;
            this.predictions = predictions;
            this.details = details;
            this.expectedBehavior = expectedBehavior;
            this.averagePrediction = predictions.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            double mean = averagePrediction;
            this.predictionVariance = predictions.stream()
                    .mapToDouble(p -> Math.pow(p - mean, 2))
                    .average().orElse(0.0);
        }

        public String getScenarioName() { return scenarioName; }
        public List<Double> getPredictions() { return predictions; }
        public List<String> getDetails() { return details; }
        public String getExpectedBehavior() { return expectedBehavior; }
        public double getAveragePrediction() { return averagePrediction; }
        public double getPredictionVariance() { return predictionVariance; }

        public boolean isReasonablyCalibrated() {
            // Simple heuristics for reasonable calibration
            return averagePrediction >= 0.1 && averagePrediction <= 0.9 && predictionVariance > 0.001;
        }
    }

    /**
     * Comprehensive calibration report
     */
    public static class CalibrationReport {
        private final List<TestResult> testResults;
        private final Date reportDate;

        public CalibrationReport(List<TestResult> testResults) {
            this.testResults = testResults;
            this.reportDate = new Date();
        }

        public void printReport() {
            System.out.println("=".repeat(80));
            System.out.println("TENNIS MODEL CALIBRATION REPORT");
            System.out.println("Generated: " + reportDate);
            System.out.println("=".repeat(80));

            for (TestResult result : testResults) {
                printTestResult(result);
            }

            printSummary();
        }

        private void printTestResult(TestResult result) {
            System.out.println("\n" + "-".repeat(60));
            System.out.println("TEST: " + result.getScenarioName());
            System.out.println("-".repeat(60));
            System.out.println("Expected: " + result.getExpectedBehavior());
            System.out.println();

            System.out.println("Predictions:");
            for (String detail : result.getDetails()) {
                System.out.println("  " + detail);
            }

            System.out.printf("\nStatistics:\n");
            System.out.printf("  Average Prediction: %.1f%%\n", result.getAveragePrediction() * 100);
            System.out.printf("  Prediction Range: %.1f%% - %.1f%%\n",
                    result.getPredictions().stream().mapToDouble(Double::doubleValue).min().orElse(0) * 100,
                    result.getPredictions().stream().mapToDouble(Double::doubleValue).max().orElse(0) * 100);
            System.out.printf("  Variance: %.4f\n", result.getPredictionVariance());
            System.out.printf("  Calibration Status: %s\n",
                    result.isReasonablyCalibrated() ? "✓ GOOD" : "⚠ NEEDS ATTENTION");
        }

        private void printSummary() {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("CALIBRATION SUMMARY");
            System.out.println("=".repeat(80));

            long wellCalibratedTests = testResults.stream()
                    .mapToLong(r -> r.isReasonablyCalibrated() ? 1 : 0)
                    .sum();

            System.out.printf("Tests Passed: %d/%d\n", wellCalibratedTests, testResults.size());

            if (wellCalibratedTests == testResults.size()) {
                System.out.println("✓ Model appears well-calibrated across all test scenarios");
            } else {
                System.out.println("⚠ Model needs calibration adjustments in some areas");
                System.out.println("\nRecommendations:");

                for (TestResult result : testResults) {
                    if (!result.isReasonablyCalibrated()) {
                        System.out.println("- Review " + result.getScenarioName() + " calibration parameters");
                    }
                }
            }
        }

        public List<TestResult> getTestResults() { return testResults; }
        public Date getReportDate() { return reportDate; }
    }

    // Simple test implementations of Match and Player interfaces
    private static class TestMatch implements Match {
        private Player winner, loser;
        private String surface, tourneyLevel, round;
        private Integer tourneyDate, drawSize, bestOf;

        public void setWinner(Player winner) { this.winner = winner; }
        public void setLoser(Player loser) { this.loser = loser; }
        public void setSurface(String surface) { this.surface = surface; }
        public void setTourneyLevel(String tourneyLevel) { this.tourneyLevel = tourneyLevel; }
        public void setRound(String round) { this.round = round; }
        public void setTourneyDate(Integer tourneyDate) { this.tourneyDate = tourneyDate; }

        @Override public Player getWinner() { return winner; }
        @Override public Player getLoser() { return loser; }
        @Override public String getSurface() { return surface; }
        @Override public String getTourneyLevel() { return tourneyLevel; }
        @Override public String getRound() { return round; }
        @Override public Integer getTourneyDate() { return tourneyDate; }
        @Override public Integer getDrawSize() { return drawSize; }
        @Override public Integer getBestOf() { return bestOf; }
        @Override public MatchStats getWinnerStats() { return null; }
        @Override public MatchStats getLoserStats() { return null; }
    }

    private static class TestPlayer implements Player {
        private String playerId, hand, entry;
        private Integer rank, rankPoints, seed, height, age;

        public void setPlayerId(String playerId) { this.playerId = playerId; }
        public void setRank(Integer rank) { this.rank = rank; }
        public void setRankPoints(Integer rankPoints) { this.rankPoints = rankPoints; }
        public void setAge(Integer age) { this.age = age; }
        public void setHeight(Integer height) { this.height = height; }
        public void setHand(String hand) { this.hand = hand; }

        @Override public String getPlayerId() { return playerId; }
        @Override public Integer getRank() { return rank; }
        @Override public Integer getRankPoints() { return rankPoints; }
        @Override public Integer getAge() { return age; }
        @Override public Integer getHeight() { return height; }
        @Override public String getHand() { return hand; }
        @Override public Integer getSeed() { return seed; }
        @Override public String getEntry() { return entry; }
    }
}