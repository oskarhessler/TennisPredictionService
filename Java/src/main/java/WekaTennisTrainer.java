import weka.classifiers.trees.RandomForest;
import weka.classifiers.evaluation.Evaluation;
import weka.core.*;
import weka.core.converters.ConverterUtils.DataSource;
import java.util.*;

public class WekaTennisTrainer {
    private RandomForest classifier;
    private Instances header;

    public WekaTennisTrainer() {
        classifier = new RandomForest();
        classifier.setNumIterations(100);
        classifier.setNumFeatures(0); // Use all features
        classifier.setMaxDepth(10);
    }

    // New constructor to use a pre-trained model
    public WekaTennisTrainer(RandomForest preTrainedModel, Instances headerTemplate) {
        this.classifier = preTrainedModel;
        this.header = headerTemplate;
    }

    // Static method to create trainer from saved model and feature structure
    public static WekaTennisTrainer fromPreTrainedModel(RandomForest model, List<Match> sampleMatches, FeatureExtractor featureExtractor) {
        try {
            // Create header from sample data
            Instances header = createHeaderFromSamples(sampleMatches, featureExtractor);
            return new WekaTennisTrainer(model, header);
        } catch (Exception e) {
            System.err.println("Failed to create trainer from pre-trained model: " + e.getMessage());
            return new WekaTennisTrainer(); // fallback to empty trainer
        }
    }

    private static Instances createHeaderFromSamples(List<Match> matches, FeatureExtractor featureExtractor) {
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No sample matches provided");
        }

        // Get feature structure from first match
        FeatureVector sampleFeatures = featureExtractor.extractFeatures(matches.get(0), true);
        List<String> featureNames = sampleFeatures.getFeatureNames();

        // Create Weka attributes
        ArrayList<Attribute> attributes = new ArrayList<>();
        for (String featureName : featureNames) {
            attributes.add(new Attribute(featureName));
        }

        // Add class attribute
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("0");
        classValues.add("1");
        attributes.add(new Attribute("class", classValues));

        // Create empty dataset with proper structure
        Instances header = new Instances("tennis_matches", attributes, 0);
        header.setClassIndex(header.numAttributes() - 1);

        return header;
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

    public WekaTrainingResult trainModelWithProgress(List<Match> trainMatches, FeatureExtractor featureExtractor) {
        try {
            // 1. Convert matches to Weka Instances
            Instances trainingData = createWekaInstances(trainMatches, featureExtractor);
            trainingData.setClassIndex(trainingData.numAttributes() - 1);
            header = new Instances(trainingData, 0); // save header

            // 2. Train RandomForest with progress
            int numTrees = classifier.getNumIterations();
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < numTrees; i++) {
                // Build the forest so far
                classifier.setNumIterations(i + 1);
                classifier.buildClassifier(trainingData);

                // Update progress bar for training
                long elapsed = System.currentTimeMillis() - startTime;
                double avgPerTree = elapsed / (double) (i + 1);
                long remaining = (long) (avgPerTree * (numTrees - i - 1));
                WekaTennisPredictionSystem.printProgressBar(i + 1, numTrees, remaining, "Training");
            }

            // 3. Evaluate with progress (10-fold CV)
            int folds = 10;
            Evaluation eval = new Evaluation(trainingData);
            Random rand = new Random(42);
            trainingData.randomize(rand);
            if (trainingData.classAttribute().isNominal()) {
                trainingData.stratify(folds);
            }

            startTime = System.currentTimeMillis();
            for (int i = 0; i < folds; i++) {
                Instances train = trainingData.trainCV(folds, i, rand);
                Instances test = trainingData.testCV(folds, i);

                RandomForest foldForest = new RandomForest();
                foldForest.setOptions(classifier.getOptions());
                foldForest.buildClassifier(train);

                eval.evaluateModel(foldForest, test);

                // Progress + ETA for evaluation
                long elapsed = System.currentTimeMillis() - startTime;
                double avgPerFold = elapsed / (double) (i + 1);
                long remaining = (long) (avgPerFold * (folds - i - 1));
                WekaTennisPredictionSystem.printProgressBar(i + 1, folds, remaining, "Evaluation");
            }

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

    public boolean isModelTrained() {
        return header != null && classifier != null;
    }

    public RandomForest getClassifier() {
        return classifier;
    }

    public Instances getHeader() {
        return header;
    }
}