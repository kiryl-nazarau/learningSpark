package com.virtualpairprogrammers.ml;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class HousePriceFields {
  public static void main(String[] args) {
    Logger.getLogger("org.apache").setLevel(Level.WARN);

    SparkSession spark = SparkSession.builder()
        .appName("House Price Analysis")
        .master("local[*]").getOrCreate();

    Dataset<Row> csvData = spark.read().option("header", true)
        .option("inferSchema", true)
        .csv("src/main/resources/ml/kc_house_data.csv");

//    csvData.describe().show();

    csvData = csvData
        .drop("id", "date", "waterfront", "view", "condition",
            "grade", "yr_renovated", "zipcode", "lat", "long",
            "sqft_lot", "yr_built", "sqft_lot15", "sqft_living15");
    for (String column1 : csvData.columns()) {
      for (String column2 : csvData.columns()) {
        System.out.println(column1 + " - " + column2 +
            " correlation  is "
            + csvData.stat().corr(column1, column2));
      }
    }
  }
}
