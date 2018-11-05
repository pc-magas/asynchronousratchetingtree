package com.facebook.asynchronousrachetingtree.executor;

import com.facebook.research.asynchronousratchetingtree.*;
import com.facebook.research.asynchronousratchetingtree.crypto.DHPubKey;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

abstract public class AbstractTestExecutor implements TestExecutor {

    protected GroupMessagingSetupPhase<GroupMessagingState> setupPhase;
    protected GroupMessagingTestImplementation<GroupMessagingState> implementation;
    protected GroupMessagingState[] states=null;
    protected SecureRandom random = null;


    public final static int messagesToSend = 100;
    public final static  int messageLength = 32;


    public AbstractTestExecutor(GroupMessagingState[] states,
                        GroupMessagingSetupPhase<GroupMessagingState> setupPhase,
                        GroupMessagingTestImplementation<GroupMessagingState> implementation
    ){
        this.states = states;
        this.setupPhase = setupPhase;
        this.implementation = implementation;
        this.random = new SecureRandom();

    }

    public void bootstap(){


    }

    /**
     * Bootstrapping the available messages we need in order to run the test.
     *
     * @param messageSenders How many messages
     * @param messages The messages themselves that will be generates
     * @param activeUsers
     */
    private void bootstrapMessages(int[] messageSenders, byte[][] messages, int activeCount, Integer[] activeUsers) {

        for (int i = 0; i < this.messagesToSend; i++) {
            // We're only interested in ratcheting events, so senders should always be
            // different from the previous sender.
            messageSenders[i] = activeUsers[this.random.nextInt(activeCount)];
            while (i != 0 && messageSenders[i] == messageSenders[i-1]) {
                messageSenders[i] = activeUsers[this.random.nextInt(activeCount)];
            }
            this.random.nextBytes(messages[i]);
        }

    }

    /**
     * Generating an Active User Set
     */
    private Integer[] bootstrapActiveUsers(int n, int activeCount){
        Set<Integer> activeSet = new HashSet<>();
        activeSet.add(0); // We always want the initiator in the set.
        while (activeSet.size() < activeCount) {
            activeSet.add(random.nextInt(n));
        }
        return activeSet.toArray(new Integer[0]);
    }

    /**
     * Generating the Keys
     * @param identities
     */
    private KeyServer bootstrapKeys(DHPubKey[] identities, int n){
        // Create the necessary setup tooling in advance.
        for (int i = 0; i < n ; i++) {
            identities[i] = this.states[i].getIdentityKeyPair().getPubKey();
        }
        return new KeyServer(states);
    }


    /**
     * @param params
     * @return
     */
    @Override
    public ResultStorage run( ExecutionParams params )
    {
        int n=(AbstractTestExecutorParams) params.getN();
        int activeCount=(AbstractTestExecutorParams) params.getActiveCount();
        boolean debug=(AbstractTestExecutorParams) params.isDebug();

        String testName = this.implementation.getClass().getSimpleName();
        TestResult result = new TestResult(testName, n, activeCount);

        // Bootstrapping code
        Integer[] active=this.bootstrapActiveUsers(n,activeCount);

        // Messages
        int[] messageSenders= new int[this.messagesToSend];
        byte[][] messages= new byte[this.messagesToSend][this.messageLength];
        this.bootstrapMessages(messageSenders,messages, activeCount,active);

        //Identities & keys
        DHPubKey[] identities = new DHPubKey[n];
        KeyServer keyServer = this.bootstrapKeys(identities,n);
        this.setupPhase.generateNecessaryPreKeys(this.states);


        this.setupArt(result,identities,keyServer,debug);
        this.setUpOthers(result,active,identities,keyServer,debug);

        this.testMessageSendAndReceive(result,messageSenders,messages,active,n,activeCount,debug);

        return result;
    }

