package com.github.chen0040.lda;


import com.github.chen0040.data.utils.TupleTwo;
import com.github.chen0040.lda.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by xschen on 9/5/2017.
 */
public class LdaUnitTest {
   private static Lda method;
   private static final Logger logger = LoggerFactory.getLogger(LdaUnitTest.class);

   @BeforeClass
   public void setup() throws IOException {

      method = new Lda();

      List<String> stopWords = new ArrayList<>();

      InputStream inputStream = FileUtils.getResource("stoplist.txt");
      try {
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
         String line;
         while((line=reader.readLine())!=null){
            String word = line.trim();
            if(!word.equals("")){
               stopWords.add(word);
            }
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      method.addStopWords(stopWords);
   }

   private List<Document> getDocs() throws IOException {

      InputStream inputStream = FileUtils.getResource("documents.txt");

      List<Document> docs = new ArrayList<>();

      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      reader.lines().forEach(line->{
         line = line.trim();
         if(line.equals("")) return;

         String[] fields = line.split("\t");
         String text = fields[0];
         if (fields.length == 3) {
            text = fields[2];
         }

         Document document = new BasicDocument(text);
         docs.add(document);
      });
      reader.close();

      return docs;
   }

   @Test
   public void testLDA() throws IOException {

      List<Document> docs = getDocs();

      method.setTopicCount(20);
      method.setMaxVocabularySize(100000);
      LdaResult result = method.fit(docs);


      int topicCount = result.topicCount();

      logger.info("Topic Count: "+topicCount);

      for(int topicIndex = 0; topicIndex < topicCount; ++topicIndex){
         String topicSummary = result.topicSummary(topicIndex);
         List<TupleTwo<String, Integer>> topKeyWords = result.topKeyWords(topicIndex, 10);
         List<TupleTwo<Doc, Double>> topDocuments = result.topDocuments(topicIndex, 10);

         logger.info("Topic #{}: {}", topicIndex+1, topicSummary);

         for(TupleTwo<String, Integer> entry : topKeyWords){
            logger.info("Keyword: {}({})", entry._1(), entry._2());
         }
      }
   }
}
