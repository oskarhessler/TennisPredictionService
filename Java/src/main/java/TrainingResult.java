import ml.dmlc.xgboost4j.java.XGBoostError;
import ml.dmlc.xgboost4j.scala.Booster;

import java.util.Map;

public class TrainingResult {
    private final Booster model;
    private final ModelEvaluation evaluation;
    private final Map<String, Integer> featureImportance;

    public TrainingResult(Booster model, ModelEvaluation evaluation, Map<String, Integer> featureImportance) {
        this.model = model;
        this.evaluation = evaluation;
        this.featureImportance = featureImportance;
    }

    public Booster getModel() { return model; }
    public ModelEvaluation getEvaluation() { return evaluation; }
    public Map<String, Integer> getFeatureImportance() { return featureImportance; }

    public void saveModel(String filePath) throws XGBoostError {
        model.saveModel(filePath);
    }
}