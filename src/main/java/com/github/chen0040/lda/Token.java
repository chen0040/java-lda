package com.github.chen0040.lda;


import lombok.Getter;
import lombok.Setter;


/**
 * Created by xschen on 11/3/15.
 */
@Getter
@Setter
public class Token {
    private int wordIndex;
    private int topicIndex;

    public Token(int wordIndex, int topicIndex){
        this.wordIndex = wordIndex;
        this.topicIndex = topicIndex;
    }

    public Token(){

    }
}
