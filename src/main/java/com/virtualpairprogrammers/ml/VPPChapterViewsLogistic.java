package com.virtualpairprogrammers.ml;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.ml.classification.LogisticRegression;
import org.apache.spark.ml.classification.LogisticRegressionModel;
import org.apache.spark.ml.classification.LogisticRegressionSummary;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.feature.OneHotEncoderEstimator;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.ml.tuning.ParamGridBuilder;
import org.apache.spark.ml.tuning.TrainValidationSplit;
import org.apache.spark.ml.tuning.TrainValidationSplitModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.when;

public class VPPChapterViewsLogistic {
  public static void main(String[] args) {

    Logger.getLogger("org.apache").setLevel(Level.WARN);

    SparkSession spark = SparkSession.builder()
        .appName("VPP Chapter Views")
        .master("local[*]").getOrCreate();

    Dataset<Row> csvData = spark.read().option("header", true)
        .option("inferSchema", true)
        .csv("src/main/resources/ml/vppChapterViews/*.csv");

    csvData = csvData.filter("is_cancelled = false")
        .drop("observation_date", "is_cancelled");

    // 1 - customers watched no videos, 0 - customers watched
    // some videos

    csvData = csvData
        .withColumn("firstSub",
            when(col("firstSub").isNull(), 0)
                .otherwise(col("firstSub")))
        .withColumn("all_time_views",
            when(col("all_time_views").isNull(), 0)
                .otherwise(col("all_time_views")))
        .withColumn("last_month_views",
            when(col("last_month_views").isNull(), 0)
                .otherwise(col("last_month_views")))
        .withColumn("next_month_views",
            when(col("next_month_views")
                    .$greater(0), 0).otherwise(1));

    csvData = csvData
        .withColumnRenamed("next_month_views",
            "label");

    StringIndexer payMethodIndexer = new StringIndexer();
    csvData = payMethodIndexer.setInputCol("payment_method_type")
        .setOutputCol("payIndex")
        .fit(csvData).transform(csvData);

    StringIndexer countryIndexer = new StringIndexer();
    csvData = countryIndexer.setInputCol("country")
        .setOutputCol("countryIndex")
        .fit(csvData).transform(csvData);

    StringIndexer periodIndexer = new StringIndexer();
    csvData = periodIndexer.setInputCol("rebill_period_in_months")
        .setOutputCol("periodIndex")
        .fit(csvData).transform(csvData);

    OneHotEncoderEstimator encoder =
        new OneHotEncoderEstimator();
    csvData = encoder.setInputCols(new String[] {"payIndex",
        "countryIndex", "periodIndex"})
        .setOutputCols(new String[] {"payVector",
            "countryVector", "periodVector"})
        .fit(csvData).transform(csvData);

    VectorAssembler vectorAssembler = new VectorAssembler();
    Dataset<Row> inputData = vectorAssembler
        .setInputCols(new String[]
            {"firstSub", "age", "all_time_views",
                "last_month_views", "payVector",
                "countryVector", "periodVector"})
        .setOutputCol("features")
        .transform(csvData).select("label", "features");

    Dataset<Row>[] dataSplits =
        inputData.randomSplit(new double[]{0.9, 0.1});
    Dataset<Row> trainingAndTestData = dataSplits[0];
    Dataset<Row> holdOutData = dataSplits[1];

    LogisticRegression logisticRegression =
        new LogisticRegression();
    ParamGridBuilder paramGridBuilder = new ParamGridBuilder();
    ParamMap[] paramMaps = paramGridBuilder.addGrid(logisticRegression
        .regParam(), new double[] {0.01, 0.1, 0.3, 0.5, 0.7, 1})
        .addGrid(logisticRegression.elasticNetParam(),
            new double[] {0, 0.5, 1})
        .build();

    TrainValidationSplit tvs = new TrainValidationSplit()
        .setEstimator(logisticRegression)
        .setEvaluator(new RegressionEvaluator()
            .setMetricName("r2"))
        .setEstimatorParamMaps(paramMaps)
        .setTrainRatio(0.9);

    TrainValidationSplitModel model =
        tvs.fit(trainingAndTestData);

    LogisticRegressionModel lrModel
        = (LogisticRegressionModel) model.bestModel();

    System.out.println("Accuracy value is " + lrModel.summary()
        .accuracy());

    System.out.println(lrModel.coefficients() + " " +
        lrModel.intercept());

    System.out.println(lrModel.getRegParam() + " " +
        lrModel.getElasticNetParam());

    LogisticRegressionSummary summary
        = lrModel.evaluate(holdOutData);

    double truePositives =
        summary.truePositiveRateByLabel()[1];

    double falsePositives =
        summary.falsePositiveRateByLabel()[0];

    System.out.println("For the holdout data the likelhood" +
        " of a positive being correct is " +
        truePositives / (truePositives + falsePositives));

    System.out.println("The holdout accuracy is " +
        summary.accuracy());

    lrModel.transform(holdOutData).groupBy("label",
        "prediction").count().show();



    spark.close();
  }
}
