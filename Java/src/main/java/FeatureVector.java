import java.util.ArrayList;
import java.util.List;

public class FeatureVector {
    private final List<Double> features;
    private final List<String> featureNames;

    public FeatureVector(List<Double> features, List<String> featureNames) {
        this.features = new ArrayList<>(features);
        this.featureNames = new ArrayList<>(featureNames);
    }

    public List<Double> getFeatures() {
        return new ArrayList<>(features);
    }

    public List<String> getFeatureNames() {
        return new ArrayList<>(featureNames);
    }

    public int getFeatureCount() {
        return features.size();
    }

    public double[] toDoubleArray() {
        return features.stream().mapToDouble(d -> d != null ? d : Double.NaN).toArray();
    }

    public float[] toFloatArray() {
        float[] result = new float[features.size()];
        for (int i = 0; i < features.size(); i++) {
            Double value = features.get(i);
            result[i] = value != null ? value.floatValue() : Float.NaN;
        }
        return result;
    }
}