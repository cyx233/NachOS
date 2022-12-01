package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.LinkedList;
import java.util.HashMap;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
	/**
	 * Allocate a new user kernel.
	 */
	public UserKernel() {
		super();
	}

	/**
	 * Initialize this kernel. Creates a synchronized console and sets the
	 * processor's exception handler.
	 */
	public void initialize(String[] args) {
		super.initialize(args);

		console = new SynchConsole(Machine.console());

		Machine.processor().setExceptionHandler(new Runnable() {
			public void run() {
				exceptionHandler();
			}
		});
        lock = new Lock();
        pipeMap = new HashMap<String, KernelPipe>();
		int numPhysPages = Machine.processor().getNumPhysPages();
        emptyPPN = new LinkedList<Integer>();
        for(int i=0; i<numPhysPages; ++i)
            emptyPPN.add(i);
	}

	/**
	 * Test the console device.
	 */
	public void selfTest() {
		super.selfTest();

		System.out.println("Testing the console device. Typed characters");
		System.out.println("will be echoed until q is typed.");

		char c;

		do {
			c = (char) console.readByte(true);
			console.writeByte(c);
		} while (c != 'q');

		System.out.println("");
	}

	/**
	 * Returns the current process.
	 * 
	 * @return the current process, or <tt>null</tt> if no process is current.
	 */
	public static UserProcess currentProcess() {
		if (!(KThread.currentThread() instanceof UThread))
			return null;

		return ((UThread) KThread.currentThread()).process;
	}

	/**
	 * The exception handler. This handler is called by the processor whenever a
	 * user instruction causes a processor exception.
	 * 
	 * <p>
	 * When the exception handler is invoked, interrupts are enabled, and the
	 * processor's cause register contains an integer identifying the cause of
	 * the exception (see the <tt>exceptionZZZ</tt> constants in the
	 * <tt>Processor</tt> class). If the exception involves a bad virtual
	 * address (e.g. page fault, TLB miss, read-only, bus error, or address
	 * error), the processor's BadVAddr register identifies the virtual address
	 * that caused the exception.
	 */
	public void exceptionHandler() {
		Lib.assertTrue(KThread.currentThread() instanceof UThread);

		UserProcess process = ((UThread) KThread.currentThread()).process;
		int cause = Machine.processor().readRegister(Processor.regCause);
		process.handleException(cause);
	}

	/**
	 * Start running user programs, by creating a process and running a shell
	 * program in it. The name of the shell program it must run is returned by
	 * <tt>Machine.getShellProgramName()</tt>.
	 * 
	 * @see nachos.machine.Machine#getShellProgramName
	 */
	public void run() {
		super.run();

		UserProcess process = UserProcess.newUserProcess();

		String shellProgram = Machine.getShellProgramName();
		if (!process.execute(shellProgram, new String[] {})) {
		    System.out.println ("Could not find executable '" +
					shellProgram + "', trying '" +
					shellProgram + ".coff' instead.");
		    shellProgram += ".coff";
		    if (!process.execute(shellProgram, new String[] {})) {
                System.out.println ("Also could not find '" +
                            shellProgram + "', aborting.");
                Lib.assertTrue(false);
		    }
		}
		KThread.finish();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

    public static Integer getPPN(){
        lock.acquire();
        Integer r = emptyPPN.poll();
        lock.release();
        return r;
    }

    public static void releasePPN(int ppn){
        lock.acquire();
        emptyPPN.add(ppn);
        lock.release();
    }

    public static int getEmptyPPN(){
        lock.acquire();
        int r = emptyPPN.size();
        lock.release();
        return r;
    }

    public static KernelPipe createPipe(String name){
        lock.acquire();
        if(pipeMap.containsKey(name) || pipeMap.size()==maxPipes){
            lock.release();
            return null;
        }
        Integer ppn = emptyPPN.poll();
        if(ppn==null){
            lock.release();
            return null;
        }
        KernelPipe p = new KernelPipe(name, ppn);
        p.openCount += 1;
        pipeMap.put(name, p);
        lock.release();
        return p;
    }

    public static KernelPipe openPipe(String name){
        lock.acquire();
        KernelPipe p = pipeMap.get(name);
        lock.release();
        if(p!=null)
            p.openCount += 1;
        return p;
    }

    public static void closePipe(String name){
        lock.acquire();
        KernelPipe p = pipeMap.get(name);
        if(p!= null){
            p.openCount -= 1;
            if(p.openCount == 0){
                emptyPPN.add(p.ppn);
                pipeMap.remove(name);
            }
        }
        lock.release();
    }

    public static int processStart(){
        lock.acquire();
        runningProcess += 1;
        int r = runningProcess;
        lock.release();
        return r;
    }

    public static int processFinish(){
        lock.acquire();
        runningProcess -= 1;
        int r = runningProcess;
        lock.release();
        return r;
    }

    public static int newID(){
        lock.acquire();
        int r = nextID;
        nextID += 1;
        lock.release();
        return r;
    }

	/** Globally accessible reference to the synchronized console. */
	public static SynchConsole console;

	// dummy variables to make javac smarter
	private static Coff dummy1 = null;

    private static LinkedList<Integer> emptyPPN;

	private static final int maxPipes = 16;

    private static HashMap<String, KernelPipe> pipeMap;

    private static Lock lock;

    private static int runningProcess;

    private static int nextID;

	public static class KernelPipe {
		public KernelPipe(String name, int ppn) {
            name = name;
            this.ppn = ppn;
            lock = new Lock();
            empty = new Condition2(lock);
            full = new Condition2(lock);
            size = 0;
            openCount = 0;
		}
        public String name;
        public int ppn;
        public Condition2 empty;
        public Condition2 full;
        public Lock lock;
        public int size;
        private int openCount;
	};
}
