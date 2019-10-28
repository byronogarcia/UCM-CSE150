package nachos.threads;
import java.util.*;
import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically re-acquire the lock before <tt>sleep()</tt> returns.
     */
    LinkedList<KThread> waiting_queue = new LinkedList<KThread>(); // makes new queue call the waiting_queue
    
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	Machine.interrupt().disable();
	conditionLock.release();
	waiting_queue.add(KThread.currentThread()); // calling sleep(), will put this thread onto the waiting_queue
	KThread.currentThread().sleep(); // goes to sleep
	conditionLock.acquire();
	Machine.interrupt().enable();		
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	Machine.interrupt().disable();
		KThread temp; // temp thread will be the next thread on the waiting_queue
		if((temp = waiting_queue.poll()) != null) //check if sleep_queue is empty or not
		{
			temp.ready(); //if not null, put it on the ready queue
		}
	Machine.interrupt().enable();
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	Machine.interrupt().disable();
		KThread temp; // temp thread will be the next thread on the waiting_queue
		while((temp = waiting_queue.poll()) != null) //"continue" checking if waiting_queue is empty or not
		{
			temp.ready(); //if not null, put it on the ready queue
		}
	Machine.interrupt().enable();
    }

    private Lock conditionLock;
    }
