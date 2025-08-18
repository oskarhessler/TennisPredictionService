import weka.classifiers.trees.RandomForest;
import weka.classifiers.evaluation.Evaluation;
import weka.core.*;
import weka.core.converters.ConverterUtils.DataSource;

import java.util.*;

/**
 * Weka-based tennis prediction trainer (replacement for XGBoost)
 */
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
            // Winner perspective (positive example)
            FeatureVector winnerFeatures = featureExtractor.extractFeatures(match, true);
            addInstance(dataset, winnerFeatures, 1.0);

            // Loser perspective (negative example)
            FeatureVector loserFeatures = featureExtractor.extractFeatures(match, false);
            addInstance(dataset, loserFeatures, 0.0);
        }

        return dataset;
    }

    private void addInstance(Instances dataset, FeatureVector features, double classValue) {
        double[] values = new double[dataset.numAttributes()];
        List<Double> featureValues = features.getFeatures();

        // Copy feature values (handle NaN)
        for (int i = 0; i < featureValues.size(); i++) {
            Double val = featureValues.get(i);
            values[i] = (val != null && !val.isNaN()) ? val : 0.0; // Simple imputation
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

            // Skapa värden i samma ordning som i trainingData/header
            double[] values = new double[header.numAttributes()];
            List<Double> featureValues = features.getFeatures();

            for (int i = 0; i < featureValues.size(); i++) {
                Double val = featureValues.get(i);
                values[i] = (val != null && !val.isNaN()) ? val : 0.0;
            }

            // Klassattributet okänt vid prediction
            values[values.length - 1] = Utils.missingValue();

            Instance instance = new DenseInstance(1.0, values);
            instance.setDataset(header); // ← Kopplar instansen till rätt dataset-header

            double[] probabilities = classifier.distributionForInstance(instance);
            return probabilities[1]; // sannolikhet för class "1" (player1 win)

        } catch (Exception e) {
            System.err.println("Prediction failed: " + e.getMessage());
            return 0.5; // default om det failar
        }
    }


    class WekaTrainingResult {
    private final RandomForest model;
    private final Evaluation evaluation;

    public WekaTrainingResult(RandomForest model, Evaluation evaluation) {
        this.model = model;
        this.evaluation = evaluation;
    }

    public RandomForest getModel() { return model; }
    public Evaluation getEvaluation() { return evaluation; }

    public double getAccuracy() {
        try {
            return evaluation.pctCorrect() / 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public String toString() {
        try {
            return String.format("Accuracy: %.3f (%.1f%%)",
                    getAccuracy(), evaluation.pctCorrect());
        } catch (Exception e) {
            return "Evaluation unavailable";
        }
    }
}