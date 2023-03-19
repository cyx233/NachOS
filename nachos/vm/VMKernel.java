package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
        globalPageTable = new ArrayList<TranslationEntry>();
        pinedPage = new HashSet<TranslationEntry>();
        swapFile = fileSystem.open("global_swap", true);
        swapTable = new HashMap<TranslationEntry, Integer>();
        hasUnPinedPage = new Condition2(lock);
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		VMProcess process = (VMProcess)UserProcess.newUserProcess();

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
    public static void addPageTable(TranslationEntry[] pt){
        for(TranslationEntry e:pt)
            globalPageTable.add(e);
    }

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
        swapFile.close();
        fileSystem.remove("global_swap");
		super.terminate();
	}

    public static int getSwapPage(){
        lock.acquire();

        while(pinedPage.size() >= Machine.processor().getNumPhysPages())
            hasUnPinedPage.sleep();

        TranslationEntry target = null;
        while(target == null){
            if(clockPointer == globalPageTable.size())
                clockPointer = 0;
            TranslationEntry e = globalPageTable.get(clockPointer);
            if(e!=null && e.valid && !pinedPage.contains(e)){
                if(e.used)
                    e.used = false;
                else
                    target = e;
            }
            clockPointer += 1;
        }

        //evict page
        target.valid = false;
        if(!target.dirty && (target.readOnly || swapTable.containsKey(target.ppn))){
            lock.release();
            return target.ppn;
        }
        Integer start = swapTable.get(target);
        if(start==null){
            start = swapPointer;
            swapPointer += 1;
            // Lib.debug('a', ""+swapPointer+" pin:"+KThread.currentThread().getName());
        }
        byte[] memory = Machine.processor().getMemory();
        int r = swapFile.write(start*pageSize, memory, target.ppn*pageSize, pageSize);
        Lib.assertTrue(r!=-1);
        swapTable.put(target, start);
        lock.release();

        return target.ppn;
    }
    
    public static boolean restorePage(TranslationEntry e){
        lock.acquire();
        Integer start = swapTable.get(e);
        byte[] memory = Machine.processor().getMemory();
        if(start == null)
            Arrays.fill(memory, e.ppn*pageSize, (e.ppn+1)*pageSize, (byte)0);
        else{
            int r = swapFile.read(start*pageSize, memory, e.ppn*pageSize, pageSize);
            Lib.assertTrue(r!=-1);
        }
        e.used = false;
        e.dirty = false;
        lock.release();
        return start!=null;
    }
    
    public static void pinPage(TranslationEntry e){
        lock.acquire();
        // Lib.debug('a', ""+e.ppn+" pin:"+KThread.currentThread().getName());
        pinedPage.add(e);
        lock.release();
    }

    public static void unPinPage(TranslationEntry e){
        lock.acquire();
        // Lib.debug('a', ""+e.ppn+" unpin:"+KThread.currentThread().getName());
        pinedPage.remove(e);
        hasUnPinedPage.wake();
        lock.release();
    }

    private static ArrayList<TranslationEntry> globalPageTable;

    private static HashSet<TranslationEntry> pinedPage;

    private static OpenFile swapFile;

    private static int swapPointer = 0;

    private static HashMap<TranslationEntry, Integer> swapTable;

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';

	private static final int pageSize = Processor.pageSize;

    private static Condition2 hasUnPinedPage;

    private static int clockPointer;
}