    /**
     * Test message io and fetch results.
     * @param result
     * @param messageSenders
     * @param messages
     * @param active
     * @param n
     * @param activeCount
     * @param debug
     */
    private void testMessageSendAndReceive(TestResult result, int[] messageSenders,byte[][] messages, Integer[] active, int n, int activeCount,boolean debug)
    {
        Stopwatch stopwatch1 = new Stopwatch();
        Stopwatch stopwatch2 = new Stopwatch();

        //Message exchange
        // Use stopwatch1 for sender, stopwatch2 for receiving.
        if (debug) Utils.print("Sending " + messagesToSend + " messages.");
        int totalSendSizes = 0;
        int totalReceiveSizes = 0;

        for (int i = 0; i < messagesToSend; i++) {

            int sender = messageSenders[i];
            byte[] message = messages[i];
            stopwatch1.startInterval();
            MessageDistributer messageDistributer = implementation.sendMessage(states[sender], message);
            stopwatch1.endInterval();

            totalSendSizes += messageDistributer.totalSize();

            for (int j = 0; j < activeCount; j++) {
                int receiver = active[j];
                if (receiver == sender) {
                    continue;
                }
                byte[] received = messageDistributer.getUpdateMessageForParticipantNum(receiver);
                totalReceiveSizes += received.length;
                stopwatch2.startInterval();
                byte[] decrypted = implementation.receiveMessage(states[receiver], received);
                stopwatch2.endInterval();
                if (!Arrays.equals(message, decrypted)) {
                    Utils.except("Message doesn't match.");
                }
            }

        }

        if (debug) Utils.print("Total sending time " + stopwatch1.getTotal() + " nanoseconds.");
        if (debug) Utils.print("Total receiving time " + stopwatch2.getTotal() + " nanoseconds.");
        if (debug) Utils.print("Total bytes sent: " + totalSendSizes + ".");
        if (debug) Utils.print("Total bytes received: " + totalReceiveSizes + ".");
        result.setSendingTime(stopwatch1.getTotal());
        result.setReceivingTime(stopwatch2.getTotal());
        result.setBytesSent(totalSendSizes);
        result.setBytesReceived(totalReceiveSizes);

        if (debug) Utils.print("Ended test  run with " + n + " participants.\n---------------\n");
    }

    /**
     * Setting up the asyncronous rachet tree
     * @param result Object for recording the rersults
     * @param identities The Participant Identities
     * @param keyServer The keyServer where Each Participant retrieves the other user participant keys
     * @param debug Whether debug megssages will be displayed
     */
    private void setupArt(TestResult result, DHPubKey[] identities, KeyServer keyServer, boolean debug)
    {
        Stopwatch stopwatch1 = new Stopwatch();
        if (debug) Utils.print("Setting up session for initiator.");
        stopwatch1.startInterval();
        this.setupPhase.setupInitiator(this.implementation, this.states, identities, keyServer);
        stopwatch1.endInterval();
        if (debug) Utils.print("Took " + stopwatch1.getTotal() + " nanoseconds.");
        if (debug) Utils.print("Initiator sent " + this.setupPhase.getBytesSentByInitiator() + " bytes.");
        result.setInitiatorSetupTime(stopwatch1.getTotal());
        result.setInitiatorSetupBytes(this.setupPhase.getBytesSentByInitiator());
        stopwatch1.reset();
    }

    /**
     * Set up other
     * @param result
     * @param active
     * @param identities
     * @param keyServer
     * @param debug
     */
    private void setUpOthers(TestResult result, Integer[] active, DHPubKey[] identities, KeyServer keyServer,boolean debug)
    {
        Stopwatch stopwatch1 = new Stopwatch();

        if (debug) Utils.print("Setting up session for " + active.length + " peers.");
        stopwatch1.startInterval();
        this.setupPhase.setupAllOthers(implementation, this.states, active, identities, keyServer);
        stopwatch1.endInterval();
        if (debug) Utils.print("Took " + stopwatch1.getTotal() + " nanoseconds.");
        if (debug) Utils.print("Others received " + setupPhase.getBytesReceivedByOthers() + " bytes.");
        if (debug) Utils.print("Others sent " + setupPhase.getBytesSentByOthers() + " bytes.");
        result.setOthersSetupTime(stopwatch1.getTotal());
        result.setOthersSetupBytesReceived(setupPhase.getBytesReceivedByOthers());
        result.setOthersSetupBytesSent(setupPhase.getBytesSentByOthers());
        stopwatch1.reset();
    }


    abstract protected void postSetupExecution(TestResult result, ExecutionParams params, Integer[] activeUsers,  DHPubKey[] identities, KeyServer keyServer);
}
