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
    public TestResultItem run( ExecutionParams params )
    {
        AbstractTestExecutorParams execParams= (AbstractTestExecutorParams)params;
        int n=execParams.getN();
        int activeCount= execParams.getActiveCount();
        boolean debug=execParams.isDebug();

        String testName = this.implementation.getClass().getSimpleName();
        TestResultItem result = new TestResultItem();

        // Bootstrapping code
        Integer[] active=this.bootstrapActiveUsers(n,activeCount);



        //Identities & keys
        DHPubKey[] identities = new DHPubKey[n];
        KeyServer keyServer = this.bootstrapKeys(identities,n);
        this.setupPhase.generateNecessaryPreKeys(this.states);


        this.setupArt(result,identities,keyServer,debug);
        this.setUpOthers(result,active,identities,keyServer,debug);

        this.testMessageSendAndReceive(result,active,n,activeCount,debug);

        this.postSetupExecution(result,params,active,identities,keyServer);

        return result;
    }

    /**
     * Test message io and fetch results.
     * @param result
     * @param active
     * @param n
     * @param activeCount
     * @param debug
     */
    private void testMessageSendAndReceive(TestResultItem result, Integer[] active, int n, int activeCount,boolean debug)
    {

        // Messages
        int[] messageSenders= new int[this.messagesToSend];
        byte[][] messages= new byte[this.messagesToSend][this.messageLength];
        this.bootstrapMessages(messageSenders,messages, activeCount,active);

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
        //result.setSendingTime(stopwatch1.getTotal());
        result.addResult("sendindg_time", stopwatch1.getTotal());
        result.addResult("receiving_time", stopwatch2.getTotal());
        result.addResult("bytes_sent",totalSendSizes);
        result.addResult("bytes_received",totalReceiveSizes);

        if (debug) Utils.print("Ended test  run with " + n + " participants.\n---------------\n");
    }

    /**
     * Setting up the asyncronous rachet tree
     * @param result Object for recording the rersults
     * @param identities The Participant Identities
     * @param keyServer The keyServer where Each Participant retrieves the other user participant keys
     * @param debug Whether debug megssages will be displayed
     */
    private void setupArt(TestResultItem result, DHPubKey[] identities, KeyServer keyServer, boolean debug)
    {
        Stopwatch stopwatch1 = new Stopwatch();
        if (debug) Utils.print("Setting up session for initiator.");
        stopwatch1.startInterval();
        this.setupPhase.setupInitiator(this.implementation, this.states, identities, keyServer);
        stopwatch1.endInterval();
        if (debug) Utils.print("Took " + stopwatch1.getTotal() + " nanoseconds.");
        if (debug) Utils.print("Initiator sent " + this.setupPhase.getBytesSentByInitiator() + " bytes.");
        result.addResult("initiator_setup_time",stopwatch1.getTotal());
        result.addResult("initiator_setup_bytes",this.setupPhase.getBytesSentByInitiator());
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
    private void setUpOthers(TestResultItem result, Integer[] active, DHPubKey[] identities, KeyServer keyServer,boolean debug)
    {
        Stopwatch stopwatch1 = new Stopwatch();

        if (debug) Utils.print("Setting up session for " + active.length + " peers.");
        stopwatch1.startInterval();
        this.setupPhase.setupAllOthers(implementation, this.states, active, identities, keyServer);
        stopwatch1.endInterval();
        if (debug) Utils.print("Took " + stopwatch1.getTotal() + " nanoseconds.");
        if (debug) Utils.print("Others received " + setupPhase.getBytesReceivedByOthers() + " bytes.");
        if (debug) Utils.print("Others sent " + setupPhase.getBytesSentByOthers() + " bytes.");
        result.addResult("other_setup_time", stopwatch1.getTotal());
        result.addResult("other_setup_received_bytes",setupPhase.getBytesReceivedByOthers());
        result.addResult("item_setup_sent_bytes",setupPhase.getBytesSentByOthers());
        stopwatch1.reset();
    }


    abstract protected void postSetupExecution(TestResultItem result, ExecutionParams params, Integer[] activeUsers,  DHPubKey[] identities, KeyServer keyServer);
}
