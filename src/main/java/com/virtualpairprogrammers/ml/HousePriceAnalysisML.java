package com.virtualpairprogrammers.ml;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.feature.OneHotEncoderEstimator;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.ml.tuning.ParamGridBuilder;
import org.apache.spark.ml.tuning.TrainValidationSplit;
import org.apache.spark.ml.tuning.TrainValidationSplitModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.col;

public class HousePriceAnalysisML {
  public static void main(String[] args) {
    Logger.getLogger("org.apache").setLevel(Level.WARN);

    SparkSession spark = SparkSession.builder()
        .appName("House Price Analysis")
        .master("local[*]").getOrCreate();

    Dataset<Row> csvData = spark.read().option("header", true)
        .option("inferSchema", true)
        .csv("src/main/resources/ml/kc_house_data.csv");

    csvData = csvData.withColumn("sqft_above_percentage",
        col("sqft_above").divide(col("sqft_living")))
    .withColumnRenamed("price", "label");

    Dataset<Row>[] dataSplits =
        csvData.randomSplit(new double[]{0.8, 0.2});
    Dataset<Row> trainingAndTestData = dataSplits[0];
    Dataset<Row> holdOnData = dataSplits[1];

    StringIndexer conditionIndexer = new StringIndexer();
    conditionIndexer.setInputCol("condition")
        .setOutputCol("conditionIndex");

    StringIndexer gradeIndexer = new StringIndexer();
    gradeIndexer.setInputCol("grade")
        .setOutputCol("gradeIndex");

    StringIndexer zipcodeIndexer = new StringIndexer();
    zipcodeIndexer.setInputCol("zipcode")
        .setOutputCol("zipcodeIndex");

    OneHotEncoderEstimator encoder =
        new OneHotEncoderEstimator();
    encoder.setInputCols(new String[] {"conditionIndex",
                        "gradeIndex", "zipcodeIndex"})
        .setOutputCols(new String[] {"conditionVector",
            "gradeVector", "zipcodeVector"});

    VectorAssembler vectorAssembler = new VectorAssembler();
    vectorAssembler
        .setInputCols(new String[]
            {"bedrooms", "bathrooms", "sqft_living",
                "sqft_above_percentage", "floors",
            "conditionVector", "gradeVector", "zipcodeVector",
            "waterfront"})
        .setOutputCol("features");

    LinearRegression linearRegression =
        new LinearRegression();
    ParamGridBuilder paramGridBuilder = new ParamGridBuilder();

    ParamMap[] paramMaps = paramGridBuilder
        .addGrid(linearRegression.regParam(),
            new double[] {0.01, 0.1, 0.5})
        .addGrid(linearRegression.elasticNetParam(),
            new double[] {0, 0.5, 1})
        .build();

    TrainValidationSplit trainValidationSplit =
        new TrainValidationSplit()
        .setEstimator(linearRegression)
        .setEvaluator(new RegressionEvaluator()
            .setMetricName("r2"))
        .setEstimatorParamMaps(paramMaps)
        .setTrainRatio(0.8);

    Pipeline pipeline = new Pipeline();
    pipeline.setStages(new PipelineStage[] {
        conditionIndexer, gradeIndexer, zipcodeIndexer, encoder,
        vectorAssembler, trainValidationSplit});

    PipelineModel pipelineModel =
        pipeline.fit(trainingAndTestData);

    TrainValidationSplitModel model =
        (TrainValidationSplitModel) pipelineModel.stages()[5];

    LinearRegressionModel lrModel
        = (LinearRegressionModel) model.bestModel();

    Dataset<Row> holOnResults =
        pipelineModel.transform(holdOnData);
    holOnResults.show();
    holOnResults = holOnResults.drop("prediction");

    System.out.println(lrModel.summary().r2() + " " +
        lrModel.summary().rootMeanSquaredError());

    System.out.println(lrModel.evaluate(holOnResults).r2() + " " +
        lrModel.evaluate(holOnResults).rootMeanSquaredError());

    System.out.println(lrModel.coefficients() + " " +
        lrModel.intercept());

    System.out.println(lrModel.getRegParam() + " " +
        lrModel.getElasticNetParam());

//    model.transform(holdOnData).show();

    spark.close();

  }
}
