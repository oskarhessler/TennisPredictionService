import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class WekaTennisPredictionSystem {

    public static void main(String[] args) {
        System.out.println("Starting Weka Tennis Prediction System...");

        try {
            // 1. Load training data
            TennisDataLoader loader = new TennisDataLoader();
            List<Match> allMatches = new ArrayList<>();

            // Try to load data from resources or current directory
            String[] dataFiles = {"merged2005_2024.csv", "atp_matches_2024.csv", "tennis_data.csv"};
            boolean dataLoaded = false;

            for (String fileName : dataFiles) {
                try {
                    // First try resources
                    URL resource = WekaTennisPredictionSystem.class.getClassLoader().getResource(fileName);
                    String filePath;

                    if (resource != null) {
                        filePath = new File(resource.getFile()).getAbsolutePath();
                    } else {
                        // Try current directory
                        filePath = fileName;
                        if (!new File(filePath).exists()) {
                            continue;
                        }
                    }

                    List<Match> yearMatches = loader.loadMatches(filePath);
                    allMatches.addAll(yearMatches);
                    System.out.println("Loaded " + yearMatches.size() + " matches from " + fileName);
                    dataLoaded = true;
                    break; // Use first available file

                } catch (Exception e) {
                    System.out.println("Could not load " + fileName + ": " + e.getMessage());
                }
            }

            if (!dataLoaded || allMatches.isEmpty()) {
                System.err.println("ERROR: No tennis data loaded! Please ensure you have a CSV file with tennis match data.");
                System.err.println("Expected files: " + Arrays.toString(dataFiles));
                createSampleData(allMatches); // Create sample data for demonstration
            }

            // Sort chronologically
            allMatches.sort((a, b) -> {
                Integer dateA = a.getTourneyDate() != null ? a.getTourneyDate() : 20200101;
                Integer dateB = b.getTourneyDate() != null ? b.getTourneyDate() : 20200101;
                return dateA.compareTo(dateB);
            });

            System.out.printf("Total matches available: %d\n", allMatches.size());

            // 2. Split data chronologically
            int trainSplit = Math.max(1, (int) (allMatches.size() * 0.8));
            List<Match> trainMatches = allMatches.subList(0, trainSplit);
            List<Match> testMatches = allMatches.size() > trainSplit ?
                    allMatches.subList(trainSplit, allMatches.size()) : new ArrayList<>();

            System.out.printf("Training on %d matches, testing on %d matches\n",
                    trainMatches.size(), testMatches.size());

            // 3. Prepare feature extractor with player histories
            PlayerHistoryManager historyManager = new PlayerHistoryManager();
            FeatureExtractor featureExtractor = new FeatureExtractor(historyManager);

            // Build player histories from training data
            System.out.println("Building player histories...");
            for (Match match : trainMatches) {
                try {
                    historyManager.updateWithMatch(match);
                } catch (Exception e) {
                    System.err.println("Error updating history for match: " + e.getMessage());
                }
            }

            // 4. Train Weka model
            System.out.println("Training model...");
            WekaTennisTrainer trainer = new WekaTennisTrainer();
            WekaTrainingResult result = trainer.trainModel(trainMatches, featureExtractor);

            //

            System.out.println("Training completed!");
            System.out.println("Training result: " + result);

            // 5. Evaluate on test data (if available)
            if (!testMatches.isEmpty()) {
                System.out.println("\nBacktesting on recent matches...");
                evaluateOnTestData(trainer, testMatches, historyManager);
            }

            // 6. Example predictions
            System.out.println("\nExample predictions:");
            demonstratePredictions(trainer, historyManager);

        } catch (Exception e) {
            System.err.println("Error in main application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createSampleData(List<Match> matches) {
        System.out.println("Creating sample data for demonstration...");

        // Create sample players
        Player djokovic = new Player("DJ01", "Novak Djokovic", "R", "SRB", 1, "", 188, 36.5, 1, 10000);
        Player nadal = new Player("RA01", "Rafael Nadal", "L", "ESP", 2, "", 185, 37.8, 2, 9500);
        Player federer = new Player("RF01", "Roger Federer", "R", "SUI", 3, "", 185, 42.0, 3, 9000);
        Player murray = new Player("AM01", "Andy Murray", "R", "GBR", 4, "", 190, 36.8, 4, 8500);

        // Create sample matches
        for (int i = 0; i < 50; i++) {
            Player winner = (i % 2 == 0) ? djokovic : nadal;
            Player loser = (i % 2 == 0) ? nadal : djokovic;

            if (i % 4 == 2) { winner = federer; loser = murray; }
            if (i % 4 == 3) { winner = murray; loser = federer; }

            String surface = (i % 3 == 0) ? "Hard" : (i % 3 == 1) ? "Clay" : "Grass";

            Match match = new Match.Builder()
                    .tourneyId("SAMPLE" + i)
                    .surface(surface)
                    .tourneyLevel("M")
                    .tourneyDate(20240100 + i)
                    .round("F")
                    .bestOf(3)
                    .winner(winner)
                    .loser(loser)
                    .build();

            matches.add(match);
        }
    }

    private static void evaluateOnTestData(WekaTennisTrainer trainer, List<Match> testMatches,
                                           PlayerHistoryManager historyManager) {
        PlayerHistoryManager backtestHistory = new PlayerHistoryManager();
        FeatureExtractor featureExtractor = new FeatureExtractor(backtestHistory);

        int correct = 0;
        int total = 0;
        double totalLogLoss = 0.0;

        for (Match match : testMatches) {
            try {
                Match syntheticMatch = createSyntheticMatch(match.getWinner(), match.getLoser(),
                        match.getSurface(), match.getTourneyLevel(), match.getTourneyDate(),
                        match.getRound(), match.getBestOf());

                FeatureVector features = featureExtractor.extractFeatures(syntheticMatch, true);
                double winProbability = trainer.predictWinProbability(features);

                boolean isCorrect = winProbability >= 0.5;
                if (isCorrect) correct++;
                total++;

                double p = Math.max(1e-15, Math.min(1 - 1e-15, winProbability));
                double logLoss = -Math.log(p);
                totalLogLoss += logLoss;

                backtestHistory.updateWithMatch(match);

            } catch (Exception e) {
                System.err.println("Error processing test match: " + e.getMessage());
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

            Player djokovic = new Player("DJ01", "Novak Djokovic", "R", "SRB", 1, "", 188, 36.5, 1, 10000);
            Player nadal = new Player("RA01", "Rafael Nadal", "L", "ESP", 2, "", 185, 37.8, 2, 9500);

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