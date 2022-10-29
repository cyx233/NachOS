package nachos.threads;

import nachos.machine.*;
import java.util.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
        wakeUpTimeMap = new HashMap<>();
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
        boolean intStatus = Machine.interrupt().disable();
        Iterator<Map.Entry<KThread, Long>> it =  wakeUpTimeMap.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<KThread, Long> record = it.next();
            if(record.getValue() <= Machine.timer().getTime()){
                record.getKey().ready();
                it.remove();
            }
        }
        Machine.interrupt().restore(intStatus);
		KThread.yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		if (x < 0)
		    return;
		long wakeTime = Machine.timer().getTime() + x;
        wakeUpTimeMap.put(KThread.currentThread(), wakeTime);
		boolean intStatus = Machine.interrupt().disable();
        KThread.sleep();
		Machine.interrupt().restore(intStatus);
	}

        /**
	 * Cancel any timer set by <i>thread</i>, effectively waking
	 * up the thread immediately (placing it in the scheduler
	 * ready set) and returning true.  If <i>thread</i> has no
	 * timer set, return false.
	 * 
	 * <p>
	 * @param thread the thread whose timer should be cancelled.
	 */
    public boolean cancel(KThread thread) {
        boolean ret = wakeUpTimeMap.remove(thread) != null;
        if(ret){
            boolean intStatus = Machine.interrupt().disable();
            thread.ready();
            Machine.interrupt().restore(intStatus);
        }
        return ret;
	}

    public static void waitUntilTest() {
        int durations[] = {1*1000, 2*1000, 3*1000, -1*1000};
        long t0, t1;
        System.out.println ("waitUntilTest:");
        for (int d : durations) {
            t0 = Machine.timer().getTime();
            ThreadedKernel.alarm.waitUntil(d);
            t1 = Machine.timer().getTime();
            System.out.println ("waited for " + (t1 - t0) + " ticks");
        }
        System.out.println ("waitUntilTest end.");
    }

    public static void cancelTest() {
        int d = 1000;
        System.out.println("cancelTest:");
        KThread t1 = new KThread( new Runnable () {
            public void run() {
                long t0 = Machine.timer().getTime();
                ThreadedKernel.alarm.waitUntil(d);
                long t1 = Machine.timer().getTime();
                Lib.assertTrue((t1-t0)>=d);
                System.out.println ("without cancel(), waited for " + (t1 - t0) + " ticks");
            }
        });
        KThread t2 = new KThread( new Runnable () {
            public void run() {
                long t0 = Machine.timer().getTime();
                ThreadedKernel.alarm.waitUntil(d);
                long t1 = Machine.timer().getTime();
                System.out.println ("with cancel(), waited for " + (t1 - t0) + " ticks");
                Lib.assertTrue((t1-t0)<d);
            }
        });
        t1.fork();
        KThread.yield();
        t1.join();

        t2.fork();
        KThread.yield();
        ThreadedKernel.alarm.cancel(t2);
        t2.join();
        System.out.println("cancelTest end.");
    }
    public static void cancelTwiceTest() {
        int d = 1000;
        System.out.println("cancelTwiceTest:");
        KThread t1 = new KThread( new Runnable () {
            public void run() {
                ThreadedKernel.alarm.waitUntil(d);
            }
        });
        t1.fork();
        KThread.yield();
        System.out.println("cancel: " + ThreadedKernel.alarm.cancel(t1));
        System.out.println("cancel again: " + ThreadedKernel.alarm.cancel(t1));
        t1.join();

        System.out.println("cancelTwiceTest end.");
    }
    public static void selfTest() {
        waitUntilTest();
        cancelTest();
        cancelTwiceTest();
    }

    private HashMap<KThread, Long> wakeUpTimeMap = null;
}
