package nachos.threads;
import nachos.ag.BoatGrader;
import java.util.LinkedList;
import nachos.machine.*;

public class Boat
{
	static BoatGrader bg;
	static int childrenOnOahu = 0;
	static int childPair = 0;
	static int adultsOnOahu = 0;
	static boolean childOnMolokai = false;
	static Lock lock = new Lock();
	static Condition childrenCounted = new Condition(lock);
	static Condition pairReady = new Condition(lock);
	static Condition ridePair = new Condition(lock);
	static Condition finishRide = new Condition(lock);
	static Condition adultRow = new Condition(lock);
	static Condition boatOnMolokai = new Condition(lock);

	public static void selfTest()
	{
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		begin(1, 2, b);

		System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		begin(3, 3, b);
	}

	public static void begin( int adults, int children, BoatGrader b )
	{
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
		bg = b;
		LinkedList<KThread> threads = new LinkedList<KThread>();

		System.out.println("began");
		Scheduler s = new RoundRobinScheduler();
	// Instantiate global variables here
		childrenOnOahu = 0;
		childPair = 0;
		adultsOnOahu = 0;
		childOnMolokai = false;
		lock = new Lock();
		childrenCounted = new Condition(lock);
		pairReady = new Condition(lock);
		ridePair = new Condition(lock);
		finishRide = new Condition(lock);
		adultRow = new Condition(lock);
		boatOnMolokai = new Condition(lock);

	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

		Runnable rc = new Runnable() {
			public void run() {
				ChildItinerary(/*lock, childrenOnOahu, childPair, adultsOnOahu, childOnMolokai, childrenCounted, pairReady, ridePair, finishRide, adultRow, boatOnMolokai, bg*/);
			}
		};

		Runnable ra = new Runnable() {
			public void run() {
				AdultItinerary(/*lock, adultsOnOahu, childOnMolokai, childrenCounted, adultRow, boatOnMolokai, bg*/);
			}
		};

		boolean status = Machine.interrupt().disable();
		for(int c = 0; c < children; c++) {
			KThread thread = new KThread(rc);
			String name = "Child Thread " + c;
			thread.setName(name);
			thread.fork();
			threads.add(thread);
			System.out.println("child");
		}
		for(int a = 0; a < adults; a++) {
			KThread thread = new KThread(ra);
			String name = "Adult Thread " + a;
			thread.setName(name);
			thread.fork();
			threads.add(thread);
			System.out.println("adult");
		}

		Machine.interrupt().restore(status);

		while (!threads.isEmpty()) {
			threads.removeFirst().join();
		}
        // KThread t = new KThread(r);
        // t.setName("Sample Boat Thread");
        // t.fork();

	}

	static void AdultItinerary(/*Lock lock, int adultsOnOahu, boolean childOnMolokai, Condition childrenCounted, Condition adultRow, Condition boatOnMolokai, BoatGrader bg*/)
	{
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/

	   lock.acquire();

	   adultsOnOahu++;
	   lock.release();
	   KThread.yield();
	   lock.acquire();
	   System.out.println("adult here");
	   while(!childOnMolokai) {
	   	adultRow.sleep();
	   }
	   adultsOnOahu--;
	   bg.AdultRowToMolokai();

	   System.out.println("adult rowed");
	   boatOnMolokai.wake();
	   lock.release();
	   return;
	}

	static void ChildItinerary(/*Lock lock, int childrenOnOahu, int childPair, int adultsOnOahu, boolean childOnMolokai, Condition childrenCounted, Condition pairReady, Condition ridePair, Condition finishRide, Condition adultRow, Condition boatOnMolokai, BoatGrader bg*/)
	{
		lock.acquire();

		childrenOnOahu++;
		lock.release();
		KThread.yield();
		lock.acquire();
	    // childrenCounted.sleep();
		childPair++;
		System.out.println("child here, "+childrenOnOahu + " pair: "+ childPair);
	// System.out.println("child there");
		while(childrenOnOahu>1 || adultsOnOahu > 0){
			if(childPair >= 2) {
				System.out.println("woke");
				pairReady.wake();
				System.out.println("shlep");
				ridePair.sleep();
				childPair--;
				childrenOnOahu--;
				System.out.println("rode");
				bg.ChildRideToMolokai();
				childOnMolokai = true;
				finishRide.wake();
				if(adultsOnOahu == 0) {
					System.out.println("staying");
					lock.release();
					return;
				}
				boatOnMolokai.sleep();

				System.out.println("woke2");
				bg.ChildRowToOahu();
				childOnMolokai = false;
				childPair++;
				childrenOnOahu++;
			}
			else if(childPair < 2){
				pairReady.sleep();
				childPair--;
				childrenOnOahu--;
				System.out.println("rowed1");
				bg.ChildRowToMolokai();
				ridePair.wake();
				finishRide.sleep();
				bg.ChildRowToOahu();
				childPair++;
				childrenOnOahu++;
				adultRow.wake();
			}
		}
		System.out.println("rowed2");
		bg.ChildRowToMolokai();
		lock.release();
		return;
	}

	static void SampleItinerary()
	{
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}
}
