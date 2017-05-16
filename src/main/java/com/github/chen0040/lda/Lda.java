package com.github.chen0040.lda;

import com.github.chen0040.data.text.BasicTokenizer;
import com.github.chen0040.data.text.LowerCase;
import com.github.chen0040.data.text.PorterStemmer;
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
    private boolean stemmerEnabled = false;


    public LdaModel model;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private StopWordRemoval stopWordFilter;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private LowerCase lowerCaseFilter;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private PorterStemmer stemmer;

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
        stemmer = new PorterStemmer();
    }

    public Lda(){
       
        stopWordFilter = new StopWordRemoval();
        lowerCaseFilter = new LowerCase();
        stemmer = new PorterStemmer();
    }

    private List<TupleTwo<Integer, TupleTwo<List<String>, String>>> map1(List<String> batch1){

        int m = batch1.size();

        List<TupleTwo<Integer, TupleTwo<List<String>, String>>> result = new ArrayList<>();
        for(int i=0; i < m; ++i){
            String doc = batch1.get(i);
            List<String> words = BasicTokenizer.doTokenize(doc);

            words = trim(words);

            words = lowerCaseFilter.filter(words);
            words = stopWordFilter.filter(words);

            if(stemmerEnabled) {
                words = stemmer.filter(words);
            }

            if(!words.isEmpty()){
                TupleTwo<Integer, TupleTwo<List<String>, String>> tuple = new TupleTwo<>(i, new TupleTwo<>(words, doc));
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


    public LdaResult fit(List<String> docs) {
        stopWordFilter.setRemoveNumbers(removeNumber);
        stopWordFilter.setRemoveIPAddress(removeIPAddress);

        notifyProgressChanged("Data pre-processing ...");

        List<TupleTwo<Integer, TupleTwo<List<String>, String>>> batch = map1(docs);

        notifyProgressChanged("Model building ...");

        buildModel(batch);



        int wordCount = model.wordCount();

        double[] topicWeights = new double[topicCount];


        int size = batch.size();

        notifyProgressChanged("Matrix initialization ...");

        List<Doc> documents = new ArrayList<>();
        for(int docIndex = 0; docIndex < size; ++docIndex){
            TupleTwo<List<String>, String> doc_tt = batch.get(docIndex)._2();
            List<String> doc = doc_tt._1();
            String text = doc_tt._2();

            Doc document = new Doc(topicCount);
            document.setDocIndex(docIndex);
            document.setContent(text);


            for(int i=0; i < doc.size(); ++i){

                String word = doc.get(i);

                int wordIndex = model.getVocabulary().indexOf(word);
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
                //int[] docTopicCounts = currentDoc.topicCounts;

                for (int position = 0; position < currentDoc.getTokens().size(); position++) {
                    Token token = currentDoc.getTokens().get(position);

                    model.tokensPerTopic[token.getTopicIndex()]--;

                    int[] currentWordTopicCounts = model.wordTopicCounts[token.getWordIndex()];

                    currentWordTopicCounts[ token.getTopicIndex() ]--;
                    currentDoc.decTopicCount(token.getTopicIndex());

                    for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
                        if (currentWordTopicCounts[topicIndex] > 0) {
                            topicWeights[topicIndex] =
                                    (documentTopicSmoothing + currentDoc.topicCounts(topicIndex)) *
                                            (topicWordSmoothing + currentWordTopicCounts[ topicIndex ]) /
                                            (wordCount * topicWordSmoothing + model.tokensPerTopic[topicIndex]);
                        }
                        else {
                            topicWeights[topicIndex] =
                                    (documentTopicSmoothing + currentDoc.topicCounts(topicIndex)) * topicWordSmoothing /
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
                    currentDoc.incTopicCount(token.getTopicIndex());
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

    private List<TupleTwo<String, Integer>> map2(List<TupleTwo<Integer, TupleTwo<List<String>, String>>> batch){
        Map<String, Integer> wordCounts = new HashMap<>();

        String word;
        for(int i=0; i < batch.size(); ++i){
            TupleTwo<Integer, TupleTwo<List<String>, String>> t = batch.get(i);
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

    private void buildModel(List<TupleTwo<Integer, TupleTwo<List<String>, String>>> batch){
        model = new LdaModel();

        model.setTopicCount(topicCount);
        model.setDocumentTopicSmoothing(documentTopicSmoothing);
        model.setTopicWordSmoothing(topicWordSmoothing);
        model.setCorrelationMinTokens(correlationMinTokens);
        model.setCorrelationMinProportion(correlationMinProportion);
        model.setDocSortSmoothing(docSortSmoothing);
        model.setRetainRawData(retainRawData);
        model.setMaxVocabularySize(maxVocabularySize);

        List<TupleTwo<String, Integer>> batch3 = map2(batch);

        List<String> candidates = new ArrayList<>();
        long count = batch3.size();

        if(model.getMaxVocabularySize() < count) {
            //sort descendingly
            batch3.sort((t1, t2)->{
                int f1 = t1._2();
                int f2 = t2._2();

                if (f1 > f2) return -1;
                else if (f1 == f2) return 0;
                else return 1;
            });
            for(int i=0; i < model.getMaxVocabularySize(); ++i){
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
