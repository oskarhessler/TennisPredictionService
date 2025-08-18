import weka.classifiers.trees.RandomForest;
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
            loadHistoricalData(historyManager);

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

            // 6. Interactive mode for custom predictions
            runInteractiveMode(predictor);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadHistoricalData(PlayerHistoryManager historyManager) {
        try {
            TennisDataLoader loader = new TennisDataLoader();
            List<Match> matches = new ArrayList<>();

            String[] dataFiles = {"merged2005_2024.csv"};

            for (String fileName : dataFiles) {
                try {
                    List<Match> yearMatches = loader.loadMatches(fileName);
                    matches.addAll(yearMatches);
                    System.out.println("Loaded " + yearMatches.size() + " matches from " + fileName);
                    break;
                } catch (Exception e) {
                    System.out.println("Could not load " + fileName + ", using sample data");
                    createSampleHistoricalData(matches);
                    break;
                }
            }

            // Build player histories
            matches.sort(Comparator.comparingInt(m -> m.getTourneyDate() != null ? m.getTourneyDate() : 20200101));
            for (Match match : matches) {
                historyManager.updateWithMatch(match);
            }

            System.out.println("Built histories for players from " + matches.size() + " matches");

        } catch (Exception e) {
            System.err.println("Error loading historical data: " + e.getMessage());
        }
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

        // Define some current top players
        Player djokovic = new Player("104925", "Novak Djokovic", "R", "SRB", 1, "", 188, 36.5, 1, 10000);
        Player alcaraz = new Player("207989", "Carlos Alcaraz", "R", "ESP", 2, "", 183, 21.0, 2, 9500);
        Player sinner = new Player("207733", "Jannik Sinner", "R", "ITA", 3, "", 188, 23.0, 3, 8500);
        Player medvedev = new Player("206173", "Daniil Medvedev", "R", "RUS", 4, "", 198, 28.5, 4, 8000);
        Player zverev = new Player("106421", "Alexander Zverev", "R", "GER", 5, "", 198, 27.0, 5, 7500);

        // Create various match scenarios
        matches.add(new UpcomingMatch(djokovic, alcaraz,
                new MatchContext("Hard", "G", 20250201, "F", 5))); // Australian Open Final

        matches.add(new UpcomingMatch(sinner, medvedev,
                new MatchContext("Hard", "M", 20250315, "SF", 3))); // Masters Semi

        matches.add(new UpcomingMatch(alcaraz, zverev,
                new MatchContext("Clay", "G", 20250601, "QF", 5))); // French Open Quarter

        matches.add(new UpcomingMatch(djokovic, sinner,
                new MatchContext("Grass", "G", 20250701, "F", 5))); // Wimbledon Final

        matches.add(new UpcomingMatch(medvedev, zverev,
                new MatchContext("Hard", "G", 20250901, "SF", 5))); // US Open Semi

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
}