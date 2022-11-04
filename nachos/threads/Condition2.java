package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		waitQueue = new LinkedList<KThread>();
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();
        waitQueue.add(KThread.currentThread());
		conditionLock.release();
        KThread.sleep();
        Machine.interrupt().restore(intStatus);

		conditionLock.acquire();
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        if (!waitQueue.isEmpty()){
            KThread t = waitQueue.removeFirst();
            if(!ThreadedKernel.alarm.cancel(t)){
                t.ready();
            }
        }
        Machine.interrupt().restore(intStatus);
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        while (!waitQueue.isEmpty()){
			KThread t = waitQueue.removeFirst();
            if(!ThreadedKernel.alarm.cancel(t) && t.isSleep()){
                t.ready();
            }
        }
        Machine.interrupt().restore(intStatus);
	}

        /**
	 * Atomically release the associated lock and go to sleep on
	 * this condition variable until either (1) another thread
	 * wakes it using <tt>wake()</tt>, or (2) the specified
	 * <i>timeout</i> elapses.  The current thread must hold the
	 * associated lock.  The thread will automatically reacquire
	 * the lock before <tt>sleep()</tt> returns.
	 */
    public void sleepFor(long timeout) {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();
        waitQueue.add(KThread.currentThread());
		conditionLock.release();
        ThreadedKernel.alarm.waitUntil(timeout);
        waitQueue.remove(KThread.currentThread());
        System.out.println(waitQueue);
        Machine.interrupt().restore(intStatus);

		conditionLock.acquire();
	}

    private static class InterlockTest {
        private static Lock lock;
        private static Condition2 cv;

        private static class Interlocker implements Runnable {
            public void run () {
                lock.acquire();
                for (int i = 0; i < 10; i++) {
                    System.out.println(KThread.currentThread().getName());
                    cv.wake();   // signal
                    cv.sleep();  // wait
                }
                lock.release();
            }
        }

        public InterlockTest () {
            System.out.println("InterlockTest:");
            lock = new Lock();
            cv = new Condition2(lock);

            KThread ping = new KThread(new Interlocker());
            ping.setName("ping");
            KThread pong = new KThread(new Interlocker());
            pong.setName("pong");

            ping.fork();
            pong.fork();

            ping.join();
            for (int i = 0; i < 50; i++) { KThread.yield();  }
            System.out.println("InterlockTest end.");
        }
    }

    public static void cvTest5() {
        System.out.println("cvTest5:");
        final Lock lock = new Lock();
        // final Condition empty = new Condition(lock);
        final Condition2 empty = new Condition2(lock);
        final LinkedList<Integer> list = new LinkedList<>();

        KThread consumer = new KThread(new Runnable() {
            public void run() {
                lock.acquire();
                while(list.isEmpty()){
                    empty.sleep();
                }
                Lib.assertTrue(list.size() == 5, "List should have 5 values.");
                while(!list.isEmpty()) {
                    KThread.yield();
                    System.out.println("Removed " + list.removeFirst());
                }
                lock.release();
            }
        });

        KThread producer = new KThread( new Runnable () {
            public void run() {
                lock.acquire();
                for (int i = 0; i < 5; i++) {
                    list.add(i);
                    System.out.println("Added " + i);
                    KThread.yield();
                }
                empty.wake();
                lock.release();
            }
        } );

        consumer.setName("Consumer");
        producer.setName("Producer");
        consumer.fork();
        producer.fork();

        consumer.join();
        producer.join();
        System.out.println("cvTest5 end.");
    }

    private static void sleepForTest() {
        Lock lock = new Lock();
        Condition2 cv = new Condition2(lock);

        lock.acquire();
        long t0 = Machine.timer().getTime();
        System.out.println("sleepForTest:");
        System.out.println(KThread.currentThread().getName() + " sleeping");
        cv.sleepFor(2000);
        long t1 = Machine.timer().getTime();
        System.out.println(KThread.currentThread().getName() +
                " woke up, slept for " + (t1 - t0) + " ticks");
        System.out.println(KThread.currentThread().getName() + " should be removed from waitQueue: " + !cv.waitQueue.contains(KThread.currentThread()));
		Lib.assertTrue(!cv.waitQueue.contains(KThread.currentThread()));
        lock.release();
        System.out.println("sleepForTest end.");
    }

    private static class sleepForWakeTest{
        private static boolean wakeUp = false;
        public sleepForWakeTest() {
            Lock lock = new Lock();
            Condition2 cv = new Condition2(lock);

            System.out.println("sleepForWakeTest:");
            KThread t = new KThread( new Runnable () {
                public void run() {
                    System.out.println(KThread.currentThread().getName() + " sleeping");
                    long t0 = Machine.timer().getTime();
                    lock.acquire();
                    while(!wakeUp)
                        cv.sleepFor(20000);
                    long t1 = Machine.timer().getTime();
                    System.out.println(KThread.currentThread().getName() +
                            " woke up, slept for " + (t1 - t0) + " ticks");
                    Lib.assertTrue((t1-t0)<20000);
                    System.out.println(KThread.currentThread().getName() + " should be removed from waitQueue: " + !cv.waitQueue.contains(KThread.currentThread()));
                    Lib.assertTrue(!cv.waitQueue.contains(KThread.currentThread()));
                    lock.release();
                }
            }).setName("child");
            t.fork();
            KThread.yield();

            lock.acquire();
            wakeUp = true;
            System.out.println("wake up the child");
            cv.wake();
            lock.release();

            t.join();
            System.out.println("sleepForTest end.");
        }
    }

    public static void selfTest() {
        new InterlockTest();
        cvTest5();
        sleepForTest();
        new sleepForWakeTest();
    }

    private Lock conditionLock;
	private LinkedList<KThread> waitQueue = null;
}
