package com.virtualpairprogrammers.ml;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.ml.feature.OneHotEncoderEstimator;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class GymCompetitorsML {

  public static void main(String[] args) {
    Logger.getLogger("org.apache").setLevel(Level.WARN);

    SparkSession spark = SparkSession.builder()
        .appName("Gym Competitors")
        .master("local[*]").getOrCreate();

    Dataset<Row> csvData = spark.read().option("header", true)
        .option("inferSchema", true)
        .csv("src/main/resources/ml/GymCompetition.csv");

    StringIndexer genderIndexer = new StringIndexer();
    genderIndexer.setInputCol("Gender")
        .setOutputCol("GenderIndex");
    csvData = genderIndexer.fit(csvData).transform(csvData);

    OneHotEncoderEstimator genderEncoder =
        new OneHotEncoderEstimator();
    genderEncoder.setInputCols(new String[] {"GenderIndex"})
        .setOutputCols(new String[] {"GenderVector"});
    csvData = genderEncoder.fit(csvData).transform(csvData);


    VectorAssembler vectorAssembler = new VectorAssembler();
    vectorAssembler
        .setInputCols(new String[] {"Age", "Height",
            "Weight", "GenderVector"});
    vectorAssembler
        .setOutputCol("features");
    Dataset<Row> csvDataWithFeature =
        vectorAssembler.transform(csvData);

    Dataset<Row> modelInputData =
        csvDataWithFeature.select("NoOfReps", "features")
            .withColumnRenamed("NoOfReps", "label");

    LinearRegression linearRegression =
        new LinearRegression();

    LinearRegressionModel model =
        linearRegression.fit(modelInputData);
    System.out.println(model.intercept() + " " +
                            model.coefficients());

    model.transform(modelInputData).show();

    spark.close();

  }
}
