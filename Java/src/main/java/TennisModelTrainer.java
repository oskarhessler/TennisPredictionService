import ml.dmlc.xgboost4j.java.XGBoostError;
import ml.dmlc.xgboost4j.java.*;

import java.util.*;

/**
 * Handles XGBoost model training and evaluation
 */
public class TennisModelTrainer {
    private final Map<String, Object> defaultParams;

    public TennisModelTrainer() {
        defaultParams = new HashMap<>();
        defaultParams.put("objective", "binary:logistic");
        defaultParams.put("eval_metric", "logloss");
        defaultParams.put("eta", 0.05);
        defaultParams.put("max_depth", 6);
        defaultParams.put("min_child_weight", 3);
        defaultParams.put("subsample", 0.8);
        defaultParams.put("colsample_bytree", 0.8);
        defaultParams.put("gamma", 0.1);
        defaultParams.put("alpha", 0.1);
        defaultParams.put("lambda", 1.0);
        defaultParams.put("random_state", 42);
    }

    public TrainingResult trainModel(DMatrix trainData, DMatrix validData, int numRounds) throws XGBoostError {
        Map<String, DMatrix> watches = new HashMap<>();
        watches.put("train", trainData);
        if (validData != null) {
            watches.put("valid", validData);
        }

        int earlyStoppingRounds = Math.max(10, numRounds / 20); // Early stopping after 5% of rounds without improvement

        Booster model = XGBoost.train(trainData, defaultParams, numRounds, watches,
                null, null, null, earlyStoppingRounds);

        // Evaluate on validation set if provided
        ModelEvaluation evaluation = null;
        if (validData != null) {
            evaluation = evaluateModel(model, validData);
        }

        return new TrainingResult(model, evaluation, getFeatureImportance(model));
    }

    public TrainingResult trainModel(DMatrix trainData, int numRounds) throws XGBoostError {
        return trainModel(trainData, null, numRounds);
    }

    private ModelEvaluation evaluateModel(Booster model, DMatrix testData) throws XGBoostError {
        float[][] predictions = model.predict(testData);
        float[] labels = testData.getLabel();

        int correct = 0;
        double totalLogLoss = 0.0;

        for (int i = 0; i < predictions.length; i++) {
            float probability = predictions[i][0];
            int predictedLabel = probability >= 0.5 ? 1 : 0;
            int actualLabel = (int) labels[i];

            if (predictedLabel == actualLabel) correct++;

            // Calculate log loss: -[y*log(p) + (1-y)*log(1-p)]
            double p = Math.max(1e-15, Math.min(1 - 1e-15, probability)); // Clip to avoid log(0)
            double logLoss = -(actualLabel * Math.log(p) + (1 - actualLabel) * Math.log(1 - p));
            totalLogLoss += logLoss;
        }

        double accuracy = (double) correct / predictions.length;
        double avgLogLoss = totalLogLoss / predictions.length;

        return new ModelEvaluation(accuracy, avgLogLoss, predictions, labels);
    }

    private Map<String, Integer> getFeatureImportance(Booster model) {
        try {
            return model.getFeatureScore((String[]) null);
        } catch (XGBoostError e) {
            System.err.println("Could not get feature importance: " + e.getMessage());
            return new HashMap<>();
        }
    }
}