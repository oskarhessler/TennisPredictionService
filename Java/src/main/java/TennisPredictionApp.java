import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;
import java.io.File;
import java.util.*;

/**
 * Main application for loading a trained tennis prediction model and making predictions
 */
public class TennisPredictionApp {

    public static void main(String[] args) {
        System.out.println("Tennis Prediction Application");
        System.out.println("============================");

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

            // Load historical data to build player histories
            System.out.println("Loading historical data for player profiles...");
            List<Match> matches = loadHistoricalData(historyManager);

            // 3. Create predictor with pre-trained model
            FeatureExtractor featureExtractor = new FeatureExtractor(historyManager);

            // Create some sample matches to establish feature structure
            List<Match> sampleMatches = createSampleMatches();

            // Create trainer from pre-trained model
            WekaTennisTrainer trainer = WekaTennisTrainer.fromPreTrainedModel(model, sampleMatches, featureExtractor);
            WekaTennisPredictor predictor = new WekaTennisPredictor(trainer, featureExtractor, historyManager);

            // 4. Define some upcoming matches to predict
            List<UpcomingMatch> upcomingMatches = createSampleUpcomingMatches();

            // 5. Make predictions
            System.out.println("\nUpcoming Match Predictions:");
            System.out.println("==========================");

            List<MatchPrediction> predictions = predictor.predictMatches(upcomingMatches);

            for (MatchPrediction prediction : predictions) {
                printPrediction(prediction);
            }

            // 6. Feature importance analysis
            System.out.println("Analyzing feature importance...");
            analyzeFeatureImportance(model, trainer.getHeader(), sampleMatches, featureExtractor);


            // 7. Interactive mode for custom predictions
            runInteractiveMode(predictor);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

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
                    //createSampleHistoricalData(matches);
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

    private static void createSampleHistoricalData(List<Match> matches) {
        // Create some sample data for major players
        Player djokovic = new Player("104925", "Novak Djokovic", "R", "SRB", 1, "", 188, 36.5, 1, 10000);
        Player nadal = new Player("104745", "Rafael Nadal", "L", "ESP", 2, "", 185, 37.8, 2, 9500);
        Player federer = new Player("103819", "Roger Federer", "R", "SUI", 3, "", 185, 42.0, 3, 9000);
        Player alcaraz = new Player("207989", "Carlos Alcaraz", "R", "ESP", 1, "", 183, 21.0, 2, 8500);
        Player sinner = new Player("207733", "Jannik Sinner", "R", "ITA", 4, "", 188, 23.0, 4, 8000);

        String[] surfaces = {"Hard", "Clay", "Grass"};
        Random random = new Random(42);

        for (int i = 0; i < 200; i++) {
            Player[] players = {djokovic, nadal, federer, alcaraz, sinner};
            Player p1 = players[random.nextInt(players.length)];
            Player p2 = players[random.nextInt(players.length)];

            if (p1.equals(p2)) continue;

            Player winner = random.nextBoolean() ? p1 : p2;
            Player loser = winner.equals(p1) ? p2 : p1;
            String surface = surfaces[random.nextInt(surfaces.length)];

            Match match = new Match.Builder()
                    .tourneyId("SAMPLE" + i)
                    .surface(surface)
                    .tourneyLevel(random.nextBoolean() ? "G" : "M")
                    .tourneyDate(20240000 + random.nextInt(365))
                    .round(random.nextBoolean() ? "F" : "SF")
                    .bestOf(random.nextBoolean() ? 3 : 5)
                    .winner(winner)
                    .loser(loser)
                    .build();

            matches.add(match);
        }
    }

    private static List<UpcomingMatch> createSampleUpcomingMatches() {
        List<UpcomingMatch> matches = new ArrayList<>();

        // Players from Canada Masters 2025
        Player rublev = new Player("RE44", "Andrey Rublev", "R", "RUS", 11, "", 188, 27.77, 11, 3110);
        Player sonego = new Player("SU87", "Lorenzo Sonego", "R", "ITA", 38, "", 191, 30.21, 38, 1281);
        Player tiafoe = new Player("TD51", "Frances Tiafoe", "R", "USA", 12, "", 188, 27.52, 12, 2990);
        Player vukic = new Player("V832", "Aleksandar Vukic", "R", "AUS", 99, "", 188, 29.31, 99, 639);
        Player fFokina = new Player("DH50", "Alejandro Davidovich Fokina", "R", "ESP", 19, "", 180, 26.14, 19, 2225);
        Player mensik = new Player("M0NI", "Jakub Mensik", "R", "CZE", 18, "", 196, 19.9, 18, 2362);
        Player cobolli = new Player("C0E9", "Flavio Cobolli", "R", "ITA", 17, "", 183, 23.23, 17, 2385);
        Player michelsen = new Player("M0QL", "Alex Michelsen", "R", "USA", 34, "", 193, 20.92, 34, 1555);
        Player zverev = new Player("Z355", "Alexander Zverev", "R", "DEU", 3, "", 198, 28.27, 3, 6030);
        Player khachanov = new Player("KE29", "Karen Khachanov", "R", "RUS", 16, "", 198, 29.18, 16, 2590);
        Player shelton = new Player("S0S1", "Ben Shelton", "L", "USA", 7, "", 193, 22.8, 7, 3520);
        Player fritz = new Player("FB98", "Taylor Fritz", "R", "USA", 4, "", 196, 27.75, 4, 5135);
        Player deMinaur = new Player("DH58", "Alex de Minaur", "R", "AUS", 8, "", 183, 26.44, 8, 3335);

        // -------------------------------
        // Obvious advantage matches (~70-90%)
        // -------------------------------
        matches.add(new UpcomingMatch(rublev, sonego,
                new MatchContext("Hard", "M", 20250727, "R32", 3))); // Rublev ~85% favorite

        matches.add(new UpcomingMatch(fFokina, mensik,
                new MatchContext("Hard", "M", 20250727, "R32", 3))); // Fokina ~91% favorite

        matches.add(new UpcomingMatch(tiafoe, vukic,
                new MatchContext("Hard", "M", 20250727, "R32", 3))); // Vukic upset? ~52% Vukic, 48% Tiafoe

        matches.add(new UpcomingMatch(zverev, khachanov,
                new MatchContext("Hard", "M", 20250727, "R32", 3))); // Zverev ~70% favorite

        matches.add(new UpcomingMatch(shelton, fritz,
                new MatchContext("Hard", "M", 20250727, "R16", 3))); // Shelton ~56% favorite

        // -------------------------------
        // Close games (~50-55%)
        // -------------------------------
        matches.add(new UpcomingMatch(cobolli, michelsen,
                new MatchContext("Hard", "M", 20250727, "R32", 3))); // Cobolli ~55% favorite

        matches.add(new UpcomingMatch(deMinaur, tiafoe,
                new MatchContext("Hard", "M", 20250727, "R16", 3))); // de Minaur ~55% favorite

        matches.add(new UpcomingMatch(zverev, shelton,
                new MatchContext("Hard", "M", 20250727, "QF", 3))); // Zverev ~56% favorite

        matches.add(new UpcomingMatch(khachanov, michelsen,
                new MatchContext("Hard", "M", 20250727, "QF", 3))); // Khachanov ~50% (even game)

        matches.add(new UpcomingMatch(shelton, khachanov,
                new MatchContext("Hard", "M", 20250727, "F", 3))); // Shelton ~59% favorite

        return matches;
    }



    private static void printPrediction(MatchPrediction prediction) {
        Player p1 = prediction.getPlayer1();
        Player p2 = prediction.getPlayer2();
        MatchContext context = prediction.getContext();

        double p1Prob = prediction.getPlayer1WinProbability();
        double p2Prob = prediction.getPlayer2WinProbability();

        String favorite = p1Prob > p2Prob ? p1.getName() : p2.getName();
        double favProb = Math.max(p1Prob, p2Prob);

        System.out.printf("%-20s vs %-20s | %s %-15s | %s %.1f%% favorite%n",
                p1.getName(), p2.getName(),
                getTournamentType(context.getTourneyLevel()),
                context.getSurface(),
                favorite, favProb * 100);

        System.out.printf("    %s: %.1f%% | %s: %.1f%%%n",
                p1.getName(), p1Prob * 100,
                p2.getName(), p2Prob * 100);
        System.out.println();
    }

    private static String getTournamentType(String level) {
        switch (level != null ? level : "") {
            case "G": return "Grand Slam";
            case "M": return "Masters   ";
            case "A": return "ATP 500   ";
            case "B": return "ATP 250   ";
            default: return "Tournament";
        }
    }

    private static void runInteractiveMode(WekaTennisPredictor predictor) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nInteractive Prediction Mode");
        System.out.println("==========================");
        System.out.println("Enter 'quit' to exit");

        while (true) {
            try {
                System.out.println("\nCreate a custom match prediction:");

                System.out.print("Player 1 name: ");
                String p1Name = scanner.nextLine().trim();
                if ("quit".equalsIgnoreCase(p1Name)) break;

                System.out.print("Player 1 rank (1-200): ");
                int p1Rank = Integer.parseInt(scanner.nextLine().trim());

                System.out.print("Player 2 name: ");
                String p2Name = scanner.nextLine().trim();
                if ("quit".equalsIgnoreCase(p2Name)) break;

                System.out.print("Player 2 rank (1-200): ");
                int p2Rank = Integer.parseInt(scanner.nextLine().trim());

                System.out.print("Surface (Hard/Clay/Grass): ");
                String surface = scanner.nextLine().trim();
                if (surface.isEmpty()) surface = "Hard";

                System.out.print("Tournament level (G/M/A/B): ");
                String level = scanner.nextLine().trim();
                if (level.isEmpty()) level = "M";

                System.out.print("Round (F/SF/QF/R16): ");
                String round = scanner.nextLine().trim();
                if (round.isEmpty()) round = "QF";

                // Create players
                Player p1 = new Player("CUSTOM1", p1Name, "R", "UNK", null, "", 185, 25.0, p1Rank,
                        Math.max(100, 10000 - p1Rank * 40));
                Player p2 = new Player("CUSTOM2", p2Name, "R", "UNK", null, "", 185, 25.0, p2Rank,
                        Math.max(100, 10000 - p2Rank * 40));

                MatchContext context = new MatchContext(surface, level, 20250801, round,
                        "G".equals(level) ? 5 : 3);

                MatchPrediction prediction = predictor.predictMatch(p1, p2, context);

                System.out.println("\nPrediction:");
                System.out.println("-----------");
                printPrediction(prediction);

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage() + ". Please try again.");
            }
        }

