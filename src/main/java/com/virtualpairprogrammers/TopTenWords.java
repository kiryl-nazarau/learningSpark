package com.virtualpairprogrammers;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.util.Arrays;
import java.util.List;

public class TopTenWords {
  public static void printTopTenWords() {
    StopWatch stopwatch = new StopWatch();
    stopwatch.start();
    Logger.getLogger("org.apache").setLevel(Level.WARN);

    SparkConf conf = new SparkConf().setAppName("startingSpark")
        .setMaster("local[*]");
    JavaSparkContext sc = new JavaSparkContext(conf);


    List<Tuple2<Long, String>> result =
        sc.textFile("src/main/resources/subtitles/input.txt")
        .map(sentence -> sentence
            .replaceAll("[^a-zA-Z\\s]", "").toLowerCase())
        .filter(sentence ->
            sentence.trim().length() > 0)
        .flatMap(sentence ->
            Arrays.asList(sentence.split(" ")).iterator())
        .filter(word ->
            word.trim().length() > 0)
        .filter(Util::isNotBoring)
        .mapToPair(word -> new Tuple2<>(word, 1L))
        .reduceByKey(Long::sum)
        .mapToPair(tuple ->
            new Tuple2<>(tuple._2, tuple._1))
        .sortByKey(false)
        .take(10);

    result.forEach(System.out::println);

    stopwatch.stop();
    System.out.println(stopwatch.getTime());
  }

}
