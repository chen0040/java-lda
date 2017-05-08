package com.github.chen0040.lda;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by xschen on 11/3/15.
 */
public class Doc {
    public int docIndex;
    public List<Token> tokens = new ArrayList<>();
    public int[] topicCounts;
    public Document content;
    public long timestamp;

    public Doc(int topicCount){
        topicCounts = new int[topicCount];
    }

    public void addToken(int wordIndex, int topicIndex){
        topicCounts[topicIndex]++;
        tokens.add(new Token(wordIndex, topicIndex));
    }

}
