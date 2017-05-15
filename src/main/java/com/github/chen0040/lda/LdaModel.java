package com.github.chen0040.lda;


import com.github.chen0040.data.text.BasicVocabulary;
import com.github.chen0040.data.text.Vocabulary;
import com.github.chen0040.data.text.VocabularyTableCell;
import com.github.chen0040.data.utils.TupleTwo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;


/**
 * Created by xschen on 11/3/15.
 */
@Getter
@Setter
public class LdaModel {

    private static final Logger logger = LoggerFactory.getLogger(LdaModel.class);

    private Vocabulary vocabulary;
    private int topicCount;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    public int[][] wordTopicCounts;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    public List<Frequency>[] topicWordCounts;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    public  int[] vocabularyCounts;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    public int[] tokensPerTopic;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    public int[] topicCounts;

    private double documentTopicSmoothing;
    private double topicWordSmoothing;

    // Constants for calculating topic correlation. A doc with 5% or more tokens in a topic is "about" that topic.
    private double correlationMinTokens;
    private double correlationMinProportion;

    // Use a more aggressive smoothing parameter to sort
    // documents by topic. This has the effect of preferring
    // longer documents.
    private double docSortSmoothing;

    private boolean retainRawData;
    private int maxVocabularySize;


    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String[] topicSummaries;

    public LdaModel makeCopy(){
        LdaModel clone = new LdaModel();
        clone.copy(this);
        return clone;
    }

    private String createTopicSummary(int selectedTopicIndex, int length){
        StringBuilder sb = new StringBuilder();
        List<Frequency> topicWordCountsByTopic = topicWordCounts[selectedTopicIndex];
        int wordCount = Math.min(wordCount(), length);

        for(int i = 0; i < wordCount; i++ ){
            if(i != 0){
                sb.append(" ");
            }
            sb.append(vocabulary.get(topicWordCountsByTopic.get(i).getWordIndex()));
        }
        return sb.toString();
    }

    public List<TupleTwo<String, Integer>> topicWords(int selectedTopicIndex, int length){
        List<TupleTwo<String, Integer>> result = new ArrayList<>();

        List<Frequency> topicWordCountsByTopic = topicWordCounts[selectedTopicIndex];
        int wordCount = Math.min(wordCount(), length);

        for(int wordIndex = 0; wordIndex < wordCount; wordIndex++ ){
            Frequency freq = topicWordCountsByTopic.get(wordIndex);
            String word = vocabulary.get(freq.getWordIndex());
            int value = freq.getCount();
            result.add(new TupleTwo<>(word, value));
        }
        return result;
    }

    private void copy(LdaModel rhs){
        vocabulary = rhs.vocabulary.makeCopy();
        topicCount = rhs.topicCount;
        wordTopicCounts = rhs.wordTopicCounts.clone();
        topicWordCounts = (List<Frequency>[])(new List[rhs.topicWordCounts.length]);

        for(int i=0; i < rhs.topicWordCounts.length; ++i){
            topicWordCounts[i] = new ArrayList<>();
            List<Frequency> rhs_item = rhs.topicWordCounts[i];
            for(int j=0; j < rhs_item.size(); ++i){
                topicWordCounts[i].add(rhs_item.get(j).makeCopy());
            }
        }

        vocabularyCounts = rhs.vocabularyCounts.clone();
        tokensPerTopic = rhs.tokensPerTopic.clone();
        topicCounts = rhs.topicCounts.clone();

        documentTopicSmoothing = rhs.documentTopicSmoothing;
        topicWordSmoothing = rhs.topicWordSmoothing;

        // Constants for calculating topic correlation. A doc with 5% or more tokens in a topic is "about" that topic.
        correlationMinTokens = rhs.correlationMinTokens;
        correlationMinProportion = rhs.correlationMinProportion;

        // Use a more aggressive smoothing parameter to sort
        // documents by topic. This has the effect of preferring
        // longer documents.
        docSortSmoothing = rhs.docSortSmoothing;

        retainRawData = rhs.retainRawData;
        maxVocabularySize = rhs.maxVocabularySize;

        topicSummaries = rhs.topicSummaries.clone();
    }

    public LdaModel(){
        vocabulary = new BasicVocabulary();
    }

