import weka.classifiers.trees.RandomForest;
import weka.core.SerializationHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * Weka-based tennis predictor (replacement for XGBoost predictor)
 */
public class WekaTennisPredictor {
    private final WekaTennisTrainer trainer;
    private final FeatureExtractor featureExtractor;
    private final PlayerHistoryManager historyManager;

    public WekaTennisPredictor(WekaTennisTrainer trainer, FeatureExtractor featureExtractor,
                               PlayerHistoryManager historyManager) {
        this.trainer = trainer;
        this.featureExtractor = featureExtractor;
        this.historyManager = historyManager;
    }

    public static WekaTennisPredictor loadFromFile(String modelPath, PlayerHistoryManager historyManager) {
        try {
            RandomForest model = (RandomForest) SerializationHelper.read(modelPath);
            WekaTennisTrainer trainer = new WekaTennisTrainer();
            // Note: You would need to set the loaded model in trainer
            FeatureExtractor featureExtractor = new FeatureExtractor(historyManager);
            return new WekaTennisPredictor(trainer, featureExtractor, historyManager);
        } catch (Exception e) {
            System.err.println("Failed to load model: " + e.getMessage());
            return null;
        }
    }

    public MatchPrediction predictMatch(Player player1, Player player2, MatchContext context) {
        try {
            // Create synthetic match for feature extraction
            Match syntheticMatch = createSyntheticMatch(player1, player2, context);

            // Extract features (treating player1 as potential winner)
            FeatureVector features = featureExtractor.extractFeatures(syntheticMatch, true);

            // Make prediction using Weka trainer
            double player1WinProbability = trainer.predictWinProbability(features);

            return new MatchPrediction(player1, player2, context, player1WinProbability);
        } catch (Exception e) {
            System.err.println("Prediction failed: " + e.getMessage());
            return new MatchPrediction(player1, player2, context, 0.5); // Default 50-50
        }
    }

    public List<MatchPrediction> predictMatches(List<UpcomingMatch> upcomingMatches) {
        List<MatchPrediction> predictions = new ArrayList<>();

        for (UpcomingMatch upcoming : upcomingMatches) {
            MatchPrediction prediction = predictMatch(upcoming.getPlayer1(),
                    upcoming.getPlayer2(),
                    upcoming.getContext());
            predictions.add(prediction);
        }

        return predictions;
    }

    public void updateWithResults(List<Match> newMatches) {
        for (Match match : newMatches) {
            historyManager.updateWithMatch(match);
        }
    }

    private Match createSyntheticMatch(Player player1, Player player2, MatchContext context) {
        return new Match.Builder()
                .surface(context.getSurface())
                .tourneyLevel(context.getTourneyLevel())
                .tourneyDate(context.getTourneyDate())
                .round(context.getRound())
                .bestOf(context.getBestOf())
                .winner(player1)
                .loser(player2)
                .build();
    }
}