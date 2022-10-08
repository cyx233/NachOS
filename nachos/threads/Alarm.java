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
        waitUntilQueue = new PriorityQueue<>();
        wakeUpMap = new HashMap<>();
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
        while(waitUntilQueue.size()>0 && waitUntilQueue.peek() <= Machine.timer().getTime()){
            long cur = waitUntilQueue.poll();
            for(KThread t: wakeUpMap.get(cur))
                t.ready();
            wakeUpMap.remove(cur);
        }
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
		long wakeTime = Machine.timer().getTime() + x;
        if(!waitUntilQueue.contains(wakeTime))
            waitUntilQueue.add(wakeTime);
            wakeUpMap.put(wakeTime, new HashSet<>());
        wakeUpMap.get(wakeTime).add(KThread.currentThread());
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
		return false;
	}
    public static void alarmTest1() {
        int durations[] = {1*1000000, 2*1000000, 3*1000000, 4*1000000};
        long t0, t1;
        for (int d : durations) {
            t0 = Machine.timer().getTime();
            ThreadedKernel.alarm.waitUntil (d);
            t1 = Machine.timer().getTime();
            System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
        }
    }

    public static void selfTest() {
        alarmTest1();
    }

    private PriorityQueue<Long> waitUntilQueue = null;
    private HashMap<Long, HashSet<KThread>> wakeUpMap = null;
}
