import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import java.util.*;

/**
 * Prepares training data for XGBoost from tennis matches
 */
public class TrainingDataPreparer {
    private final FeatureExtractor featureExtractor;
    private final boolean useSymmetricAugmentation;

    public TrainingDataPreparer(FeatureExtractor featureExtractor, boolean useSymmetricAugmentation) {
        this.featureExtractor = featureExtractor;
        this.useSymmetricAugmentation = useSymmetricAugmentation;
    }

    public DMatrix prepareDMatrix(List<Match> matches) throws XGBoostError {
        List<FeatureVector> featureVectors = new ArrayList<>();
        List<Float> labels = new ArrayList<>();

        for (Match match : matches) {
            // Extract features with winner as player 1 (positive example)
            FeatureVector winnerFeatures = featureExtractor.extractFeatures(match, true);
            featureVectors.add(winnerFeatures);
            labels.add(1.0f);

            if (useSymmetricAugmentation) {
                // Extract features with loser as player 1 (negative example)
                FeatureVector loserFeatures = featureExtractor.extractFeatures(match, false);
                featureVectors.add(loserFeatures);
                labels.add(0.0f);
            }
        }

        if (featureVectors.isEmpty()) {
            throw new IllegalStateException("No feature vectors generated from matches");
        }

        // Convert to matrices
        int numRows = featureVectors.size();
        int numCols = featureVectors.get(0).getFeatureCount();

        // Handle missing values (impute with column means)
        float[][] dataMatrix = imputeMissingValues(featureVectors, numRows, numCols);

        // Flatten to 1D array for XGBoost
        float[] flatData = new float[numRows * numCols];
        for (int i = 0; i < numRows; i++) {
            System.arraycopy(dataMatrix[i], 0, flatData, i * numCols, numCols);
        }

        // Convert labels
        float[] labelArray = new float[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            labelArray[i] = labels.get(i);
        }

        // Create and configure DMatrix
        DMatrix dmatrix = new DMatrix(flatData, numRows, numCols, Float.NaN);
        dmatrix.setLabel(labelArray);

        return dmatrix;
    }

    private float[][] imputeMissingValues(List<FeatureVector> vectors, int numRows, int numCols) {
        // First pass: calculate column means (excluding NaN values)
        double[] columnSums = new double[numCols];
        int[] columnCounts = new int[numCols];

        for (FeatureVector vector : vectors) {
            float[] features = vector.toFloatArray();
            for (int col = 0; col < numCols; col++) {
                if (!Float.isNaN(features[col])) {
                    columnSums[col] += features[col];
                    columnCounts[col]++;
                }
            }
        }

        float[] columnMeans = new float[numCols];
        for (int col = 0; col < numCols; col++) {
            columnMeans[col] = columnCounts[col] > 0 ? (float)(columnSums[col] / columnCounts[col]) : 0.0f;
        }

        // Second pass: create matrix with imputed values
        float[][] dataMatrix = new float[numRows][numCols];
        for (int row = 0; row < numRows; row++) {
            float[] features = vectors.get(row).toFloatArray();
            for (int col = 0; col < numCols; col++) {
                dataMatrix[row][col] = Float.isNaN(features[col]) ? columnMeans[col] : features[col];
            }
        }

        return dataMatrix;
    }

    public List<String> getFeatureNames() {
        // Get feature names from first processed match (assuming consistent structure)
        return featureExtractor.extractFeatures(new Match.Builder().build(), true).getFeatureNames();
    }
}