package nachos.threads;

import java.util.*;
import java.util.function.IntSupplier;
import nachos.machine.*;

/**
 * A <i>Future</i> is a convenient mechanism for using asynchonous
 * operations.
 */
public class Future {
    /**
     * Instantiate a new <i>Future</i>.  The <i>Future</i> will invoke
     * the supplied <i>function</i> asynchronously in a KThread.  In
     * particular, the constructor should not block as a consequence
     * of invoking <i>function</i>.
     */
    public Future (IntSupplier function) {
        lock = new Lock();
        cv = new Condition2(lock);
        finished = false;

        KThread t = new KThread( new Runnable () {
            public void run() {
                ret = function.getAsInt();
                lock.acquire();
                finished = true;
                cv.wakeAll();
                lock.release();
            }
        });
        t.fork();
    }

    /**
     * Return the result of invoking the <i>function</i> passed in to
     * the <i>Future</i> when it was created.  If the function has not
     * completed when <i>get</i> is invoked, then the caller is
     * blocked.  If the function has completed, then <i>get</i>
     * returns the result of the function.  Note that <i>get</i> may
     * be called any number of times (potentially by multiple
     * threads), and it should always return the same value.
     */
    public int get () {
        lock.acquire();
        if(!finished){
            cv.sleep();
        }
        lock.release();
        return ret;
    }
    public static void basicTest(){
        IntSupplier fib = new IntSupplier() {
                private int previous = 0;
                private int current = 1;

                public int getAsInt() {
                int nextValue = this.previous + this.current;
                this.previous = this.current;
                this.current = nextValue;
                return this.previous;
            }
        };
        Future f = new Future(
            new IntSupplier() {
                public int getAsInt() {
                    System.out.println("Future begin");
                    long t0 = Machine.timer().getTime();
                    ThreadedKernel.alarm.waitUntil(1000);
                    long t1 = Machine.timer().getTime();
                    System.out.println("Future end.");
                    return (int)(t1-t0);
                }
            }
        );
        KThread t1 = new KThread( new Runnable () {
            public void run() {
                System.out.println(KThread.currentThread().getName()+" Future call");
                int ret = f.get();
                System.out.println(KThread.currentThread().getName()+" gets ans: "+ret);
            }
        });
        t1.setName("process 1");
        KThread t2 = new KThread( new Runnable () {
            public void run() {
                System.out.println(KThread.currentThread().getName()+" Future call");
                int ret = f.get();
                System.out.println(KThread.currentThread().getName()+" gets ans: "+ret);
            }
        });
        t2.setName("process 2");
        t1.fork(); t2.fork();
        t1.join(); t2.join();
    }

    public static void selfTest(){
        basicTest();
    }
    private Lock lock = null;  
    private Condition2 cv = null;  
    private Boolean finished = null;  
    private Integer ret = null;
}
