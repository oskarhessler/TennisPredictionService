import weka.classifiers.trees.RandomForest;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Main application using Weka instead of XGBoost
 */
public class WekaTennisPredictionSystem {

    public static void main(String[] args) throws IOException {
        System.out.println("Starting Weka Tennis Prediction System...");

        // 1. Load training data
        TennisDataLoader loader = new TennisDataLoader();
        List<Match> allMatches = new ArrayList<>();

        // Load data from resources
        String[] dataFiles = {"merged2005_2024.csv"};
        for (String fileName : dataFiles) {
            URL resource = TennisDataLoader.class.getClassLoader().getResource(fileName);
            if (resource == null) {
                System.out.println("File not found in resources: " + fileName);
                continue;
            }

            String filePath = new File(resource.getFile()).getAbsolutePath();
            List<Match> yearMatches = loader.loadMatches(filePath);
            allMatches.addAll(yearMatches);
        }

        if (allMatches.isEmpty()) {
            System.err.println("ERROR: No matches loaded! Check if CSV file exists.");
            return;
        }

        // Sort chronologically
        allMatches.sort((a, b) -> {
            Integer dateA = a.getTourneyDate() != null ? a.getTourneyDate() : 0;
            Integer dateB = b.getTourneyDate() != null ? b.getTourneyDate() : 0;
            return dateA.compareTo(dateB);
        });

        System.out.printf("Loaded %d matches\n", allMatches.size());

        // 2. Split data chronologically
        int trainSplit = (int) (allMatches.size() * 0.8);
        List<Match> trainMatches = allMatches.subList(0, trainSplit);
        List<Match> testMatches = allMatches.subList(trainSplit, allMatches.size());

        System.out.printf("Training on %d matches, testing on %d matches\n",
                trainMatches.size(), testMatches.size());

        // 3. Prepare feature extractor with player histories
        PlayerHistoryManager historyManager = new PlayerHistoryManager();
        FeatureExtractor featureExtractor = new FeatureExtractor(historyManager);

        // Build player histories from training data
        for (Match match : trainMatches) {
            historyManager.updateWithMatch(match);
        }

        // 4. Train Weka model
        WekaTennisTrainer trainer = new WekaTennisTrainer();
        WekaTrainingResult result = trainer.trainModel(trainMatches, featureExtractor);

        System.out.println("Training completed!");
        System.out.println("Training result: " + result);

        // 5. Evaluate on test data (backtest)
        System.out.println("\nBacktesting on recent matches...");
        evaluateOnTestData(trainer, testMatches, historyManager);

        // 6. Example predictions
        System.out.println("\nExample predictions:");
        demonstratePredictions(trainer, historyManager);
    }

    private static void evaluateOnTestData(WekaTennisTrainer trainer, List<Match> testMatches,
                                           PlayerHistoryManager historyManager) {

        // Create fresh history manager for backtesting
        PlayerHistoryManager backtestHistory = new PlayerHistoryManager();
        FeatureExtractor featureExtractor = new FeatureExtractor(backtestHistory);

        int correct = 0;
        int total = 0;
        double totalLogLoss = 0.0;

        for (Match match : testMatches) {
            try {
                // Create synthetic match for prediction (winner as player1)
                Match syntheticMatch = createSyntheticMatch(match.getWinner(), match.getLoser(),
                        match.getSurface(), match.getTourneyLevel(), match.getTourneyDate(),
                        match.getRound(), match.getBestOf());

                // Extract features and predict
                FeatureVector features = featureExtractor.extractFeatures(syntheticMatch, true);
                double winProbability = trainer.predictWinProbability(features);

                // Check if prediction is correct (winner should have > 50% probability)
                boolean isCorrect = winProbability >= 0.5;
                if (isCorrect) correct++;
                total++;

                // Calculate log loss
                double p = Math.max(1e-15, Math.min(1 - 1e-15, winProbability));
                double logLoss = -Math.log(p);
                totalLogLoss += logLoss;

                // Update histories AFTER making prediction
                backtestHistory.updateWithMatch(match);

            } catch (Exception e) {
                System.err.println("Error processing match: " + e.getMessage());
            }
        }

        if (total > 0) {
            double accuracy = (double) correct / total;
            double avgLogLoss = totalLogLoss / total;

            System.out.printf("Backtest Results: Accuracy = %.3f (%.1f%%), Log Loss = %.4f\n",
                    accuracy, accuracy * 100, avgLogLoss);
        } else {
            System.out.println("No valid test matches processed");
        }
    }

    private static void demonstratePredictions(WekaTennisTrainer trainer, PlayerHistoryManager historyManager) {
        try {
            FeatureExtractor featureExtractor = new FeatureExtractor(historyManager);

            // Create example players
            Player djokovic = new Player("DJ01", "Novak Djokovic", "R", "SRB", 1, "", 188, 36.5, 1, 10000);
            Player nadal = new Player("RA01", "Rafael Nadal", "L", "ESP", 2, "", 185, 37.8, 2, 9500);

            // Example match contexts
            Match hardCourtMatch = createSyntheticMatch(djokovic, nadal, "Hard", "M", 20250801, "F", 3);
            Match clayCourtMatch = createSyntheticMatch(djokovic, nadal, "Clay", "G", 20250601, "F", 5);

            FeatureVector hardFeatures = featureExtractor.extractFeatures(hardCourtMatch, true);
            FeatureVector clayFeatures = featureExtractor.extractFeatures(clayCourtMatch, true);

            double hardProbability = trainer.predictWinProbability(hardFeatures);
            double clayProbability = trainer.predictWinProbability(clayFeatures);

            System.out.printf("Hard Court Final: Djokovic vs Nadal - Djokovic %.1f%% favorite\n",
                    hardProbability * 100);
            System.out.printf("Clay Court Final: Djokovic vs Nadal - Djokovic %.1f%% favorite\n",
                    clayProbability * 100);

        } catch (Exception e) {
            System.err.println("Error in demonstration predictions: " + e.getMessage());
        }
    }

    private static Match createSyntheticMatch(Player player1, Player player2, String surface,
                                              String tourneyLevel, Integer tourneyDate,
                                              String round, Integer bestOf) {
        return new Match.Builder()
                .surface(surface)
                .tourneyLevel(tourneyLevel)
                .tourneyDate(tourneyDate)
                .round(round)
                .bestOf(bestOf)
                .winner(player1)
                .loser(player2)
                .build();
    }
}