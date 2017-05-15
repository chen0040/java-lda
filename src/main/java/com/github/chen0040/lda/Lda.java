package com.github.chen0040.lda;

import com.github.chen0040.data.text.BasicTokenizer;
import com.github.chen0040.data.text.LowerCase;
import com.github.chen0040.data.text.StopWordRemoval;
import com.github.chen0040.data.utils.TupleTwo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;


/**
 * Created by xschen on 11/3/15.
 */
@Getter
@Setter
public class Lda {

    private static final Logger logger = LoggerFactory.getLogger(Lda.class);

    private double documentTopicSmoothing = 0.1;
    private double topicWordSmoothing = 0.01;

    private double correlationMinTokens = 2;
    private double correlationMinProportion = 0.05;

    private int topicCount = 20;
    private double docSortSmoothing = 10.0;

    private boolean retainRawData = false;
    private int maxVocabularySize = 1000000;

    private int maxSweepCount = 50;
    private int maxTopicSummaryLength = 20;

    private boolean removeIPAddress = true;
    private boolean removeNumber = true;


    public LdaModel model;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private StopWordRemoval stopWordFilter;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private LowerCase lowerCaseFilter;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private Random random = new Random();

    public void addStopWords(List<String> stopWords){
        this.stopWordFilter.join(stopWords);
    }

    public Consumer<String> progressListener;

    public void setProgressListener(Consumer<String> listener){
        progressListener = listener;
    }

    private void notifyProgressChanged(String message){
        if(progressListener != null) {
            progressListener.accept(message);
        } else {
            logger.info(message);
        }
    }

    public Lda makeCopy() {
        Lda clone = new Lda();
        clone.copy(this);
        return clone;
    }

    public void copy(Lda that){
        this.model = that.model.makeCopy();
        stopWordFilter = that.stopWordFilter.makeCopy();
        lowerCaseFilter = that.lowerCaseFilter.makeCopy();
    }

    public Lda(){
       
        stopWordFilter = new StopWordRemoval();
        lowerCaseFilter = new LowerCase();
    }

    private List<TupleTwo<Integer, TupleTwo<List<String>, Long>>> map1(List<Document> batch1){

        int m = batch1.size();

        List<TupleTwo<Integer, TupleTwo<List<String>, Long>>> result = new ArrayList<>();
        for(int i=0; i < m; ++i){
            Document doc = batch1.get(i);
            List<String> words = BasicTokenizer.doTokenize(doc.getText());

            words = trim(words);

            words = lowerCaseFilter.filter(words);
            words = stopWordFilter.filter(words);

            if(!words.isEmpty()){
                TupleTwo<Integer, TupleTwo<List<String>, Long>> tuple = new TupleTwo<>(i, new TupleTwo<>(words, (long)i));
                result.add(tuple);
            }
        }
        return result;
    }

    private List<String> trim(List<String> words){
        Pattern ge = Pattern.compile(",");
        List<String> processed = new ArrayList<>();
        for(int i=0; i < words.size(); ++i){
            String word = words.get(i);
            if(word.contains(",")){
                List<String> words2 = Arrays .asList(ge.split(word));
                for(int j=0; j < words2.size(); ++j) {
                    word = words2.get(j);
                    if(word.length() > 30){
                        word = word.substring(0, 30);
                    }
                    processed.add(word);
                }
            } else {
                if(word.length() > 30){
                    word = word.substring(0, 30);
                }
                processed.add(word);
            }
        }

        return processed;
    }


