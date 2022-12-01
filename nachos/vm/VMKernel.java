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
        lock = new Lock();
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
        TranslationEntry[] pt = process.getPageTable();
        for(int i=0; i<pt.length; ++i)
            globalPageTable.add(pt[i]);
		KThread.finish();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
        swapFile.close();
        fileSystem.remove("global_swap");
		super.terminate();
	}

    public static TranslationEntry getSwapPage(){
        lock.acquire();
        TranslationEntry target = null;
        for(int i=0; i<globalPageTable.size(); i++){
            TranslationEntry e = globalPageTable.get(i);
            if(e!=null && e.valid && !pinedPage.contains(e))
                if(e.used)
                    e.used = false;
                else{
                    target = e;
                    break;
                }
        }
        if(target != null){
            lock.release();
            return target;
        }

        for(int i=0; i<globalPageTable.size(); i++){
            TranslationEntry e = globalPageTable.get(i);
            if(e!=null && e.valid && !pinedPage.contains(e) && !e.used){
                target = e;
                break;
            }
        }
        lock.release();
        Lib.assertTrue(target!=null);
        return target;
    }
    
    public static void evictPage(TranslationEntry e){
        lock.acquire();
        e.valid = false;
        if(!e.dirty && (e.readOnly || swapTable.containsKey(e.ppn))){
            lock.release();
            return;
        }
        Integer start = swapTable.get(e);
        if(start==null){
            start = swapPointer;
            swapPointer += 1;
        }
        int r = swapFile.write(start*pageSize,Machine.processor().getMemory(),e.ppn*pageSize,pageSize);
        Lib.assertTrue(r!=-1);
        swapTable.put(e, start);
        lock.release();
    }
    
    public static boolean restorePage(TranslationEntry e){
        lock.acquire();
        Integer start = swapTable.get(e);
        byte[] memory = Machine.processor().getMemory();
        if(start == null)
            Arrays.fill(memory, e.ppn*pageSize, (e.ppn+1)*pageSize, (byte)0);
        else
            swapFile.read(start*pageSize, memory, e.ppn*pageSize, pageSize);
        e.used = false;
        e.dirty = false;
        lock.release();
        return start!=null;
    }
    
    public static void pinPage(TranslationEntry e){
        lock.acquire();
        pinedPage.add(e);
        lock.release();
    }

    public static void unPinPage(TranslationEntry e){
        lock.acquire();
        pinedPage.remove(e);
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

    private static Lock lock;
}
