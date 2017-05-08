package com.github.chen0040.lda.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


/**
 * Created by memeanalytics on 12/8/15.
 */
public class FileUtils {
    public static InputStream getResource(String fileName) throws IOException {

        StringBuilder result = new StringBuilder("");

        //Get file from resources folder
        ClassLoader classLoader = FileUtils.class.getClassLoader();
        return classLoader.getResource(fileName).openStream();

    }
}
