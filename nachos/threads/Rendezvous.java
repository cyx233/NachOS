package nachos.threads;

import nachos.machine.*;
import java.util.HashMap;

/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 */
public class Rendezvous {
    /**
     * Allocate a new Rendezvous.
     */
    public Rendezvous () {
        lock = new Lock();
        conditionMap = new HashMap<Integer, Condition2>();
        exchangeMap = new HashMap<Condition2, Integer>();
    }

    /**
     * Synchronously exchange a value with another thread.  The first
     * thread A (with value X) to exhange will block waiting for
     * another thread B (with value Y).  When thread B arrives, it
     * will unblock A and the threads will exchange values: value Y
     * will be returned to thread A, and value X will be returned to
     * thread B.
     *
     * Different integer tags are used as different, parallel
     * synchronization points (i.e., threads synchronizing at
     * different tags do not interact with each other).  The same tag
     * can also be used repeatedly for multiple exchanges.
     *
     * @param tag the synchronization tag.
     * @param value the integer to exchange.
     */
    public int exchange (int tag, int value) {
        lock.acquire();
        if(conditionMap.containsKey(tag)){
            Condition2 cv = conditionMap.get(tag);
            cv.wake();
            int r = exchangeMap.get(cv);
            exchangeMap.put(cv, value);
            conditionMap.remove(tag);

            lock.release();
            return r;
        }
        else{
            Condition2 cv = new Condition2(lock);

            conditionMap.put(tag, cv);
            exchangeMap.put(cv, value);

            cv.sleep();

            int r = exchangeMap.get(cv);
            exchangeMap.remove(cv);

            lock.release();
            return r;
        }
    }
    private static KThread rendezTest1Thread(String name, Rendezvous r, int send, int tag, int target) {
        KThread t = new KThread(new Runnable() {
            public void run() {
                System.out.println("Thread " + 
                        KThread.currentThread().getName() + " exchanging " + send + " (tag " + tag +")");
                int recv = r.exchange(tag, send);
                Lib.assertTrue(recv == target, 
                        "Was expecting " + target + " but received " + recv);
                System.out.println("Thread " + 
                        KThread.currentThread().getName() + " received " + recv);
            }
        });
        t.setName(name);
        return t;
    }

    public static void rendezTest1() {
        System.out.println("redezTest:");
        final Rendezvous r = new Rendezvous();
        KThread t1 = rendezTest1Thread("t1", r, 1, 0, -1);
        KThread t2 = rendezTest1Thread("t2", r, -1, 0, 1);
        KThread t3 = rendezTest1Thread("t3", r, 2, 1, -2);
        KThread t4 = rendezTest1Thread("t4", r, -2, 1, 2);
        t1.fork(); t3.fork(); t2.fork(); t4.fork();
        t1.join(); t2.join(); t3.join(); t4.join();
        System.out.println("redezTest end");
    }

    public static void oddTest() {
        System.out.println("redezTest:");
        final Rendezvous r = new Rendezvous();
        KThread t1 = rendezTest1Thread("t1", r, 1, 0, -1);
        KThread t2 = rendezTest1Thread("t2", r, -1, 0, 1);
        KThread t3 = rendezTest1Thread("t3", r, 2, 1, -2);
        t1.fork(); t3.fork(); t2.fork();
        t1.join(); t2.join(); t3.join();
        System.out.println("Error! should be blocked");
    }


    public static void selfTest() {
        rendezTest1();
        // oddTest();
    }

    private HashMap<Integer, Condition2> conditionMap = null;
    private HashMap<Condition2, Integer> exchangeMap = null;
    private Lock lock = null;
}
