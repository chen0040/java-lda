package com.github.chen0040.lda;

import java.io.Serializable;


/**
 * Created by xschen on 11/4/15.
 */
public class Frequency implements Serializable, Cloneable {
    public int wordIndex;
    public int count;

    public Frequency(int wordIndex, int count){
        this.wordIndex = wordIndex;
        this.count = count;
    }

    public Frequency(){
        this.wordIndex = -1;
        this.count = 0;
    }

    @Override
    public Object clone(){
        Frequency clone = new Frequency(wordIndex, count);
        return clone;
    }
}
