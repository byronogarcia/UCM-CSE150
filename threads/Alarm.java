package nachos.threads;
import java.util.*;
import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */

		
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */	
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }
    
    //class that holds thread with its waketime
    class thread_waketime{
    	public long waketime;
    	public KThread current_thread;
    	public thread_waketime(long waketime, KThread current_thread) {
    		this.waketime = waketime;
    		this.current_thread = current_thread;
    	}
    }
    //comparator class that compares the wake time of threads
    class sort_by_time implements Comparator<thread_waketime>{
		public int compare(thread_waketime thread1, thread_waketime thread2) {
			//return positive if thread1 > thread2
			if(thread1.waketime > thread2.waketime) {
				return 1;
			}
			//return negative if thread1 < thread2
			else if(thread1.waketime < thread2.waketime) {
				return -1;
			}
			//return 0 if thread1 = thread2
			else if(thread1.waketime == thread2.waketime) {
				return 0;
			}
			return 0;
		}
    }
    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    Comparator<thread_waketime> compare = new sort_by_time();
    PriorityQueue<thread_waketime> waiting_queue = new PriorityQueue<thread_waketime>(1, compare);
    
    public void timerInterrupt() {
	KThread.currentThread().yield();
	Machine.interrupt().disable();
	while((waiting_queue.peek() != null) && (waiting_queue.peek().waketime <= Machine.timer().getTime())) { //check if the first element in the queue is not empty, and the wake time is at the timer of the machine
		waiting_queue.poll().current_thread.ready(); // if so, put it in ready queue()
	}
	Machine.interrupt().enable();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
    	Machine.interrupt().disable();
    	long wakeTime = Machine.timer().getTime() + x;
    	thread_waketime cur_thread = new thread_waketime(wakeTime, KThread.currentThread()); //add current_thread + its waketime to the thread_waketime class
    	waiting_queue.add(cur_thread); // add it to the priority queue
    	KThread.currentThread().sleep(); 
    	Machine.interrupt().enable();
    }
}