        scanner.close();
        System.out.println("Goodbye!");
    }

    private static void analyzeFeatureImportance(RandomForest model, Instances header,
                                                 List<Match> sampleMatches, FeatureExtractor featureExtractor) {
        try {
            // Method 1: Simple heuristic-based importance (faster)
            System.out.println("\nCalculating feature importance (heuristic method)...");
            List<FeatureImportanceAnalyzer.FeatureImportance> simpleImportances =
                    FeatureImportanceAnalyzer.calculateSimpleImportance(model, header);
            FeatureImportanceAnalyzer.printFeatureImportance(simpleImportances, 15);

            // Method 2: Permutation importance (more accurate but slower)
            // Uncomment if you want more accurate but slower analysis
            /*
            System.out.println("\nCalculating permutation-based feature importance...");
            Instances testData = createTestInstances(sampleMatches, featureExtractor);
            List<FeatureImportanceAnalyzer.FeatureImportance> permutationImportances =
                FeatureImportanceAnalyzer.calculatePermutationImportance(model, testData, 5);
            FeatureImportanceAnalyzer.printFeatureImportance(permutationImportances, 15);
            */

        } catch (Exception e) {
            System.err.println("Error analyzing feature importance: " + e.getMessage());
        }
    }

    private static Instances createTestInstances(List<Match> matches, FeatureExtractor featureExtractor) {
        // Create a small test dataset for permutation importance
        // This is a simplified version - you might want to use actual validation data
        return null; // Placeholder - implement if needed
    }
    private static void evaluateModel(RandomForest model, FeatureExtractor featureExtractor,
                                      PlayerHistoryManager historyManager, List<Match> matches) {
        try {
            // Filter test set (example: only 2024 matches)
            List<Match> testMatches = new ArrayList<>();
            for (Match m : matches) {
                if (m.getTourneyDate() != null && m.getTourneyDate() / 10000 == 2024) {
                    testMatches.add(m);
                }
            }

            if (testMatches.isEmpty()) {
                System.out.println("No matches found for evaluation!");
                return;
            }

            int correct = 0;
            int total = 0;
            double logloss = 0.0;

            WekaTennisTrainer trainer = WekaTennisTrainer.fromPreTrainedModel(model, testMatches, featureExtractor);
            WekaTennisPredictor predictor = new WekaTennisPredictor(trainer, featureExtractor, historyManager);

            for (Match m : testMatches) {
                MatchContext context = new MatchContext(
                        m.getSurface(), m.getTourneyLevel(), m.getTourneyDate(),
                        m.getRound(), m.getBestOf()
                );

                MatchPrediction prediction = predictor.predictMatch(m.getWinner(), m.getLoser(), context);

                double probWinner = prediction.getPlayer1().equals(m.getWinner())
                        ? prediction.getPlayer1WinProbability()
                        : prediction.getPlayer2WinProbability();

                // Accuracy
                if (probWinner >= 0.5) {
                    correct++;
                }
                total++;

                // Logloss
                logloss += -Math.log(Math.max(probWinner, 1e-15));
            }

            double accuracy = (double) correct / total;
            double avgLogloss = logloss / total;

            System.out.println("\n=== MODEL EVALUATION (2024) ===");
            System.out.printf("Matches evaluated: %d%n", total);
            System.out.printf("Accuracy: %.2f%%%n", accuracy * 100);
            System.out.printf("Logloss: %.4f%n", avgLogloss);

        } catch (Exception e) {
            System.err.println("Error during evaluation: " + e.getMessage());
            e.printStackTrace();
        }
    }

}