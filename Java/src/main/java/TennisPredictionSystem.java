import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ml.dmlc.xgboost4j.java.Booster;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Main application orchestrating the tennis prediction system
 */
public class TennisPredictionSystem {

    public static void main(String[] args) throws IOException, XGBoostError {
        System.out.println("Starting Tennis Prediction System...");

// 1. Load training data
        TennisDataLoader loader = new TennisDataLoader();
        List<Match> allMatches = new ArrayList<>();

// Load multiple years of data from resources
        String[] dataFiles = {"merged2005_2024.csv"};
        for (String fileName : dataFiles) {
            // Hitta filen i resources
            URL resource = TennisDataLoader.class.getClassLoader().getResource(fileName);
            if (resource == null) {
                System.out.println("Filen hittades inte i resources: " + fileName);
                continue;
            }

            // Konvertera URL till filväg som String
            String filePath = new File(resource.getFile()).getAbsolutePath();

            // Ladda matcher med String
            List<Match> yearMatches = loader.loadMatches(filePath);
            allMatches.addAll(yearMatches);
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

        System.out.printf("Training on %d matches, testing on %d matches\n", trainMatches.size(), testMatches.size());

        // 3. Prepare training data
        PlayerHistoryManager historyManager = new PlayerHistoryManager();
        FeatureExtractor featureExtractor = new FeatureExtractor(historyManager);
        TrainingDataPreparer dataPreparer = new TrainingDataPreparer(featureExtractor, true);

        // Build player histories from training data
        for (Match match : trainMatches) {
            historyManager.updateWithMatch(match);
        }

        DMatrix trainData = dataPreparer.prepareDMatrix(trainMatches);
        // FIXED: Use getColNum() instead of featureNum()
        System.out.printf("Prepared training matrix: %d rows\n",
                trainData.rowNum());

        // 4. Train model
        TennisModelTrainer trainer = new TennisModelTrainer();
        TrainingResult result = trainer.trainModel(trainData, 500);

        System.out.println("Training completed!");

        // 5. Save model
        String modelPath = "models/tennis_model.json";
        result.saveModel(modelPath);
        System.out.printf("Model saved to: %s\n", modelPath);

        // 6. Evaluate on test data (backtest)
        System.out.println("\nBacktesting on recent matches...");
        evaluateOnTestData(result.getModel(), testMatches, historyManager);

        // 7. Example predictions on new matches
        System.out.println("\nExample predictions:");
        demonstratePredictions(result.getModel(), historyManager);
    }

    private static void evaluateOnTestData(Booster model, List<Match> testMatches, PlayerHistoryManager historyManager)
            throws XGBoostError {

        // Reset history manager to training state for fair backtesting
        PlayerHistoryManager backtestHistory = new PlayerHistoryManager();
        FeatureExtractor featureExtractor = new FeatureExtractor(backtestHistory);
        TennisPredictor predictor = new TennisPredictor(model, featureExtractor, backtestHistory);

        int correct = 0;
        int total = 0;
        double totalLogLoss = 0.0;

        for (Match match : testMatches) {
            // Make prediction before updating histories
            MatchContext context = new MatchContext(
                    match.getSurface(), match.getTourneyLevel(), match.getTourneyDate(),
                    match.getRound(), match.getBestOf()
            );

            MatchPrediction prediction = predictor.predictMatch(match.getWinner(), match.getLoser(), context);

            // Check if prediction is correct (winner should have higher probability)
            boolean isCorrect = prediction.getPlayer1WinProbability() >= 0.5;
            if (isCorrect) correct++;
            total++;

            // Calculate log loss
            double prob = prediction.getPlayer1WinProbability();
            double logLoss = -Math.log(Math.max(1e-15, prob));
            totalLogLoss += logLoss;

            // Update histories AFTER making prediction
            backtestHistory.updateWithMatch(match);
        }

        double accuracy = (double) correct / total;
        double avgLogLoss = totalLogLoss / total;

        System.out.printf("Backtest Results: Accuracy = %.3f (%.1f%%), Log Loss = %.4f\n",
                accuracy, accuracy * 100, avgLogLoss);
    }

    private static void demonstratePredictions(Booster model, PlayerHistoryManager historyManager)
            throws XGBoostError {

        FeatureExtractor featureExtractor = new FeatureExtractor(historyManager);
        TennisPredictor predictor = new TennisPredictor(model, featureExtractor, historyManager);

        // Create example players (you'd normally load these from your database)
        Player djokovic = new Player("DJ01", "Novak Djokovic", "R", "SRB", 1, "", 188, 36.5, 1, 10000);
        Player nadal = new Player("RA01", "Rafael Nadal", "L", "ESP", 2, "", 185, 37.8, 2, 9500);

        // Example match contexts
        MatchContext hardCourt = new MatchContext("Hard", "M", 20250801, "F", 3);
        MatchContext clayCourt = new MatchContext("Clay", "G", 20250601, "F", 5);

        MatchPrediction hardPrediction = predictor.predictMatch(djokovic, nadal, hardCourt);
        MatchPrediction clayPrediction = predictor.predictMatch(djokovic, nadal, clayCourt);

        System.out.println("Hard Court Final: " + hardPrediction);
        System.out.println("Clay Court Final: " + clayPrediction);
    }
}