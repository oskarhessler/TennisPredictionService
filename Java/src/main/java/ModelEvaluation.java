public class ModelEvaluation {
    private final double accuracy;
    private final double logLoss;
    private final float[][] predictions;
    private final float[] actualLabels;

    public ModelEvaluation(double accuracy, double logLoss, float[][] predictions, float[] actualLabels) {
        this.accuracy = accuracy;
        this.logLoss = logLoss;
        this.predictions = predictions;
        this.actualLabels = actualLabels;
    }

    public double getAccuracy() { return accuracy; }
    public double getLogLoss() { return logLoss; }
    public float[][] getPredictions() { return predictions; }
    public float[] getActualLabels() { return actualLabels; }

    @Override
    public String toString() {
        return String.format("Accuracy: %.3f (%.1f%%), Log Loss: %.4f",
                accuracy, accuracy * 100, logLoss);
    }
}