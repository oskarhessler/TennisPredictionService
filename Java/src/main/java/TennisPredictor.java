import ml.dmlc.xgboost4j.java.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for making predictions on new tennis matches
 */
public class TennisPredictor {
    private final Booster model;
    private final FeatureExtractor featureExtractor;
    private final PlayerHistoryManager historyManager;

    public TennisPredictor(Booster model, FeatureExtractor featureExtractor, PlayerHistoryManager historyManager) {
        this.model = model;
        this.featureExtractor = featureExtractor;
        this.historyManager = historyManager;
    }

    public static TennisPredictor loadFromFile(String modelPath, PlayerHistoryManager historyManager) throws XGBoostError {
        Booster model = XGBoost.loadModel(modelPath);
        FeatureExtractor featureExtractor = new FeatureExtractor(historyManager);
        return new TennisPredictor(model, featureExtractor, historyManager);
    }

    /**
     * Predict the probability that player1 beats player2
     */
    public MatchPrediction predictMatch(Player player1, Player player2, MatchContext context) throws XGBoostError {
        // Create a synthetic match for feature extraction
        Match syntheticMatch = createSyntheticMatch(player1, player2, context);

        // Extract features (treating player1 as potential winner)
        FeatureVector features = featureExtractor.extractFeatures(syntheticMatch, true);

        // Convert to DMatrix
        float[] featureArray = features.toFloatArray();
        DMatrix predMatrix = new DMatrix(featureArray, 1, features.getFeatureCount(), Float.NaN);

        // Make prediction
        float[][] predictions = model.predict(predMatrix);
        double player1WinProbability = predictions[0][0];

        return new MatchPrediction(player1, player2, context, player1WinProbability);
    }

    /**
     * Batch predict multiple matches
     */
    public List<MatchPrediction> predictMatches(List<UpcomingMatch> upcomingMatches) throws XGBoostError {
        List<MatchPrediction> predictions = new ArrayList<>();

        for (UpcomingMatch upcoming : upcomingMatches) {
            MatchPrediction prediction = predictMatch(upcoming.getPlayer1(), upcoming.getPlayer2(), upcoming.getContext());
            predictions.add(prediction);
        }

        return predictions;
    }

    /**
     * Update the predictor with new match results (for maintaining player histories)
     */
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
                .winner(player1)  // Just for feature extraction structure
                .loser(player2)   // Doesn't imply actual outcome
                .build();
    }
}