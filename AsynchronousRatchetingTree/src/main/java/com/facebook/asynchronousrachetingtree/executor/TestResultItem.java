package com.facebook.asynchronousrachetingtree.executor;

import java.util.HashMap;

public class TestResultItem {

    private HashMap<String,Long> testResult=new HashMap<String,Long>();

    public void addResult(String item, long value) {
        this.testResult.put(item,new Long(value));
    }

    public void addResult(String item, int value) {
        this.addResult(item, (long) value);
    }

    public String getResultAsCSVRow(String[] keys) {
        String endResult="";
        for(String key:keys)
        {
            endResult+=this.testResult.get(key)+",";
        }

        return endResult.substring(0,endResult.length()-1);
    }
}