    public LdaResult fit(List<Document> batch0) {
        stopWordFilter.setRemoveNumbers(removeNumber);
        stopWordFilter.setRemoveIPAddress(removeIPAddress);

        notifyProgressChanged("Data preprocessing ...");

        List<TupleTwo<Integer, TupleTwo<List<String>, Long>>> batch = map1(batch0);

        notifyProgressChanged("Model building ...");

        buildModel(batch);



        int wordCount = model.wordCount();

        double[] topicWeights = new double[topicCount];


        int size = batch.size();

        notifyProgressChanged("Matrix initialization ...");

        List<Doc> documents = new ArrayList<>();
        for(int docIndex = 0; docIndex < size; ++docIndex){
            TupleTwo<List<String>, Long> doc_tt = batch.get(docIndex)._2();
            List<String> doc = doc_tt._1();
            long timestamp = doc_tt._2();

            Doc document = new Doc(topicCount);
            document.docIndex = docIndex;
            document.timestamp = timestamp;


            for(int i=0; i < doc.size(); ++i){

                String word = doc.get(i);

                int wordIndex = model.vocabulary.indexOf(word);
                int topicIndex = random.nextInt(topicCount);

                if(wordIndex==-1) continue;

                model.tokensPerTopic[topicIndex]++;

                model.wordTopicCounts[wordIndex][topicIndex]++;

                model.vocabularyCounts[wordIndex]++;
                model.topicCounts[topicIndex]++;

                document.addToken(wordIndex, topicIndex);
            }
            documents.add(document);
        }

        notifyProgressChanged("Start iteration ...");

        for(int sweepIndex = 0; sweepIndex < maxSweepCount; ++sweepIndex){

            for(int docIndex=0; docIndex < size; ++docIndex){
                Doc currentDoc = documents.get(docIndex);
                int[] docTopicCounts = currentDoc.topicCounts;

                for (int position = 0; position < currentDoc.tokens.size(); position++) {
                    Token token = currentDoc.tokens.get(position);

                    model.tokensPerTopic[token.getTopicIndex()]--;

                    int[] currentWordTopicCounts = model.wordTopicCounts[token.getWordIndex()];

                    currentWordTopicCounts[ token.getTopicIndex() ]--;
                    docTopicCounts[ token.getTopicIndex() ]--;

                    for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
                        if (currentWordTopicCounts[topicIndex] > 0) {
                            topicWeights[topicIndex] =
                                    (documentTopicSmoothing + docTopicCounts[topicIndex]) *
                                            (topicWordSmoothing + currentWordTopicCounts[ topicIndex ]) /
                                            (wordCount * topicWordSmoothing + model.tokensPerTopic[topicIndex]);
                        }
                        else {
                            topicWeights[topicIndex] =
                                    (documentTopicSmoothing + docTopicCounts[topicIndex]) * topicWordSmoothing /
                                            (wordCount * topicWordSmoothing + model.tokensPerTopic[topicIndex]);
                        }
                    }

                    token.setTopicIndex(sampleDiscrete(topicWeights));

                    model.tokensPerTopic[token.getTopicIndex()]++;
                    if (currentWordTopicCounts[ token.getTopicIndex() ] <= 0) {
                        currentWordTopicCounts[ token.getTopicIndex() ] = 1;
                    }
                    else {
                        currentWordTopicCounts[ token.getTopicIndex() ] += 1;
                    }
                    docTopicCounts[ token.getTopicIndex() ]++;
                }
            }

            notifyProgressChanged("Iterating #"+sweepIndex + " / " + maxSweepCount);
        }


        notifyProgressChanged("Finalizing ...");

        model.sortTopicWords();
        model.createTopicSummary(maxTopicSummaryLength);

        LdaResult result = new LdaResult(model, documents);

        notifyProgressChanged("Completed!");

        return result;
    }

    public int sampleDiscrete (double[] weights) {
        double sample = sum(weights) * Math.random();
        int i = 0;
        sample -= weights[i];
        while (sample > 0.0) {
            i++;
            sample -= weights[i];
        }
        return i;
    }

    private double sum(double[] weights){
        double sum = 0;
        for(int i=0; i < weights.length; ++i){
            sum += weights[i];
        }
        return sum;
    }

    private List<TupleTwo<String, Integer>> map2(List<TupleTwo<Integer, TupleTwo<List<String>, Long>>> batch){
        Map<String, Integer> wordCounts = new HashMap<>();

        String word;
        for(int i=0; i < batch.size(); ++i){
            TupleTwo<Integer, TupleTwo<List<String>, Long>> t = batch.get(i);
            List<String> t2 = t._2()._1();
            for(int j=0; j < t2.size(); ++j){
                word = t2.get(j);

                if(wordCounts.containsKey(word)){
                    wordCounts.put(word, wordCounts.get(word)+1);
                } else {
                    wordCounts.put(word, 1);
                }
            }
        }

        List<TupleTwo<String, Integer>> result = new ArrayList<>();
        for(Map.Entry<String, Integer> entry : wordCounts.entrySet()){
            result.add(new TupleTwo<>(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private void buildModel(List<TupleTwo<Integer, TupleTwo<List<String>, Long>>> batch){
        model = new LdaModel();

        model.topicCount = topicCount;
        model.documentTopicSmoothing = documentTopicSmoothing;
        model.topicWordSmoothing = topicWordSmoothing;
        model.correlationMinTokens = correlationMinTokens;
        model.correlationMinProportion = correlationMinProportion;
        model.docSortSmoothing = docSortSmoothing;
        model.retainRawData = retainRawData;
        model.maxVocabularySize = maxVocabularySize;

        List<TupleTwo<String, Integer>> batch3 = map2(batch);

        List<String> candidates = new ArrayList<>();
        long count = batch3.size();

        if(model.maxVocabularySize < count) {
            //sort descendingly
            batch3.sort((t1, t2)->{
                int f1 = t1._2();
                int f2 = t2._2();

                if (f1 > f2) return -1;
                else if (f1 == f2) return 0;
                else return 1;
            });
            for(int i=0; i < model.maxVocabularySize; ++i){
                candidates.add(batch3.get(i)._1());
            }
        } else {
            for(int i=0; i < count; ++i){
                candidates.add(batch3.get(i)._1());
            }
        }

        logger.info("Vocabulary Size: {}", candidates.size());

        model.initialize(candidates);
    }
}
