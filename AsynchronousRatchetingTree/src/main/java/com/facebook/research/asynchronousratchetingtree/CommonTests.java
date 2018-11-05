package com.facebook.research.asynchronousratchetingtree;

import com.facebook.asynchronousrachetingtree.executor.AbstractTestExecutor;
import com.facebook.asynchronousrachetingtree.executor.ExecutionParams;
import com.facebook.research.asynchronousratchetingtree.crypto.DHPubKey;

public class CommonTests extends AbstractTestExecutor {

    public CommonTests(GroupMessagingState[] states, GroupMessagingSetupPhase<GroupMessagingState> setupPhase, GroupMessagingTestImplementation<GroupMessagingState> implementation) {
        super(states, setupPhase, implementation);
    }

    @Override
    protected void postSetupExecution(TestResult result, ExecutionParams params, Integer[] activeUsers, DHPubKey[] identities, KeyServer keyServer)
    {

    }
}
