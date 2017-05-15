# java-lda
Package provides java implementation of the latent dirichlet allocation (LDA) for topic modelling

[![Build Status](https://travis-ci.org/chen0040/java-lda.svg?branch=master)](https://travis-ci.org/chen0040/java-lda) [![Coverage Status](https://coveralls.io/repos/github/chen0040/java-lda/badge.svg?branch=master)](https://coveralls.io/github/chen0040/java-lda?branch=master)

# Install

```xml
<dependency>
  <groupId>com.github.chen0040</groupId>
  <artifactId>java-lda</artifactId>
  <version>1.0.1</version>
</dependency>
```


# Usage

The sample code belows created a LDA which takes in the texts stored in "docs" list and created 20 different topics from these texts: 

```java
import com.github.chen0040.data.utils.TupleTwo;
import com.github.chen0040.lda.Lda;

List<String> docs = Arrays.asList("[paragraph1]", "[paragraph2]", ..., "[paragraphN]");

Lda method = new Lda();
method.setTopicCount(20);
method.setMaxVocabularySize(100000);

LdaResult result = method.fit(docs);

System.out.println("Topic Count: "+result.topicCount());

for(int topicIndex = 0; topicIndex < topicCount; ++topicIndex){
 String topicSummary = result.topicSummary(topicIndex);
 List<TupleTwo<String, Integer>> topKeyWords = result.topKeyWords(topicIndex, 10);
 List<TupleTwo<Doc, Double>> topStrings = result.topDocuments(topicIndex, 5);

 System.out.println("Topic #" + (topicIndex+1) + ": " + topicSummary);

 for(TupleTwo<String, Integer> entry : topKeyWords){
    String keyword = entry._1();
    int score = entry._2();
    System.out.println("Keyword: " + keyword + "(" + score + ")");
 }

 for(TupleTwo<Doc, Double> entry : topStrings){
    double score = entry._2();
    int docIndex = entry._1().getDocIndex();
    String docContent = entry._1().getContent();
    System.out.println("Doc (" + docIndex + ", " + score + ")): " + docContent);
 }
}
```
