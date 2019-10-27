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
    	
    	lock = new Lock();
    	listenerWaitingQueue = new Condition(lock);
    	speakerWaitingQueue = new Condition(lock);
    	listenerRecieving = new Condition (lock);
    	speakerSending = new Condition(lock);
    	listenerWaiting = false;
    	speakerWaiting = false;
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
    	lock.acquire();
    	
    	while(speakerWaiting) {
    		speakerWaitingQueue.sleep();
    	}
    	
    	speakerWaiting = true;
    	holder = word;
    	
    	while(!listenerWaiting || !received ) {
    		listenerRecieving.wake();
    		speakerSending.sleep();
    	}
    	
    	listenerWaiting = false;
    	speakerWaiting = false;
    	received = false;
    	
    	speakerWaitingQueue.wake();
    	listenerWaitingQueue.wake();
    	lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	lock.acquire();
    	
    	while(listenerWaiting) {
    		listenerWaitingQueue.sleep();
    	}
    	
    	listenerWaiting = true;
    	
    	while(!speakerWaiting) {
    		listenerRecieving.sleep();
    	}
    	
    	speakerSending.wake();
    	received = true;
    	lock.release();
    	return holder;
    	
    }
    
    public static void selfTest() {
    	final Communicator com = new Communicator();
    	
    	KThread thread2 = new KThread(new Runnable() {
    		public void run() {
    			System.out.println("Thread 2 begin listening");
    			com.listen();
    			System.out.println("Thread 2 finished listening");
    		}
    	});
    	
    	KThread thread1 = new KThread(new Runnable() {
    		public void run() {
    			System.out.println("Thread 1 begin speaking");
    			com.speak(2);
    			System.out.println("Thread 1 finished speaking");
    		}
    	});
    	
    	thread1.fork();
    	thread2.fork();
    	thread1.join();
    	thread2.join();
    }

    
    private Condition speakerWaitingQueue;
    private Condition speakerSending;
    private Condition listenerWaitingQueue;
    private Condition listenerRecieving;
    
    private boolean listenerWaiting;
    private boolean speakerWaiting;
    private boolean received;
    
    private int holder;
    private Lock lock;
}
