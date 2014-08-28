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

Indexer usage info:
  ```
  java -cp ./bin/wikisearch-1.0-SNAPSHOT-jar-with-dependencies.jar wikisearch.WikiIndexer 
  ```


Example: index articles from file enwiki-latest-pages-articles.xml into enwiki folder in current directory:

  ```
  java -cp ./bin/wikisearch-1.0-SNAPSHOT-jar-with-dependencies.jar wikisearch.WikiIndexer --input=enwiki-latest-pages-articles.xml --directory=./enwiki
  ```

Searcher usage info:

  ```
  java -cp ./bin/wikisearch-1.0-SNAPSHOT-jar-with-dependencies.jar wikisearch.WikiSearcher
  ```

Example: search by contributor name "arm" and specify "./enwiki" as the location of lucene index files:

  ```
  java -cp ./bin/wikisearch-1.0-SNAPSHOT-jar-with-dependencies.jar wikisearch.WikiSearcher --contributor=arm --directory=./enwiki
  ```


Initial solution was based on http://www.docjar.com/html/api/org/apache/lucene/benchmark/utils/ExtractWikipedia.java.html. Some code was ripped from there.
