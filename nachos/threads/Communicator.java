package nachos.threads;

import java.util.ArrayList;


/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	private int TheWord;
	private Lock lock;
	private Condition speakWaits;
	private Condition listenWaits;
	private Condition listenerReady;
	private Condition transact;
	
	private boolean listnerStarted;
	private int speaker;
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		lock = new Lock();
		speakWaits = new Condition(lock);
		listenWaits = new Condition(lock);
		listenerReady = new Condition(lock);
		transact = new Condition(lock);
		listnerStarted = false;
		speaker = 0;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
		lock.acquire();
		speaker++;
		if (listnerStarted)
			listenerReady.wake();
		speakWaits.sleep();

		this.TheWord = new Integer(word);
		transact.wake();
		
		lock.release();
	}
	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		lock.acquire();
		
		while (listnerStarted)
			listenWaits.sleep();
		
		listnerStarted = true;
		while (speaker == 0)
			listenerReady.sleep();
		
		speakWaits.wake();
		transact.sleep();
		
		int word;
		word = this.TheWord;
		speaker--;
		listnerStarted = false;
		
		listenWaits.wake();
		lock.release();
		
		return word;

	}

	
	private static class CommTest implements Runnable {
		//this test obj may have valid speaker or listener.. not both
		public CommTest(Communicator comm, ArrayList<String> test) {
			this.testsuite = test;
			this.comm = comm;
		}
		
		
		@SuppressWarnings("static-access")
		public void run() {
			int i = testsuite.size() * 2;
			while(0 != i){
				if (!this.testsuite.isEmpty()){
					String val = testsuite.get(0);
					if (KThread.currentThread().getName().equals("COMMTESTER")){
						if (val.equals("S")){
							KThread thrd = new KThread(this);
							thrd.setName("Speaker").fork();
							//thrd.join();
						} else {
							KThread thrd = new KThread(this);
							thrd.setName("Listener").fork();
							//thrd.join();
						}
						KThread.currentThread().yield();
					}
					if ((val.equals("S")) && (KThread.currentThread().getName().equals("Speaker"))) {
						testsuite.remove(0);
						//System.out.println("Speak " + (++word) + " on " + KThread.currentThread().toString());
						System.out.println("Speak " + (++word));
						this.comm.speak(word);
						return;
						//System.out.println("Spoke " + (word) + " on " + KThread.currentThread().toString());	
					} else if ((val.equals("L")) && (KThread.currentThread().getName().equals("Listener"))){
						testsuite.remove(0);
						System.out.println(" heard " + this.comm.listen());// + " on " + KThread.currentThread().toString());
						return;
					} else if (!KThread.currentThread().getName().equals("COMMTESTER")){
						//the speak or write thread work is done
						return;
					}
				} else {
					KThread.currentThread().yield();
				}
				i--;
			}
			
		}

		private Communicator comm;
		private ArrayList<String> testsuite;
		private static int word = 100;
	}

	/**
	 * Test that this module is working.
	 */
	public static void selfTest() {

		//all use a single communicator object
		//Test1: 1 speaker and 1 listener
		//Test2: 2 speaker followed by 1 listener, then  1 speaker followed by 2 listener
	
		Communicator obj = new Communicator();
		ArrayList<String> test = new ArrayList<String>();
		
		test.add("L");
		test.add("S");
		test.add("L");
		test.add("L");
		test.add("S");
		test.add("S");
		test.add("S");
		test.add("L");
		test.add("S");
		test.add("L");
		CommTest ct = new CommTest(obj, test);
		
		
		KThread thrd1 = new KThread(ct);
		thrd1.setName("COMMTESTER").fork();
		//thrd1.join();
		//KThread.currentThread().yield();
		
		//KThread thrd2 = new KThread(ct);
		//thrd2.setName("Listener").fork();
		thrd1.join();
		//thrd2.join();
		
	}
}
