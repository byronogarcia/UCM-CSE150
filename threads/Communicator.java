package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    	
        // lock for communication
    	lockCommunicator = new Lock();

    	// Speak condition
    	speaker = new Condition2(lockCommunicator);
        send = new Condition2(lockCommunicator);
        speakerWait = false;
        
        // Listen condition
        listener = new Condition2(lockCommunicator);
    	receive = new Condition2(lockCommunicator);
        listenerWait = false;

        // Check if successful
    	received = false;
    	
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        // Lock so that the task completes without interruption
    	lockCommunicator.acquire();
    	
        // Run while it waits for a speaker, puts it to sleep with the lock
    	while (speakerWait == true) {
    		speaker.sleep();
    	}
    	
        // Shows that a speaker is waiting and asleep, makes the word returningWord
    	speakerWait = true;
    	returningWord = word;
    	
        // While there is no listener and no word has been received, run the loop
    	while(listenerWait == false || received == false) {
    		receive.wake(); // Wake up all potential listeners
    		send.sleep();
    	}
    	
        // Set all to false to reset
    	listenerWait = speakerWait = received = false;

    	// Wake up speaker, wake up listener and pair both
    	speaker.wake();
    	listener.wake();
        
        // Release lock
    	lockCommunicator.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */

    public int listen() {
    	lockCommunicator.acquire();
    	
        // Runs loop while waiting for a listener, puts potential listener to sleep with lock
    	while (listenerWait == true) {
    		listener.sleep();
    	}
    	
        // Theres a waiting listener
    	listenerWait = true;
    	
        // Put to sleep a listener while speaker is waiting
    	while (speakerWait == false) {
    		receive.sleep();
    	}
    	
        // Exits both loops and the word has been 'received'
    	received = true;

        // Wake up potential sender and pair, release lock
        send.wake();
    	lockCommunicator.release();

        // Return spoken word
    	return returningWord;
    }


    
    private Condition2 speaker;
    private Condition2 send; // Speaker sends the word

    private Condition2 listener;
    private Condition2 receive; // Listener receives the word
    
    private boolean listenerWait; // Denotes a waiting listener
    private boolean speakerWait; // Denotes a waiting speaker
    private boolean received; // Denotes that a message has been received
    
    private int returningWord; // Holds the word to be returned
    private Lock lockCommunicator; //THE MUTEX
}
