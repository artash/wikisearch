Download wikipedia latest dump
-------------
  ```
  aria2c -x5 http://download.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2
  ```
  

Building a flat jar
------------

  ```
  mvn clean compile assembly:single
  ```

Running the indexer
------------

  ```
  java -jar ./target/wikisearch-1.0-SNAPSHOT-jar-with-dependencies.jar -i enwiki-latest-pages-articles.xml
  ```

Based on http://www.docjar.com/html/api/org/apache/lucene/benchmark/utils/ExtractWikipedia.java.html
