package com.github.chen0040.lda;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


/**
 * Created by xschen on 11/4/15.
 */
@Getter
@Setter
public class Frequency implements Serializable {
    private int wordIndex;
    private int count;

    public Frequency(int wordIndex, int count){
        this.wordIndex = wordIndex;
        this.count = count;
    }

    public Frequency(){
        this.wordIndex = -1;
        this.count = 0;
    }

    public Frequency makeCopy(){
        Frequency clone = new Frequency(wordIndex, count);
        return clone;
    }
}
