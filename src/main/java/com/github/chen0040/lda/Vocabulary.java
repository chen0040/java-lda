package com.github.chen0040.lda;

import java.util.List;


/**
 * Created by xschen on 14/8/15.
 */
public interface Vocabulary {
    String get(int index);
    int getLength();
    void add(String word);
    boolean contains(String word);
    void setWords(List<String> words);

    int indexOf(String word);
    Vocabulary makeCopy();
}
