package com.github.chen0040.lda;

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
}
