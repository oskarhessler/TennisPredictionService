import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;
import java.io.File;
import java.util.*;
import java.time.format.DateTimeFormatter;

/**
 * Enhanced Tennis Prediction Application with integrated odds comparison
 */
public class TennisPredictionApp {

    public static void main(String[] args) {
        System.out.println("Tennis Prediction & Betting Analysis Application");
        System.out.println("===============================================");

        try {
            // 1. Load the trained model
            String modelPath = "Java/src/main/resources/tennis_rf.model";
            if (!new File(modelPath).exists()) {
                modelPath = "tennis_rf.model"; // fallback path
            }

            System.out.println("Loading model from: " + modelPath);
            RandomForest model = (RandomForest) SerializationHelper.read(modelPath);
            System.out.println("Model loaded successfully!");

            // 2. Initialize prediction system with historical data
            PlayerHistoryManager historyManager = new PlayerHistoryManager();

            System.out.println("Loading historical data for player profiles...");
            List<Match> matches = loadHistoricalData(historyManager);

            // 3. Create predictor with pre-trained model
            FeatureExtractor featureExtractor = new FeatureExtractor(historyManager);
            List<Match> sampleMatches = createSampleMatches();

            WekaTennisTrainer trainer = WekaTennisTrainer.fromPreTrainedModel(model, sampleMatches, featureExtractor);
            WekaTennisPredictor predictor = new WekaTennisPredictor(trainer, featureExtractor, historyManager);

            // 4. Get upcoming matches with odds
            System.out.println("\nFetching upcoming matches with betting odds...");
            List<TennisOddsScraper.MatchWithOdds> matchesWithOdds = TennisOddsScraper.scrapeUpcomingMatches();

            // 5. Analyze matches and provide betting recommendations
            analyzeBettingOpportunities(predictor, matchesWithOdds);

            // 6. Feature importance analysis for model debugging
            System.out.println("\nAnalyzing model features for calibration...");
            analyzeFeatureImportance(model, trainer.getHeader(), sampleMatches, featureExtractor);

            // 7. Model calibration analysis
            performModelCalibrationAnalysis(predictor, historyManager);

            // 8. Display summary statistics
            displayBettingSummary(predictor, matchesWithOdds);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Analyze betting opportunities with detailed odds comparison
     */
    private static void analyzeBettingOpportunities(WekaTennisPredictor predictor,
                                                    List<TennisOddsScraper.MatchWithOdds> matchesWithOdds) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("BETTING ANALYSIS - MODEL vs MARKET ODDS");
        System.out.println("=".repeat(80));

        int valueBetsFound = 0;
        double totalEV = 0.0;

        for (TennisOddsScraper.MatchWithOdds matchWithOdds : matchesWithOdds) {
            try {
                // Make model prediction
                MatchPrediction prediction = predictor.predictMatch(
                        matchWithOdds.getPlayer1(),
                        matchWithOdds.getPlayer2(),
                        matchWithOdds.getContext()
                );

                // Calculate implied probabilities from market odds
                double marketImpliedP1 = 1.0 / matchWithOdds.getOddsPlayer1();
                double marketImpliedP2 = 1.0 / matchWithOdds.getOddsPlayer2();
                double totalImplied = marketImpliedP1 + marketImpliedP2;

                // Remove vig (bookmaker margin) for fair comparison
                double fairMarketP1 = marketImpliedP1 / totalImplied;
                double fairMarketP2 = marketImpliedP2 / totalImplied;

                // Get model probabilities
                double modelP1 = prediction.getPlayer1WinProbability();
                double modelP2 = prediction.getPlayer2WinProbability();

                // Calculate expected values
                double evP1 = calculateEV(matchWithOdds.getOddsPlayer1(), modelP1);
                double evP2 = calculateEV(matchWithOdds.getOddsPlayer2(), modelP2);

                // Display match header
                System.out.println("\n" + "-".repeat(80));
                System.out.printf("%-25s vs %-25s%n",
                        matchWithOdds.getPlayer1().getName() + " (#" + matchWithOdds.getPlayer1().getRank() + ")",
                        matchWithOdds.getPlayer2().getName() + " (#" + matchWithOdds.getPlayer2().getRank() + ")");

                System.out.printf("Tournament: %-15s | Surface: %-5s | Round: %-4s | Time: %s%n",
                        matchWithOdds.getTournament(),
                        matchWithOdds.getContext().getSurface(),
                        matchWithOdds.getContext().getRound(),
                        matchWithOdds.getMatchTime().format(DateTimeFormatter.ofPattern("MMM dd HH:mm")));

                // Display odds comparison table
                System.out.println();
                System.out.println("ODDS COMPARISON:");
                System.out.printf("%-25s | %-12s | %-12s | %-12s | %-8s%n",
                        "Player", "Market Odds", "Market Prob", "Model Prob", "Edge");
                System.out.println("-".repeat(80));

                System.out.printf("%-25s | %-12.2f | %-11.1f%% | %-11.1f%% | %+6.1f%%%n",
                        truncateName(matchWithOdds.getPlayer1().getName(), 25),
                        matchWithOdds.getOddsPlayer1(),
                        fairMarketP1 * 100,
                        modelP1 * 100,
                        (modelP1 - fairMarketP1) * 100);

                System.out.printf("%-25s | %-12.2f | %-11.1f%% | %-11.1f%% | %+6.1f%%%n",
                        truncateName(matchWithOdds.getPlayer2().getName(), 25),
                        matchWithOdds.getOddsPlayer2(),
                        fairMarketP2 * 100,
                        modelP2 * 100,
                        (modelP2 - fairMarketP2) * 100);

                // Betting recommendation
                BettingRecommendation recommendation = getBettingRecommendation(
                        prediction, matchWithOdds.getOddsPlayer1(), matchWithOdds.getOddsPlayer2(), evP1, evP2);

                System.out.println();
                System.out.println("BETTING RECOMMENDATION:");
                System.out.println(formatRecommendation(recommendation));

                if (recommendation.hasValue()) {
                    valueBetsFound++;
                    totalEV += recommendation.getExpectedValue();
                }

            } catch (Exception e) {
                System.err.println("Error analyzing match: " + e.getMessage());
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("ANALYSIS COMPLETE: %d value bets found from %d matches%n",
                valueBetsFound, matchesWithOdds.size());
        if (valueBetsFound > 0) {
            System.out.printf("Average EV of value bets: %.2f%%%n", (totalEV / valueBetsFound) * 100);
        }
        System.out.println("=".repeat(80));
    }

    /**
     * Enhanced betting recommendation with detailed analysis
     */
    private static class BettingRecommendation {
        private final String playerName;
        private final double expectedValue;
        private final double confidence;
        private final String reasoning;
        private final boolean hasValue;
        private final double recommendedStake;

        public BettingRecommendation(String playerName, double expectedValue, double confidence,
                                     String reasoning, boolean hasValue, double recommendedStake) {
            this.playerName = playerName;
            this.expectedValue = expectedValue;
            this.confidence = confidence;
            this.reasoning = reasoning;
            this.hasValue = hasValue;
            this.recommendedStake = recommendedStake;
        }

        // Getters
        public String getPlayerName() { return playerName; }
        public double getExpectedValue() { return expectedValue; }
        public double getConfidence() { return confidence; }
        public String getReasoning() { return reasoning; }
        public boolean hasValue() { return hasValue; }
        public double getRecommendedStake() { return recommendedStake; }
    }

    /**
     * Get detailed betting recommendation
     */
    private static BettingRecommendation getBettingRecommendation(MatchPrediction prediction,
                                                                  double oddsP1, double oddsP2,
                                                                  double evP1, double evP2) {

        final double MIN_EV = 0.05; // 5% minimum expected value
        final double MIN_CONFIDENCE = 0.6; // 60% minimum confidence

        Player p1 = prediction.getPlayer1();
        Player p2 = prediction.getPlayer2();
        double modelP1 = prediction.getPlayer1WinProbability();
        double modelP2 = prediction.getPlayer2WinProbability();

        // Determine confidence based on model certainty and ranking difference
        double maxProb = Math.max(modelP1, modelP2);
        double rankingDiff = Math.abs(p1.getRank() - p2.getRank());
        double confidence = (maxProb - 0.5) * 2.0; // Scale 0.5-1.0 to 0.0-1.0

        // Adjust confidence based on ranking difference (more confidence when ranks align with prediction)
        if (rankingDiff > 20) {
            confidence *= 1.1; // Boost confidence for clear favorites
        }
        confidence = Math.min(confidence, 1.0);

        if (evP1 > MIN_EV && modelP1 > MIN_CONFIDENCE && evP1 > evP2) {
            // Bet on Player 1
            double stake = calculateKellyStake(oddsP1, modelP1, evP1);
            String reasoning = String.format("Model gives %s %.1f%% chance vs market's %.1f%% (edge: +%.1f%%)",
                    p1.getName(), modelP1 * 100, (1.0/oddsP1) * 100,
                    (modelP1 - 1.0/oddsP1) * 100);

            return new BettingRecommendation(p1.getName(), evP1, confidence, reasoning, true, stake);

        } else if (evP2 > MIN_EV && modelP2 > MIN_CONFIDENCE && evP2 > evP1) {
            // Bet on Player 2
            double stake = calculateKellyStake(oddsP2, modelP2, evP2);
            String reasoning = String.format("Model gives %s %.1f%% chance vs market's %.1f%% (edge: +%.1f%%)",
                    p2.getName(), modelP2 * 100, (1.0/oddsP2) * 100,
                    (modelP2 - 1.0/oddsP2) * 100);

            return new BettingRecommendation(p2.getName(), evP2, confidence, reasoning, true, stake);

        } else {
            // No value bet
            String reasoning;
            if (Math.max(evP1, evP2) < MIN_EV) {
                reasoning = String.format("No positive expected value found (best EV: %.2f%%)",
                        Math.max(evP1, evP2) * 100);
            } else {
                reasoning = String.format("Model confidence too low (%.1f%% required)", MIN_CONFIDENCE * 100);
            }

            return new BettingRecommendation("None", 0.0, confidence, reasoning, false, 0.0);
        }
    }

    /**
     * Calculate Kelly criterion stake size
     */
    private static double calculateKellyStake(double odds, double probability, double ev) {
        // Kelly = (bp - q) / b, where b = odds-1, p = probability, q = 1-p
        double b = odds - 1.0;
        double p = probability;
        double q = 1.0 - p;

        double kelly = (b * p - q) / b;

        // Cap at 10% of bankroll for safety (fractional Kelly)
        return Math.min(kelly * 0.25, 0.10); // Use quarter Kelly, max 10%
    }

    /**
     * Format betting recommendation for display
     */
    private static String formatRecommendation(BettingRecommendation rec) {
        if (rec.hasValue()) {
            return String.format(
                    "🎯 BET RECOMMENDATION: %s\n" +
                            "   Expected Value: +%.2f%%\n" +
                            "   Confidence: %.1f%%\n" +
                            "   Suggested Stake: %.1f%% of bankroll\n" +
                            "   Reasoning: %s",
                    rec.getPlayerName().toUpperCase(),
                    rec.getExpectedValue() * 100,
                    rec.getConfidence() * 100,
                    rec.getRecommendedStake() * 100,
                    rec.getReasoning()
            );
        } else {
            return String.format(
                    "❌ NO BET RECOMMENDED\n" +
                            "   Reasoning: %s",
                    rec.getReasoning()
            );
        }
    }

    /**
     * Display betting summary statistics
     */
    private static void displayBettingSummary(WekaTennisPredictor predictor,
                                              List<TennisOddsScraper.MatchWithOdds> matchesWithOdds) {

        System.out.println("\n" + "=".repeat(60));
        System.out.println("BETTING SUMMARY & STATISTICS");
        System.out.println("=".repeat(60));

        int totalMatches = matchesWithOdds.size();
        int valueBets = 0;
        int strongFavorites = 0;
        int upsetPicks = 0;
        double totalPositiveEV = 0.0;

        Map<String, Integer> surfaceCount = new HashMap<>();
        Map<String, Integer> tournamentCount = new HashMap<>();

        for (TennisOddsScraper.MatchWithOdds match : matchesWithOdds) {
            try {
                MatchPrediction prediction = predictor.predictMatch(
                        match.getPlayer1(), match.getPlayer2(), match.getContext());

                double evP1 = calculateEV(match.getOddsPlayer1(), prediction.getPlayer1WinProbability());
                double evP2 = calculateEV(match.getOddsPlayer2(), prediction.getPlayer2WinProbability());

                if (evP1 > 0.05 || evP2 > 0.05) {
                    valueBets++;
                    totalPositiveEV += Math.max(evP1, evP2);
                }

                // Check for upset picks (model favors underdog)
                boolean p1FavoriteByRank = match.getPlayer1().getRank() < match.getPlayer2().getRank();
                boolean p1FavoriteByModel = prediction.getPlayer1WinProbability() > 0.5;

                if (p1FavoriteByRank != p1FavoriteByModel) {
                    upsetPicks++;
                }

                // Strong favorites (model >75% confident)
                if (Math.max(prediction.getPlayer1WinProbability(),
                        prediction.getPlayer2WinProbability()) > 0.75) {
                    strongFavorites++;
                }

                // Count by surface and tournament
                String surface = match.getContext().getSurface();
                String tournament = match.getTournament();

                surfaceCount.put(surface, surfaceCount.getOrDefault(surface, 0) + 1);
                tournamentCount.put(tournament, tournamentCount.getOrDefault(tournament, 0) + 1);

            } catch (Exception e) {
                // Skip problematic matches
                continue;
            }
        }

        // Display summary
        System.out.printf("Total Matches Analyzed: %d%n", totalMatches);
        System.out.printf("Value Bets Found: %d (%.1f%%)%n", valueBets, (valueBets * 100.0) / totalMatches);
        System.out.printf("Upset Predictions: %d (%.1f%%)%n", upsetPicks, (upsetPicks * 100.0) / totalMatches);
        System.out.printf("Strong Favorites (>75%%): %d (%.1f%%)%n",
                strongFavorites, (strongFavorites * 100.0) / totalMatches);

        if (valueBets > 0) {
            System.out.printf("Average EV of Value Bets: +%.2f%%\n", (totalPositiveEV / valueBets) * 100);
        }

        System.out.println("\nBreakdown by Surface:");
        for (Map.Entry<String, Integer> entry : surfaceCount.entrySet()) {
            System.out.printf("  %s: %d matches%n", entry.getKey(), entry.getValue());
        }

        System.out.println("=".repeat(60));
    }

    /**
     * Comprehensive feature importance analysis for model debugging
     */
    private static void analyzeFeatureImportance(RandomForest model, Instances header,
                                                 List<Match> sampleMatches, FeatureExtractor featureExtractor) {
        try {
            System.out.println("\n" + "=".repeat(70));
            System.out.println("MODEL FEATURE IMPORTANCE ANALYSIS");
            System.out.println("=".repeat(70));

            // Method 1: Tree-based feature importance from Random Forest
            System.out.println("\n1. RANDOM FOREST FEATURE IMPORTANCE:");
            System.out.println("-".repeat(50));
            List<FeatureImportanceAnalyzer.FeatureImportance> rfImportances =
                    FeatureImportanceAnalyzer.calculateSimpleImportance(model, header);
            FeatureImportanceAnalyzer.printFeatureImportance(rfImportances, 20);

            // Method 2: Create test data for permutation importance
            System.out.println("\n2. CREATING TEST DATA FOR VALIDATION:");
            System.out.println("-".repeat(50));
            Instances testData = createValidationInstances(sampleMatches, featureExtractor);

            if (testData != null && testData.numInstances() > 0) {
                System.out.printf("Created %d test instances with %d features%n",
                        testData.numInstances(), testData.numAttributes() - 1);

                // Method 3: Permutation importance (more accurate)
                System.out.println("\n3. PERMUTATION-BASED FEATURE IMPORTANCE:");
                System.out.println("-".repeat(50));
                List<FeatureImportanceAnalyzer.FeatureImportance> permImportances =
                        FeatureImportanceAnalyzer.calculatePermutationImportance(model, testData, 3);
                FeatureImportanceAnalyzer.printFeatureImportance(permImportances, 15);

                // Method 4: Feature correlation analysis
                analyzeFeatureCorrelations(testData);
            } else {
                System.out.println("Could not create test data for permutation importance");
            }

        } catch (Exception e) {
            System.err.println("Error analyzing feature importance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Model calibration analysis to identify prediction issues
     */
    private static void performModelCalibrationAnalysis(WekaTennisPredictor predictor,
                                                        PlayerHistoryManager historyManager) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("MODEL CALIBRATION ANALYSIS");
        System.out.println("=".repeat(70));

        try {
            // Create diverse test matches to check model behavior
            List<TestMatch> testMatches = createCalibrationTestMatches();

            System.out.println("\n1. RANKING SENSITIVITY TEST:");
            System.out.println("-".repeat(50));
            analyzeRankingSensitivity(predictor, testMatches);

            System.out.println("\n2. SURFACE SENSITIVITY TEST:");
            System.out.println("-".repeat(50));
            analyzeSurfaceSensitivity(predictor, testMatches);

            System.out.println("\n3. HEAD-TO-HEAD CONSISTENCY TEST:");
            System.out.println("-".repeat(50));
            analyzeHeadToHeadConsistency(predictor, testMatches);

            System.out.println("\n4. PROBABILITY DISTRIBUTION ANALYSIS:");
            System.out.println("-".repeat(50));
            analyzeProbabilityDistribution(predictor, testMatches);

        } catch (Exception e) {
            System.err.println("Error in calibration analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test match class for calibration analysis
     */
    private static class TestMatch {
        private final Player player1;
        private final Player player2;
        private final MatchContext context;
        private final String description;
        private final double expectedProbabilityRange;

        public TestMatch(Player player1, Player player2, MatchContext context,
                         String description, double expectedProbabilityRange) {
            this.player1 = player1;
            this.player2 = player2;
            this.context = context;
            this.description = description;
            this.expectedProbabilityRange = expectedProbabilityRange;
        }

        // Getters
        public Player getPlayer1() { return player1; }
        public Player getPlayer2() { return player2; }
        public MatchContext getContext() { return context; }
        public String getDescription() { return description; }
        public double getExpectedProbabilityRange() { return expectedProbabilityRange; }
    }

    /**
     * Create test matches for calibration analysis
     */
    private static List<TestMatch> createCalibrationTestMatches() {
        List<TestMatch> testMatches = new ArrayList<>();

        // Create diverse players for testing
        Player djokovic = new Player("104925", "Novak Djokovic", "R", "SRB", 1, "", 188, 37.0, 1, 10000);
        Player alcaraz = new Player("207989", "Carlos Alcaraz", "R", "ESP", 2, "", 183, 21.0, 2, 9500);
        Player sinner = new Player("207733", "Jannik Sinner", "R", "ITA", 4, "", 188, 23.0, 3, 8500);
        Player fritz = new Player("FB98", "Taylor Fritz", "R", "USA", 4, "", 196, 27.0, 12, 3000);
        Player shelton = new Player("S0S1", "Ben Shelton", "L", "USA", 7, "", 193, 22.0, 15, 2500);
        Player qualifier = new Player("Q001", "John Qualifier", "R", "USA", 150, "", 180, 25.0, 150, 200);

        // Test 1: Extreme ranking differences
        testMatches.add(new TestMatch(djokovic, qualifier,
                new MatchContext("Hard", "G", 20250801, "R32", 5),
                "Top 1 vs Qualifier (Grand Slam)", 0.85));

        testMatches.add(new TestMatch(alcaraz, fritz,
                new MatchContext("Hard", "M", 20250801, "QF", 3),
                "Top 3 vs Top 15 (Masters)", 0.70));

        testMatches.add(new TestMatch(sinner, shelton,
                new MatchContext("Hard", "M", 20250801, "SF", 3),
                "Similar level players", 0.60));

        // Test 2: Surface variations
        testMatches.add(new TestMatch(djokovic, alcaraz,
                new MatchContext("Clay", "G", 20250801, "F", 5),
                "Top players on Clay", 0.65));

        testMatches.add(new TestMatch(djokovic, alcaraz,
                new MatchContext("Grass", "G", 20250801, "F", 5),
                "Top players on Grass", 0.65));

        testMatches.add(new TestMatch(djokovic, alcaraz,
                new MatchContext("Hard", "G", 20250801, "F", 5),
                "Top players on Hard", 0.65));

        // Test 3: Tournament level variations
        testMatches.add(new TestMatch(fritz, shelton,
                new MatchContext("Hard", "G", 20250801, "QF", 5),
                "Mid-tier Grand Slam", 0.55));

        testMatches.add(new TestMatch(fritz, shelton,
                new MatchContext("Hard", "B", 20250801, "F", 3),
                "Mid-tier ATP 250", 0.55));

        // Test 4: Age/experience factors
        Player youngPlayer = new Player("Y001", "Young Talent", "R", "USA", 25, "", 185, 19.0, 30, 1500);
        Player veteranPlayer = new Player("V001", "Veteran Player", "R", "ESP", 35, "", 183, 35.0, 40, 1200);

        testMatches.add(new TestMatch(youngPlayer, veteranPlayer,
                new MatchContext("Hard", "M", 20250801, "R32", 3),
                "Young vs Veteran", 0.60));

        return testMatches;
    }

    /**
     * Analyze how model responds to ranking differences
     */
    private static void analyzeRankingSensitivity(WekaTennisPredictor predictor, List<TestMatch> testMatches) {
        System.out.printf("%-40s | %-8s | %-8s | %-10s%n", "Match Description", "P1 Rank", "P2 Rank", "P1 Win %");
        System.out.println("-".repeat(75));

        for (TestMatch test : testMatches) {
            try {
                MatchPrediction prediction = predictor.predictMatch(
                        test.getPlayer1(), test.getPlayer2(), test.getContext());

                System.out.printf("%-40s | %-8d | %-8d | %-9.1f%%n",
                        truncateName(test.getDescription(), 40),
                        test.getPlayer1().getRank(),
                        test.getPlayer2().getRank(),
                        prediction.getPlayer1WinProbability() * 100);

            } catch (Exception e) {
                System.out.printf("%-40s | ERROR: %s%n", test.getDescription(), e.getMessage());
            }
        }
    }

    /**
     * Analyze surface sensitivity
     */
    private static void analyzeSurfaceSensitivity(WekaTennisPredictor predictor, List<TestMatch> testMatches) {
        Map<String, List<Double>> surfaceProbabilities = new HashMap<>();

        System.out.printf("%-25s | %-8s | %-10s | %-15s%n", "Match", "Surface", "P1 Win %", "Expected Range");
        System.out.println("-".repeat(65));

        for (TestMatch test : testMatches) {
            if (test.getDescription().contains("Top players")) {
                try {
                    MatchPrediction prediction = predictor.predictMatch(
                            test.getPlayer1(), test.getPlayer2(), test.getContext());

                    String surface = test.getContext().getSurface();
                    double prob = prediction.getPlayer1WinProbability();

                    surfaceProbabilities.computeIfAbsent(surface, k -> new ArrayList<>()).add(prob);

                    System.out.printf("%-25s | %-8s | %-9.1f%% | %-15.1f%%%n",
                            truncateName(test.getDescription(), 25),
                            surface,
                            prob * 100,
                            test.getExpectedProbabilityRange() * 100);

                } catch (Exception e) {
                    System.out.printf("%-25s | ERROR%n", test.getDescription());
                }
            }
        }

        // Summary
        System.out.println("\nSurface Sensitivity Summary:");
        for (Map.Entry<String, List<Double>> entry : surfaceProbabilities.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            System.out.printf("  %s: Average = %.1f%%%n", entry.getKey(), avg * 100);
        }
    }

    /**
     * Check head-to-head consistency
     */
    private static void analyzeHeadToHeadConsistency(WekaTennisPredictor predictor, List<TestMatch> testMatches) {
        System.out.printf("%-30s | %-10s | %-10s | %-12s%n", "Match", "A vs B", "B vs A", "Consistency");
        System.out.println("-".repeat(70));

        for (TestMatch test : testMatches) {
            try {
                // Predict A vs B
                MatchPrediction predictionAB = predictor.predictMatch(
                        test.getPlayer1(), test.getPlayer2(), test.getContext());

                // Predict B vs A (should be complement)
                MatchPrediction predictionBA = predictor.predictMatch(
                        test.getPlayer2(), test.getPlayer1(), test.getContext());

                double probAB = predictionAB.getPlayer1WinProbability();
                double probBA = predictionBA.getPlayer2WinProbability(); // Player A is now player 2

                double consistency = Math.abs(probAB - probBA);
                String status = consistency < 0.05 ? "✓ Good" : "⚠ Poor";

                System.out.printf("%-30s | %-9.1f%% | %-9.1f%% | %s (%.3f)%n",
                        truncateName(test.getDescription(), 30),
                        probAB * 100,
                        probBA * 100,
                        status,
                        consistency);

            } catch (Exception e) {
                System.out.printf("%-30s | ERROR%n", test.getDescription());
            }
        }
    }

    /**
     * Analyze probability distribution patterns
     */
    private static void analyzeProbabilityDistribution(WekaTennisPredictor predictor, List<TestMatch> testMatches) {
        List<Double> allProbabilities = new ArrayList<>();

        for (TestMatch test : testMatches) {
            try {
                MatchPrediction prediction = predictor.predictMatch(
                        test.getPlayer1(), test.getPlayer2(), test.getContext());
                allProbabilities.add(prediction.getPlayer1WinProbability());
            } catch (Exception e) {
                // Skip failed predictions
            }
        }

        if (allProbabilities.isEmpty()) {
            System.out.println("No predictions available for analysis");
            return;
        }

        Collections.sort(allProbabilities);
        double min = allProbabilities.get(0);
        double max = allProbabilities.get(allProbabilities.size() - 1);
        double mean = allProbabilities.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double median = allProbabilities.get(allProbabilities.size() / 2);

        // Calculate standard deviation
        double variance = allProbabilities.stream()
                .mapToDouble(p -> Math.pow(p - mean, 2))
                .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);

        System.out.printf("Probability Distribution Analysis (%d predictions):%n", allProbabilities.size());
        System.out.printf("  Minimum:  %.1f%%%n", min * 100);
        System.out.printf("  Maximum:  %.1f%%%n", max * 100);
        System.out.printf("  Mean:     %.1f%%%n", mean * 100);
        System.out.printf("  Median:   %.1f%%%n", median * 100);
        System.out.printf("  Std Dev:  %.1f%%%n", stdDev * 100);
        System.out.printf("  Range:    %.1f%%%n", (max - min) * 100);

        // Identify potential issues
        if (stdDev < 0.05) {
            System.out.println("  ⚠️  WARNING: Very low standard deviation suggests model may not be differentiating well");
        }
        if (max - min < 0.20) {
            System.out.println("  ⚠️  WARNING: Narrow probability range suggests poor calibration");
        }
        if (Math.abs(mean - 0.5) < 0.02) {
            System.out.println("  ⚠️  WARNING: Mean too close to 50% - model may be defaulting");
        }
    }

    /**
     * Create validation instances for permutation importance
     */
    private static Instances createValidationInstances(List<Match> matches, FeatureExtractor featureExtractor) {
        try {
            // Create a small but representative dataset
            List<Match> validationMatches = new ArrayList<>();

            // Add diverse matches if available from historical data
            if (matches.size() >= 50) {
                // Sample matches from different years, surfaces, levels
                Random random = new Random(42);
                Set<Integer> selectedIndices = new HashSet<>();

                while (selectedIndices.size() < Math.min(100, matches.size() / 10)) {
                    selectedIndices.add(random.nextInt(matches.size()));
                }

                for (int index : selectedIndices) {
                    validationMatches.add(matches.get(index));
                }
            } else {
                // Create synthetic validation data
                validationMatches = createSyntheticValidationMatches();
            }

            if (validationMatches.isEmpty()) {
                return null;
            }

            // Convert to Weka instances using your feature extractor
            List<double[]> features = new ArrayList<>();
            List<Boolean> outcomes = new ArrayList<>();

            for (Match match : validationMatches) {
                try {
                    // Extract features from winner's perspective
                    double[] winnerFeatures = featureExtractor.extractFeatures(match, true).toDoubleArray();
                    if (winnerFeatures != null && winnerFeatures.length > 0) {
                        features.add(winnerFeatures);
                        outcomes.add(true); // Winner perspective
                    }

                    // Extract features from loser's perspective for balanced dataset
                    double[] loserFeatures = featureExtractor.extractFeatures(match, false).toDoubleArray();
                    if (loserFeatures != null && loserFeatures.length > 0) {
                        features.add(loserFeatures);
                        outcomes.add(false); // Loser perspective
                    }
                } catch (Exception e) {
                    // Skip problematic matches
                    continue;
                }
            }

            if (features.isEmpty()) {
                return null;
            }

            // Create Weka Instances (simplified version)
            // Note: This is a basic implementation - you might need to adapt based on your exact Weka setup
            return createWekaInstances(features, outcomes);

        } catch (Exception e) {
            System.err.println("Error creating validation instances: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create synthetic matches for validation if historical data is insufficient
     */
    private static List<Match> createSyntheticValidationMatches() {
        List<Match> matches = new ArrayList<>();

        // Create players with varying characteristics
        Player[] players = {
                new Player("P001", "Top Player", "R", "ESP", 1, "", 185, 25.0, 1, 10000),
                new Player("P002", "Clay Specialist", "R", "ESP", 15, "", 180, 28.0, 15, 2500),
                new Player("P003", "Grass Expert", "R", "GBR", 25, "", 188, 30.0, 25, 1800),
                new Player("P004", "Hard Court", "R", "USA", 10, "", 193, 24.0, 10, 3200),
                new Player("P005", "Veteran", "R", "SUI", 50, "", 185, 35.0, 50, 1000),
                new Player("P006", "Young Gun", "R", "ITA", 30, "", 183, 20.0, 30, 1500)
        };

        String[] surfaces = {"Hard", "Clay", "Grass"};
        String[] levels = {"G", "M", "A", "B"};
        String[] rounds = {"R32", "R16", "QF", "SF", "F"};

        Random random = new Random(42);

        for (int i = 0; i < 50; i++) {
            Player p1 = players[random.nextInt(players.length)];
            Player p2 = players[random.nextInt(players.length)];

            if (p1.equals(p2)) continue;

            // Randomly determine winner (with some bias towards higher ranked)
            Player winner, loser;
            if (p1.getRank() < p2.getRank()) {
                winner = random.nextDouble() < 0.7 ? p1 : p2;
            } else {
                winner = random.nextDouble() < 0.7 ? p2 : p1;
            }
            loser = winner.equals(p1) ? p2 : p1;

            Match match = new Match.Builder()
                    .tourneyId("SYNTH" + i)
                    .surface(surfaces[random.nextInt(surfaces.length)])
                    .tourneyLevel(levels[random.nextInt(levels.length)])
                    .tourneyDate(20240000 + random.nextInt(365))
                    .round(rounds[random.nextInt(rounds.length)])
                    .bestOf(random.nextBoolean() ? 3 : 5)
                    .winner(winner)
                    .loser(loser)
                    .build();

            matches.add(match);
        }

        return matches;
    }

    /**
     * Create Weka Instances from feature arrays
     */
    private static Instances createWekaInstances(List<double[]> features, List<Boolean> outcomes) {
        // This is a placeholder - you'll need to implement this based on your exact Weka setup
        // and feature structure. For now, return null to avoid compilation errors.

        System.out.println("Created synthetic dataset with " + features.size() + " instances");
        System.out.println("Feature vector length: " + (features.isEmpty() ? 0 : features.get(0).length));

        return null; // Replace with actual Weka Instances creation
    }

    /**
     * Analyze feature correlations to identify redundant features
     */
    private static void analyzeFeatureCorrelations(Instances data) {
        try {
            System.out.println("\n4. FEATURE CORRELATION ANALYSIS:");
            System.out.println("-".repeat(50));

            if (data == null || data.numAttributes() < 3) {
                System.out.println("Insufficient data for correlation analysis");
                return;
            }

            // Find highly correlated features (simplified version)
            int numFeatures = data.numAttributes() - 1; // Exclude class attribute
            System.out.printf("Analyzing correlations among %d features...%n", numFeatures);

            // This is a simplified placeholder - implement proper correlation analysis
            System.out.println("Top correlated feature pairs:");
            System.out.println("  (Feature correlation analysis would go here)");
            System.out.println("  Implement based on your specific feature set");

        } catch (Exception e) {
            System.err.println("Error in correlation analysis: " + e.getMessage());
        }
    }

    // Helper methods
    private static double calculateEV(double odds, double modelProb) {
        return (modelProb * odds) - 1.0;
    }

    private static String truncateName(String name, int maxLength) {
        return name.length() <= maxLength ? name : name.substring(0, maxLength - 3) + "...";
    }

    // ... (keep all the existing helper methods from the original code)
    private static List<Match> loadHistoricalData(PlayerHistoryManager historyManager) {
        List<Match> matches = new ArrayList<>();
        try {
            TennisDataLoader loader = new TennisDataLoader();
            String[] dataFiles = {"Java/src/main/resources/merged2005_2025.csv"};

            for (String fileName : dataFiles) {
                try {
                    List<Match> yearMatches = loader.loadMatches(fileName);
                    matches.addAll(yearMatches);
                    System.out.println("Loaded " + yearMatches.size() + " matches from " + fileName);
                    break;
                } catch (Exception e) {
                    System.out.println("Could not load " + fileName + ", using sample data");
                    break;
                }
            }

            matches.sort(Comparator.comparingInt(m -> m.getTourneyDate() != null ? m.getTourneyDate() : 20200101));
            for (Match match : matches) {
                historyManager.updateWithMatch(match);
            }

            System.out.println("Built histories for players from " + matches.size() + " matches");

        } catch (Exception e) {
            System.err.println("Error loading historical data: " + e.getMessage());
        }
        return matches;
    }

    private static List<Match> createSampleMatches() {
        List<Match> matches = new ArrayList<>();

        Player p1 = new Player("104925", "Novak Djokovic", "R", "SRB", 1, "", 188, 36.5, 1, 10000);
        Player p2 = new Player("207989", "Carlos Alcaraz", "R", "ESP", 2, "", 183, 21.0, 2, 9500);

        Match sample = new Match.Builder()
                .tourneyId("SAMPLE")
                .surface("Hard")
                .tourneyLevel("G")
                .tourneyDate(20250101)
                .round("F")
                .bestOf(5)
                .winner(p1)
                .loser(p2)
                .build();

        matches.add(sample);
        return matches;
    }
}