package com.github.chen0040.lda;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by xschen on 9/9/15.
 */
public class BasicDocument implements Document {
    private final String text;
    public BasicDocument(String text){
        this.text = text;
    }


    public String getText(){
        return text;
    }
}