    public double sumDocSortSmoothing(){
        return docSortSmoothing * topicCount;
    }

    public int wordCount(){
        return vocabulary.getLength();
    }

    public void sortTopicWords() {
        int wordCount = wordCount();

        for (int wordIndex = 0; wordIndex < wordCount; ++wordIndex) {
            for (int topicIndex = 0; topicIndex < topicCount; ++topicIndex) {
                topicWordCounts[topicIndex].get(wordIndex).setCount(wordTopicCounts[wordIndex][topicIndex]);
            }
        }

        for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
            topicWordCounts[topicIndex].sort((f1, f2)->{
                if(f1.getCount() > f2.getCount()) return -1;
                else if(f1.getCount() == f2.getCount()) return 0;
                else return 1;
            });
        }
    }

    public double entropy(int[] counts) {
        double sum = sum(counts);
        if(sum == 0) return 0;
        double entropy = Math.log(sum) - (1.0 / sum) * sum(counts, x ->  x == 0 ? x : x * Math.log(x));

        if(Double.isNaN(entropy)){
            logger.warn("NaN entropy");
        }
        return entropy;
    }

    private int sum(int[] counts){
        int sum = 0;
        for(int i=0; i < counts.length; ++i){
            sum += counts[i];
        }
        return sum;
    }

    private double sum(int[] counts, Function<Integer, Double> f){
        double sum = 0;
        for(int i=0; i < counts.length; ++i){
            sum += f.apply(counts[i]);
        }
        return sum;
    }


    public List<VocabularyTableCell> vocabularyTable(int maxSize){
        List<VocabularyTableCell> table = new ArrayList<>();

        int wordCount = wordCount();
        for(int wordIndex = 0; wordIndex < wordCount; ++wordIndex){
            VocabularyTableCell cell = new VocabularyTableCell();
            cell.setWord(vocabulary.get(wordIndex));
            cell.setWordIndex(wordIndex);
            cell.setCount(vocabularyCounts[wordIndex]);

            if(wordIndex >= maxSize) {
                int cell2Kick = -1;

                int minCount = Integer.MAX_VALUE;
                for(int i=0; i < table.size(); ++i){
                    VocabularyTableCell cell2Check = table.get(i);
                    if(cell2Check.getCount() < minCount){
                        minCount = cell2Check.getCount();
                        cell2Kick = i;
                    }
                }

                if(cell2Kick != -1){
                    table.set(cell2Kick, cell);
                }

            } else {
                table.add(cell);
            }
        }

        for(int i=0; i < table.size(); ++i) {
            VocabularyTableCell cell = table.get(i);
            cell.setTopicSpecificity(1.0 - entropy(wordTopicCounts[cell.getWordIndex()]) / Math.log(topicCount));
        }
        return table;
    }

    public void initialize(List<String> words){
        int wordCount = words.size();

        vocabulary.setWords(words);


        wordTopicCounts = new int[wordCount][];
        for(int wordIndex = 0; wordIndex < wordCount; ++wordIndex){
            wordTopicCounts[wordIndex] = new int[topicCount];
        }

        topicWordCounts = (List<Frequency>[])new ArrayList[topicCount];
        for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
            topicWordCounts[topicIndex] = new ArrayList<>();
            for(int wordIndex = 0; wordIndex < wordCount; ++wordIndex){
                topicWordCounts[topicIndex].add(new Frequency(wordIndex, 0));
            }
        }

        vocabularyCounts = new int[wordCount];

        tokensPerTopic = new int[topicCount];

        for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
            tokensPerTopic[topicIndex] = 0;
        }

        topicCounts = new int[topicCount];
        topicSummaries = new String[topicCount];
        for(int topicIndex = 0; topicIndex < topicCount; ++topicIndex){
            topicSummaries[topicIndex] = "Topic: "+topicIndex;
        }
    }

    public void createTopicSummary(int length){
        for(int topicIndex = 0; topicIndex < topicCount; ++topicIndex){
            topicSummaries[topicIndex] = createTopicSummary(topicIndex, length);
        }
    }


    public String getTopicSummary(int topicIndex) {
        return topicSummaries[topicIndex];
    }
}
