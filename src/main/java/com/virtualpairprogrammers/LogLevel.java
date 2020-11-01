package com.virtualpairprogrammers;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import static org.apache.spark.sql.functions.*;

public class LogLevel {
  public static void main(String[] args) {
    Logger.getLogger("org.apache").setLevel(Level.WARN);

    SparkSession spark = SparkSession.builder()
        .appName("testingSql")
        .master("local[*]").getOrCreate();

    Dataset<Row> dataset = spark.read().option("header", true)
        .csv("src/main/resources/extras/biglog.txt");

//    Dataset<Row> results = spark.sql("select level, " +
//        "date_format(datetime, 'MMMM') as month, " +
//        "count(1) as total " +
//        "from logging_table " +
//        "group by level, month " +
//        "order by first(date_format(datetime, 'MM')), level");

//    dataset = dataset
//      .select(col("level"),
//        date_format(col("datetime"), "MMMM").as("month"),
//        date_format(col("datetime"), "MM").as("monthnum"))
//      .groupBy(col("level"), col("month"), col("monthnum"))
//      .count()
//      .orderBy(col("monthnum"), col("level"))
//      .drop(col("monthnum"));

    dataset = dataset
        .select(col("level"),
            date_format(col("datetime"), "MMMM").as("month"),
            date_format(col("datetime"), "MM").as("monthnum"));

    dataset = dataset.groupBy("level").pivot("monthnum")
        .count().na().fill(0);

    dataset.show();

  }

}
