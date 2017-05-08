package com.github.chen0040.lda;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by xschen on 9/9/15.
 */
public interface Document {
    ArrayList<String> getRawContents();
    void setRawContents(ArrayList<String> contents);

    HashMap<String, Integer> getWordCounts();
    void setWordCounts(HashMap<String, Integer> counts);

    HashMap<String, String> getAttributes();
    void setAttributes(HashMap<String, String> attributes);
    void setAttribute(String name, String value);
    String getAttribute(String name);

    Document makeCopy();
}
