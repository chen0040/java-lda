package com.github.chen0040.lda;

/**
 * Created by xschen on 11/3/15.
 */
public class Token {
    public int wordIndex;
    public int topicIndex;

    public Token(int wordIndex, int topicIndex){
        this.wordIndex = wordIndex;
        this.topicIndex = topicIndex;
    }

    public Token(){

    }
}
