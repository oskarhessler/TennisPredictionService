import weka.classifiers.trees.RandomForest;
import weka.classifiers.evaluation.Evaluation;
import weka.core.SerializationHelper;

/**
 * Weka-based training result (replacement for XGBoost TrainingResult)
 */
public class WekaTrainingResult {
    private final RandomForest model;
    private final Evaluation evaluation;

    public WekaTrainingResult(RandomForest model, Evaluation evaluation) {
        this.model = model;
        this.evaluation = evaluation;
    }

    public RandomForest getModel() {
        return model;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public double getAccuracy() {
        try {
            return evaluation.pctCorrect() / 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public double getLogLoss() {
        try {
            // Weka doesn't have direct log loss, use error rate as approximation
            return evaluation.errorRate();
        } catch (Exception e) {
            return 1.0;
        }
    }

    public void saveModel(String filePath) {
        try {
            SerializationHelper.write(filePath, model);
            System.out.println("Model saved to: " + filePath);
        } catch (Exception e) {
            System.err.println("Failed to save model: " + e.getMessage());
        }
    }

    public static RandomForest loadModel(String filePath) {
        try {
            return (RandomForest) SerializationHelper.read(filePath);
        } catch (Exception e) {
            System.err.println("Failed to load model: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        try {
            return String.format("Accuracy: %.3f (%.1f%%), Error Rate: %.4f",
                    getAccuracy(), evaluation.pctCorrect(), evaluation.errorRate());
        } catch (Exception e) {
            return "Evaluation unavailable";
        }
    }
}