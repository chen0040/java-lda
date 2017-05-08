package com.github.chen0040.lda;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * Created by xschen on 11/3/15.
 */
public class LdaModel {
    public Vocabulary vocabulary;
    public int topicCount;
    public int[][] wordTopicCounts;
    public List<Frequency>[] topicWordCounts;
    public  int[] vocabularyCounts;
    public int[] tokensPerTopic;
    public int[] topicCounts;

    public double documentTopicSmoothing;
    public double topicWordSmoothing;

    // Constants for calculating topic correlation. A doc with 5% or more tokens in a topic is "about" that topic.
    public double correlationMinTokens;
    public double correlationMinProportion;

    // Use a more aggressive smoothing parameter to sort
    // documents by topic. This has the effect of preferring
    // longer documents.
    public double docSortSmoothing;

    public boolean retainRawData;
    public int maxVocabularySize;


    public String[] topicSummaries;

    public LdaModel makeCopy(){
        LdaModel clone = new LdaModel();
        clone.copy(this);
        return clone;
    }

    private String topicSummary(int selectedTopicIndex, int length){
        StringBuilder sb = new StringBuilder();
        List<Frequency> topicWordCountsByTopic = topicWordCounts[selectedTopicIndex];
        int wordCount = Math.min(wordCount(), length);

        for(int wordIndex = 0; wordIndex < wordCount; wordIndex++ ){
            if(wordIndex != 0){
                sb.append(" ");
            }
            sb.append(vocabulary.get(topicWordCountsByTopic.get(wordIndex).wordIndex));
        }
        return sb.toString();
    }

    public String describeTopic(int selectedTopicIndex, int length){
        StringBuilder sb = new StringBuilder();
        List<Frequency> topicWordCountsByTopic = topicWordCounts[selectedTopicIndex];
        int wordCount = Math.min(wordCount(), length);
        for(int wordIndex = 0; wordIndex < wordCount; wordIndex++ ){
            if(wordIndex != 0){
                sb.append(" ");
            }
            Frequency token = topicWordCountsByTopic.get(wordIndex);
            sb.append(vocabulary.get(token.wordIndex)+"("+token.count+")");
        }
        return sb.toString();
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
                topicWordCounts[i].add((Frequency)rhs_item.get(j).clone());
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
                topicWordCounts[topicIndex].get(wordIndex).count = wordTopicCounts[wordIndex][topicIndex];
            }
        }

        for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
            topicWordCounts[topicIndex].sort((f1, f2)->{
                if(f1.count > f2.count) return -1;
                else if(f1.count == f2.count) return 0;
                else return 1;
            });
        }
    }

    public double entropy(int[] counts) {
        double sum = sum(counts);
        if(sum == 0) return 0;
        double entropy = Math.log(sum) - (1.0 / sum) * sum(counts, x ->  x == 0 ? x : x * Math.log(x));

        if(Double.isNaN(entropy)){
            System.out.println("entropy spotted");
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
            cell.word = vocabulary.get(wordIndex);
            cell.wordIndex = wordIndex;
            cell.count = vocabularyCounts[wordIndex];

            if(wordIndex >= maxSize) {
                int cell2Kick = -1;

                int minCount = Integer.MAX_VALUE;
                for(int i=0; i < table.size(); ++i){
                    VocabularyTableCell cell2Check = table.get(i);
                    if(cell2Check.count < minCount){
                        minCount = cell2Check.count;
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
            cell.topicSpecificity = 1.0 - entropy(wordTopicCounts[cell.wordIndex]) / Math.log(topicCount);
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

        topicWordCounts = new ArrayList[topicCount];
        for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
            topicWordCounts[topicIndex] = new ArrayList();
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
            topicSummaries[topicIndex] = topicSummary(topicIndex, length);
        }
    }

}
