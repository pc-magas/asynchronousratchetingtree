package com.facebook.asynchronousrachetingtree.executor;

import java.util.ArrayList;
import java.util.Iterator;

public class TestResults implements Iterable<TestResultItem> {

    private ArrayList<TestResultItem> testResults = null;

    public TestResults()
    {
        this.testResults= new ArrayList<TestResultItem>();
    }


    public void addTestResult(TestResultItem resultItem)
    {
        this.testResults.add(resultItem);
    }

    @Override
    public Iterator<TestResultItem> iterator() { return testResults.iterator(); }

}
