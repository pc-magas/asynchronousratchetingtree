/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
*/

package com.facebook.research.asynchronousratchetingtree;

import com.facebook.asynchronousrachetingtree.executor.AbstractTestExecutorParams;
import com.facebook.asynchronousrachetingtree.executor.TestResultItem;
import com.facebook.asynchronousrachetingtree.executor.TestResults;
import com.facebook.research.asynchronousratchetingtree.art.ARTSetupPhase;
import com.facebook.research.asynchronousratchetingtree.art.ARTState;
import com.facebook.research.asynchronousratchetingtree.art.ARTTestImplementation;
import com.facebook.research.asynchronousratchetingtree.dhratchet.DHRatchet;
import com.facebook.research.asynchronousratchetingtree.dhratchet.DHRatchetSetupPhase;
import com.facebook.research.asynchronousratchetingtree.dhratchet.DHRatchetState;

import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public class Main {

  private static boolean debug = true;

  public static void main(String[] args) {
    checkForUnlimitedStrengthCrypto();

    // Run a test run first to warm up the JIT
    artTestRun(8, 8);
    dhTestRun(8, 8);

    TestResults artResults = new TestResults();
    TestResults dhResults = new TestResults();

    int test_size_limit = 1000;
    double multiplier = 1.5;

    // First run ART tests. Warm up the JIT with a few smallish values, and then start recording results.
    for (int i = 2; i < 20; i += 2) {
      artTestRun(i, i);
    }

    int lastNumber = 1;

    for (double i = 2.0; i <= test_size_limit; i *= multiplier) {
      int n = (int) i;
      if (n == lastNumber) {
        continue;
      }
      lastNumber = n;
      System.gc();
      artResults.addTestResult(artTestRun(n, n));
    }

    // Now run DH tests. Again, warm up the JIT with a few smallish values, and then start recording results.
    for (int i = 2; i < 20; i += 2) {
      dhTestRun(i, i);
    }
    lastNumber = 1;
    for (double i = 2.0; i <= test_size_limit; i *= multiplier) {
      int n = (int) i;
      if (n == lastNumber) {
        continue;
      }
      System.gc();
      dhResults.addTestResult(dhTestRun(n, n));
    }

    Iterator<TestResultItem> artIterator = artResults.iterator();
    while (artIterator.hasNext()) {
      TestResultItem r = artIterator.next();
      String csvRow= (String) r.getResultAsCSVRow(CommonTests.CSV_KEYS);
      Utils.print(csvRow);
    }

    Iterator<TestResultItem> dhIterator = dhResults.iterator();

    while (dhIterator.hasNext()) {
      TestResultItem r = dhIterator.next();
      String csvRow = (String) r.getResultAsCSVRow(CommonTests.CSV_KEYS);
      Utils.print(csvRow);
    }

  }

  private static TestResultItem artTestRun(int n, int activePeers) {
    ARTState[] states = new ARTState[n];

    for (int i = 0; i < n; i++) {
      states[i] = new ARTState(i, n);
    }

    return testRun(
      n,
      activePeers,
      states,
      new ARTSetupPhase(),
      new ARTTestImplementation()
    );
  }

  private static void checkForUnlimitedStrengthCrypto() {
    boolean hasEnoughCrypto = true;
    try {
      if (Cipher.getMaxAllowedKeyLength("AES") < 256) {
        hasEnoughCrypto = false;
      }
    } catch (NoSuchAlgorithmException e) {
      hasEnoughCrypto = false;
    }
    if (!hasEnoughCrypto) {
      Utils.except("Your JRE does not support unlimited-strength " +
		   "cryptography and thus cannot run ART. See README.md " +
		   "for Java Cryptography Extension (JCE) Unlimited " +
		   "Strength Jurisdiction Policy Files installation " +
		   "instructions.");
    }
  }

  private static TestResultItem dhTestRun(int n, int activePeers) {
    DHRatchetState[] states = new DHRatchetState[n];

    for (int i = 0; i < n; i++) {
      states[i] = new DHRatchetState(i, n);
    }


    return testRun(
      n,
      activePeers,
      states,
      new DHRatchetSetupPhase(),
      new DHRatchet()
    );
  }

  private static <TState extends GroupMessagingState> TestResultItem testRun(
    int n,
    int activeCount,
    TState[] states,
    GroupMessagingSetupPhase<TState> setupPhase,
    GroupMessagingTestImplementation<TState> implementation
  ) {

    CommonTests exec = new CommonTests((GroupMessagingState[]) states, (GroupMessagingSetupPhase<GroupMessagingState>) setupPhase, (GroupMessagingTestImplementation<GroupMessagingState>) implementation);

    return exec.run(new AbstractTestExecutorParams(n,activeCount,debug));
  }


}
