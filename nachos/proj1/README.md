# Teamwork
https://cseweb.ucsd.edu/classes/fa22/cse120-a/projects/project1.html
## Group Member
|Name|PID|Email|
|-|-|-|
|Yuxiang Chen|A59016369|yuc129@ucsd.edu|
|Jiale Xu|A15123298|jix012@ucsd.edu|
## TODO
- [x] Alarm.waitUntil - Yuxiang, Jiale
- [x] KThread.join - Yuxiang, Jiale
- [x] Condition2.{sleep, wake, wakeAll} - Yuxiang, Jiale
- [x] Alarm.cancel, Condition2.sleepFor - Yuxiang, Jiale
- [x] Rendezvous.exchange - Yuxiang
- [x] (Extra) Future.get - Yuxiang

## Implementation 
### Alarm
```java
private HashMap<KThread, Long> wakeUpTimeMap = null;

public void timerInterrupt() {
    Iterator<Map.Entry<KThread, Long>> it =  wakeUpTimeMap.entrySet().iterator();
    while(it.hasNext()){
        Map.Entry<KThread, Long> record = it.next();
        if(record.getValue() <= Machine.timer().getTime()){
            record.getKey().ready();
            it.remove();
        }
    }
    KThread.yield();
}

public void waitUntil(long x) {
    long wakeTime = Machine.timer().getTime() + x;
    wakeUpTimeMap.put(KThread.currentThread(), wakeTime);
    boolean intStatus = Machine.interrupt().disable();
    KThread.sleep(); // (a)
    Machine.interrupt().restore(intStatus);
}

public boolean cancel(KThread thread) {
    boolean ret = wakeUpTimeMap.remove(thread) != null;
    if(ret){
        boolean intStatus = Machine.interrupt().disable();
        thread.ready();
        Machine.interrupt().restore(intStatus);
    }
    return ret;
}
```
Use a ```HashMap wakeUpTimeMap``` to save alart events as KThread-TimeStamp pairs. 
During every timerInterrupt, traversal the list and set ready status to threads whose TimeStamp is earlier than the current. These threads will continue in **line (a)**.
Canceling an alarm events is defined by removing a Kthread-TimeStamp pair from the ```wakeUpTimeMap``` directly. And then set the status to ready.

```Alarm``` class is tested as follows: ```waitUntil``` is tested with four inputs including negative waiting time which is handled by simply returning. ```cancel``` is tested with two cases: simple cancel and cancel twice. The latter test makes sure that after a thread has been cancelled, cancelling it again does not cause exceptions.

### KThread.join 
```java
private KThread joinThread = null;
public static void finish() {
    ...
    if(currentThread.joinThread != null)
        currentThread.joinThread.ready();
    ...
}
public void join() {
    Lib.debug(dbgThread, "Joining to thread: " + toString());

    Lib.assertTrue(this != currentThread);
    if(status == statusFinished)
        return;
    else{
        Lib.assertTrue(joinThread == null);
        boolean intStatus = Machine.interrupt().disable();
        joinThread = currentThread;
        KThread.sleep(); // (a)
        joinThread = null;
        Machine.interrupt().restore(intStatus);
    }
}
```
Use a private variable of joined thread B to save the current thread A. Then set the Thread A to sleep status.
In ```finish()``` function of the Thread B, awake the Thread A. The Thread A will continue in **line (a)**.

```KThread.join()```  is tested as follows: 1. Yield and join one KThread; 2. Yield and join two KThreads; 3. Test if join self thread is handled; 4. Make sure join() can be called on a thread at most once.

