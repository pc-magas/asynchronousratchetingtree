package com.facebook.asynchronousrachetingtree.executor;

import java.util.ArrayList;
import java.util.HashMap;

public class TestResultItem {

    private HashMap<String,Object> testResult=new HashMap<String,Object>();


    public void addResult(String item, long value) {
        this.testResult.put(item,new Long(value));
    }
    public void addResult(String item, String value) {
        this.testResult.put(item, value);
    }
    public void addResult(String item, int value) {
        this.testResult.put(item, new Integer(value));
    }


    public String getResultAsCSVRow(String[] keys) {

        ArrayList<String> endResults=new ArrayList<String>();

        for(String key:keys)
        {
            endResults.add(String.valueOf(this.testResult.get(key)));
        }

        String endResult= String.join(",",endResults);
        return endResult.substring(0,endResult.length()-1);
    }
}
