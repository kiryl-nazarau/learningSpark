package com.virtualpairprogrammers;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.relaxng.datatype.DatatypeStreamingValidator;

import static org.apache.spark.sql.functions.*;

public class ExamResults {
  public static void main(String[] args) {
    Logger.getLogger("org.apache").setLevel(Level.WARN);

    SparkSession spark = SparkSession.builder()
        .appName("testingSql")
        .master("local[*]").getOrCreate();

    Dataset<Row> dataset = spark.read().option("header", true)
        .csv("src/main/resources/exams/students.csv");

//    dataset = dataset.groupBy("subject")
//        .agg(max(col("score")).as("max_score"),
//              min(col("score")).as("min_score"));

//    dataset = dataset.select(col("subject"),
//            date_format(col("datetime"), "MMMM").as("month"),
//            date_format(col("datetime"), "MM").as("monthnum"));

    dataset = dataset.groupBy("subject").pivot("year")
        .agg(round(avg("score"), 2).as("avg"),
            round(stddev("score"), 2).as("std"));

    dataset.show(1000);

  }
}
