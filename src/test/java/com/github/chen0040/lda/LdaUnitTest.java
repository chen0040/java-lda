package com.github.chen0040.lda;


import com.github.chen0040.data.utils.TupleTwo;
import com.github.chen0040.lda.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by xschen on 9/5/2017.
 */
public class LdaUnitTest {
   private static final Logger logger = LoggerFactory.getLogger(LdaUnitTest.class);


   private List<String> getDocs() throws IOException {

      InputStream inputStream = FileUtils.getResource("documents.txt");

      List<String> docs = new ArrayList<>();

      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      reader.lines().forEach(line->{
         line = line.trim();
         if(line.equals("")) return;

         String[] fields = line.split("\t");
         String text = fields[0];
         if (fields.length == 3) {
            text = fields[2];
         }
         docs.add(text);
      });
      reader.close();

      return docs;
   }

   @Test
   public void testLDA() throws IOException {

      List<String> docs = getDocs();

      Lda method = new Lda();
      method.setTopicCount(20);
      method.setMaxVocabularySize(100000);
      LdaResult result = method.fit(docs);

      int topicCount = result.topicCount();

      logger.info("Topic Count: "+topicCount);

      for(int topicIndex = 0; topicIndex < topicCount; ++topicIndex){
         String topicSummary = result.topicSummary(topicIndex);
         List<TupleTwo<String, Integer>> topKeyWords = result.topKeyWords(topicIndex, 10);
         List<TupleTwo<Doc, Double>> topStrings = result.topDocuments(topicIndex, 5);

         logger.info("Topic #{}: {}", topicIndex+1, topicSummary);

         for(TupleTwo<String, Integer> entry : topKeyWords){
            logger.info("Keyword: {}({})", entry._1(), entry._2());
         }

         for(TupleTwo<Doc, Double> entry : topStrings){
            double ranking = entry._2();
            int docIndex = entry._1().getDocIndex();
            String docContent = entry._1().getContent();
            logger.info("Doc ({}, {}): {}", docIndex, ranking, docContent);
         }
      }

      List<Doc> list = result.documents();
      for(int i=0; i < 3; i++){
         Doc doc = list.get(i);
         logger.info("Doc: {}", doc.getContent());
         List<TupleTwo<Integer, Double>> topTopics = doc.topTopics(3);

         logger.info("Top Topics: {} (score: {}), {} (score: {}), {} (score: {})",
                 topTopics.get(0)._1(), topTopics.get(0)._2(),
                 topTopics.get(1)._1(), topTopics.get(1)._2(),
                 topTopics.get(2)._1(), topTopics.get(2)._2());
      }
   }
}
