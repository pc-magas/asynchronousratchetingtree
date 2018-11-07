package com.facebook.research.asynchronousratchetingtree;

import com.facebook.asynchronousrachetingtree.executor.AbstractTestExecutor;
import com.facebook.asynchronousrachetingtree.executor.ExecutionParams;
import com.facebook.asynchronousrachetingtree.executor.TestResultItem;
import com.facebook.research.asynchronousratchetingtree.crypto.DHPubKey;

public class CommonTests extends AbstractTestExecutor {

    public static final  String[] CSV_KEYS={
    CommonTests.TEST_NAME,
    CommonTests.INITIATOR_SETUP_TIME,
    CommonTests.INITIATOR_SETUP_BYTES,
    CommonTests.SENDING_TIME,
    CommonTests.RECEIVING_TIME,
    CommonTests.BYTES_SENT,
    CommonTests.BYTES_RECEIVED,
    CommonTests.OTHER_SETUP_TIME,
    CommonTests.OTHER_SETUP_RECEIVED_BYTES,
    CommonTests.OTHER_SETUP_SENT_BYTES
    };

    public CommonTests(GroupMessagingState[] states, GroupMessagingSetupPhase<GroupMessagingState> setupPhase, GroupMessagingTestImplementation<GroupMessagingState> implementation) {
        super(states, setupPhase, implementation);
    }

    @Override
    protected void postSetupExecution(TestResultItem result, ExecutionParams params, Integer[] activeUsers, DHPubKey[] identities, KeyServer keyServer)
    {

    }
}
