package com.github.chen0040.lda;

import com.github.chen0040.data.utils.TupleTwo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by xschen on 11/3/15.
 */
@Getter
@Setter
public class Doc {
    private int docIndex;
    private List<Token> tokens = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final int[] topicCounts;

    private String content;
    private long timestamp;

    public Doc(int topicCount){
        topicCounts = new int[topicCount];
    }

    public void addToken(int wordIndex, int topicIndex){
        topicCounts[topicIndex]++;
        tokens.add(new Token(wordIndex, topicIndex));
    }


    public void decTopicCount(int topicIndex) {
        topicCounts[topicIndex]--;
    }


    public double topicCounts(int topicIndex) {
        return topicCounts[topicIndex];
    }


    public void incTopicCount(int topicIndex) {
        topicCounts[topicIndex]++;
    }

    public List<TupleTwo<Integer, Double>> topTopics(int limits) {
        double sum = 0;

        for(int i=0; i < topicCounts.length; ++i){
            sum += topicCounts[i];
        }
        List<TupleTwo<Integer, Double>> ranked = new ArrayList<>();
        for(int topicIndex = 0; topicIndex < topicCounts.length; ++topicIndex){
            ranked.add(new TupleTwo<>(topicIndex, topicCounts[topicIndex] / sum));
        }
        ranked.sort((a, b) -> -Double.compare(a._2(), b._2()));

        List<TupleTwo<Integer, Double>> result = new ArrayList<>();
        for(int i=0; i < limits; ++i){
            result.add(ranked.get(i));
        }
        return result;
    }
}
