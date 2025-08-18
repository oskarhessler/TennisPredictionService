import weka.classifiers.trees.RandomForest;
import weka.classifiers.evaluation.Evaluation;
import weka.core.*;
import weka.core.converters.ConverterUtils.DataSource;
import java.util.*;

public class WekaTennisTrainer {
    private final RandomForest classifier;
    private Instances header;

    public WekaTennisTrainer() {
        classifier = new RandomForest();
        classifier.setNumIterations(100);
        classifier.setNumFeatures(0); // Use all features
        classifier.setMaxDepth(10);
    }

    public WekaTrainingResult trainModel(List<Match> trainMatches, FeatureExtractor featureExtractor) {
        try {
            // Convert matches to Weka format
            Instances trainingData = createWekaInstances(trainMatches, featureExtractor);
            trainingData.setClassIndex(trainingData.numAttributes() - 1);

            // Train the model
            classifier.buildClassifier(trainingData);

            // Save header for later predictions
            header = new Instances(trainingData, 0);

            // Evaluate (10-fold cross validation)
            Evaluation eval = new Evaluation(trainingData);
            eval.crossValidateModel(classifier, trainingData, 10, new Random(42));

            return new WekaTrainingResult(classifier, eval);

        } catch (Exception e) {
            throw new RuntimeException("Training failed: " + e.getMessage(), e);
        }
    }

    private Instances createWekaInstances(List<Match> matches, FeatureExtractor featureExtractor) {
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No matches provided for training");
        }

        // Get feature structure from first match
        FeatureVector sampleFeatures = featureExtractor.extractFeatures(matches.get(0), true);
        List<String> featureNames = sampleFeatures.getFeatureNames();

        // Create Weka attributes
        ArrayList<Attribute> attributes = new ArrayList<>();
        for (String featureName : featureNames) {
            attributes.add(new Attribute(featureName));
        }

        // Add class attribute (winner: 0 or 1)
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("0"); // loser
        classValues.add("1"); // winner
        attributes.add(new Attribute("class", classValues));

        // Create dataset
        Instances dataset = new Instances("tennis_matches", attributes, matches.size() * 2);
        dataset.setClassIndex(dataset.numAttributes() - 1);

        // Add instances (both winner and loser perspectives)
        for (Match match : matches) {
            try {
                // Winner perspective (positive example)
                FeatureVector winnerFeatures = featureExtractor.extractFeatures(match, true);
                addInstance(dataset, winnerFeatures, 1.0);

                // Loser perspective (negative example)
                FeatureVector loserFeatures = featureExtractor.extractFeatures(match, false);
                addInstance(dataset, loserFeatures, 0.0);
            } catch (Exception e) {
                System.err.println("Error processing match: " + e.getMessage());
                // Continue with other matches
            }
        }

        return dataset;
    }

    private void addInstance(Instances dataset, FeatureVector features, double classValue) {
        double[] values = new double[dataset.numAttributes()];
        List<Double> featureValues = features.getFeatures();

        // Copy feature values (handle NaN and null)
        for (int i = 0; i < featureValues.size() && i < values.length - 1; i++) {
            Double val = featureValues.get(i);
            values[i] = (val != null && !val.isNaN() && !val.isInfinite()) ? val : 0.0;
        }

        // Set class value
        values[values.length - 1] = classValue;

        Instance instance = new DenseInstance(1.0, values);
        dataset.add(instance);
    }

    public double predictWinProbability(FeatureVector features) {
        try {
            if (header == null) {
                throw new IllegalStateException("Model not trained yet!");
            }

            // Create values in same order as training data
            double[] values = new double[header.numAttributes()];
            List<Double> featureValues = features.getFeatures();

            for (int i = 0; i < featureValues.size() && i < values.length - 1; i++) {
                Double val = featureValues.get(i);
                values[i] = (val != null && !val.isNaN() && !val.isInfinite()) ? val : 0.0;
            }

            // Class attribute unknown during prediction
            values[values.length - 1] = Utils.missingValue();

            Instance instance = new DenseInstance(1.0, values);
            instance.setDataset(header);

            double[] probabilities = classifier.distributionForInstance(instance);
            return probabilities[1]; // probability for class "1" (player1 win)

        } catch (Exception e) {
            System.err.println("Prediction failed: " + e.getMessage());
            return 0.5; // default if prediction fails
        }
    }

    public RandomForest getClassifier() {
        return classifier;
    }

    public Instances getHeader() {
        return header;
    }
}