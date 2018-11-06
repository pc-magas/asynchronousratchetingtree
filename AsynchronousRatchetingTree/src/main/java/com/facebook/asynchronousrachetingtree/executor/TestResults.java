package com.facebook.asynchronousrachetingtree.executor;

import java.util.ArrayList;

public class TestResults {

    private ArrayList<TestResultItem> testResults = null;

    public TestResults()
    {
        this.testResults= new ArrayList<TestResultItem>();
    }


    public void addTestResult(TestResultItem resultItem)
    {
        this.testResults.add(resultItem);
    }
}