### Condition2
```java
private Lock conditionLock;
private LinkedList<KThread> waitQueue = null;

public void sleep() {
    Lib.assertTrue(conditionLock.isHeldByCurrentThread());

    waitQueue.add(KThread.currentThread());

    boolean intStatus = Machine.interrupt().disable();
    conditionLock.release();
    KThread.sleep(); // (a)
    Machine.interrupt().restore(intStatus);

    conditionLock.acquire();
}

public void sleepFor(long timeout) {
    Lib.assertTrue(conditionLock.isHeldByCurrentThread());

    waitQueue.add(KThread.currentThread());

    boolean intStatus = Machine.interrupt().disable();
    conditionLock.release();
    ThreadedKernel.alarm.waitUntil(timeout); // (b)
    Machine.interrupt().restore(intStatus);

    conditionLock.acquire();
    waitQueue.remove(KThread.currentThread());
}

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

public void wakeAll() {
    Lib.assertTrue(conditionLock.isHeldByCurrentThread());

    boolean intStatus = Machine.interrupt().disable();
    while (!waitQueue.isEmpty()){
        KThread t = waitQueue.removeFirst();
        if(!ThreadedKernel.alarm.cancel(t)){
            t.ready();
        }
    }
    Machine.interrupt().restore(intStatus);
}

```
Use a ```LinkedList waitQueue``` to save waiting threads.

```sleep()``` will call ```Kthread.sleep()``` in **line (a)**. Threads must be woke by ```wake()``` or ```wakeAll()```.

```sleepFor()``` will call ```Alarm.waitUntil()``` in **line (b)**. In this case, threads can be woke by any of ```wake()```, ```wakeAll()``` and alarm events ```timerInterrupt()```. A Thread that has called ```sleepFor()``` will try to remove itself from the ```waitQueue``` when it is woken.

```Condition2``` is tested as follows: ```InterlockTest``` is intended to check the ```Lock``` class implementation asserts thread lock properly; ``` cvTest5()``` checks that condition values are manipulated correctly; ```sleepForTest()``` checks whether the sleeping time is correct; ```sleepForWakeTest``` checks if KThread can sleep and be waken properly.

### Rendezvous
```java
private HashMap<Integer, Integer> exchangeMap = null;
private HashMap<Integer, Condition2> conditionMap = null;
private Lock lock = null;
public int exchange (int tag, int value) {
    lock.acquire();
    if(exchangeMap.containsKey(tag)){
        int r = exchangeMap.get(tag);
        exchangeMap.put(tag, value);
        conditionMap.get(tag).wake(); // (b)

        lock.release();
        return r;
    }
    else{
        Condition2 cv = new Condition2(lock);

        conditionMap.put(tag, cv);
        exchangeMap.put(tag, value);

        cv.sleep(); // (a)

        int r = exchangeMap.get(tag);
        exchangeMap.remove(tag);
        conditionMap.remove(tag);

        lock.release();
        return r;
    }
}
```
The ```exchangeMap``` saves Tag-Value pairs. The ```conditionMap``` saves tag-Condition Variable pairs.

When the first thread A calls ```exchange()```, the tag-value will be saved in the ```exchangeMap```, and a ```Condition2``` will be create and be saved in the ```conditionMap```.

When the second thread B calls ```exchange()```, it will get value from the ```exchangeMap``` and store it in local variable ```r```, then Thread B modifies the ```exchangeMap``` with its tag-value pair. Finally, it will wake up Thread A in **line (b)**. Thread A will continue in **line (a)**. And Thread A will get value from the ```exchangeMap``` that has been modified by Thread B.

```Rendezvous``` is tested as follows: ```rendezTest1Thread()``` designed how KThreads are are compared with each other. ```rendezTest1()``` creates the KThread objects to be compared and test the ```exchange()``` function.

### Future
```java
private Lock lock = null;  
private Condition2 cv = null;  
private Boolean finished = null;  
private Integer ret = null;
public Future (IntSupplier function) {
    lock = new Lock();
    cv = new Condition2(lock);
    finished = false;

    KThread t = new KThread( new Runnable () {
        public void run() {
            ret = function.getAsInt();
            lock.acquire();
            finished = true; 
            cv.wakeAll(); // (b)
            lock.release();
        }
    });
    t.fork();
}

public int get () {
    lock.acquire();
    if(!finished){
        cv.sleep(); // (a)
    }
    lock.release();
    return ret;
}
```
Implemented by ```Condition2```. The ```finished``` shows whether the result is available.

When a ```Future``` is created, it will start a child thread T for the given function.

Before finishing, all threads will wait in **line (a)**.

When the Thread T finishes the given function,  it will change the ```ret``` and  ```finished```. Then in **line (b)**, Thread T wakes up all waiting threads. These threads continue in **line (a)**.
