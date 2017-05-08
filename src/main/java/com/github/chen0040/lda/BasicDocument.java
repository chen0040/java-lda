package com.github.chen0040.lda;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by xschen on 9/9/15.
 */
public class BasicDocument implements Document {

    private HashMap<String, Integer> wordCounts;
    private ArrayList<String> rawContents;
    private HashMap<String, String> attributes;
    private String label;
    private long timestamp;

    public String getLabel(){return label; }

    public void setLabel(String label ){
        this.label = label;
    }

    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(HashMap<String, String> attributes) {
        this.attributes = attributes;
    }

    public void setAttribute(String name, String value){
        attributes.put(name, value);
    }

    public String getAttribute(String name){
        return attributes.get(name);
    }

    public long getTimestamp(){
        return timestamp;
    }

    public void setTimestamp(long timestamp){
        this.timestamp = timestamp;
    }

    public BasicDocument(){

        wordCounts = new HashMap<String, Integer>();
        rawContents = new ArrayList<>();
        attributes = new HashMap<String, String>();
    }

    @Override
    public Document makeCopy(){
        BasicDocument clone = new BasicDocument();
        for(String key : this.wordCounts.keySet()){
            clone.wordCounts.put(key, wordCounts.get(key));
        }

        for(String attrname : attributes.keySet()){
            clone.attributes.put(attrname, attributes.get(attrname));
        }

        for(int i=0; i < rawContents.size(); ++i){
            clone.rawContents.add(rawContents.get(i));
        }

        clone.timestamp = this.timestamp;

        return clone;
    }


    public ArrayList<String> getRawContents() {
        return rawContents;
    }

    public void setRawContents(ArrayList<String> contents) {
        rawContents = contents;
    }

    public HashMap<String, Integer> getWordCounts() {
        return wordCounts;
    }

    public void setWordCounts(HashMap<String, Integer> counts) {
        this.wordCounts = counts;
    }

    public void initialize(String text) {
        rawContents.add(text);
    }
}
