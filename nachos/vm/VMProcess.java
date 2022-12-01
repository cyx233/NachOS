package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.Arrays;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		super.saveState();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		super.restoreState();
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");
            
			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;
                pageTable[vpn] = new TranslationEntry(vpn, 0, false, section.isReadOnly(), false, false);
			}
		}

        // stack
        for(int i=0; i<stackPages; ++i){
            int vpn = numPages - 1 - stackPages + i;
            pageTable[vpn] = new TranslationEntry(vpn, 0, false, false, false, false);
        }

        //args
        pageTable[numPages-1] = new TranslationEntry(numPages-1, 0, false, false, false, false);
		return true;

	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		super.unloadSections();
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		switch (cause) {
            case Processor.exceptionPageFault:
                int vaddr = Machine.processor().readRegister(Processor.regBadVAddr);
                int vpn = vaddr/pageSize;
                loadPage(vpn);
                break;
            default:
                super.handleException(cause);
                break;
		}
	}

    private boolean loadPage(int vpn){
        if(vpn<0 || vpn >=numPages){
            Lib.debug(dbgProcess,"VPN out of space:"+vpn);
            return false;
        }
        byte[] memory = Machine.processor().getMemory();
        Integer ppn = UserKernel.getPPN();
        if(ppn==null){
            TranslationEntry e = VMKernel.getSwapPage();
            VMKernel.evictPage(e);
            ppn = e.ppn;
        }
        pageTable[vpn].ppn = ppn;
        pageTable[vpn].valid = true;

        VMKernel.pinPage(pageTable[vpn]);
        if(!VMKernel.restorePage(pageTable[vpn]) && vpn<numPages-stackPages-1)
        {
            //coff
            for (int s = 0; s < coff.getNumSections(); s++) {
                CoffSection section = coff.getSection(s);

                int start = section.getFirstVPN();
                if(vpn-start < section.getLength()){
                    section.loadPage(vpn-start, ppn);
                    break;
                }
            }
        }
        VMKernel.unPinPage(pageTable[vpn]);

        return true;
    }

    protected int translate(int vaddr, boolean write){
        int vpn = vaddr / pageSize;
        int vaOffset = vaddr % pageSize;
        if (!pageTable[vpn].valid && !loadPage(vpn)){
            Lib.debug(dbgProcess, "Cannot load page. VPN:"+vpn);
            return -1;
        }
        if (write && pageTable[vpn].readOnly){
            Lib.debug(dbgProcess, "RO VPN:"+vpn);
			return -1;
        }
        return pageTable[vpn].ppn*pageSize + vaOffset;
    }

    public TranslationEntry[] getPageTable(){
        return pageTable;
    }

	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
        if(vaddr <= 0){
            Lib.debug(dbgProcess, "Access NULL pointer");
            return -1;
        }

        if(length==0)
            return 0;

        int amount = 0;
        int vaOffset = vaddr % pageSize;
        if(vaOffset+length > pageSize){
            amount = readVirtualMemory(vaddr, data, offset, pageSize-vaOffset); 
            if(amount < pageSize - vaOffset)
                return amount;
        }

        int vpn = (vaddr+amount) / pageSize;
        if (vpn>=numPages){
            Lib.debug(dbgProcess, "VPN is out of space:"+vpn);
			return amount;
        }
        VMKernel.pinPage(pageTable[vpn]);

        int paddr = translate(vaddr+amount, false);
        if(paddr == -1)
            return amount;
		byte[] memory = Machine.processor().getMemory();
		System.arraycopy(memory, paddr, data, offset+amount, length-amount);
        pageTable[vpn].used = true;

        VMKernel.unPinPage(pageTable[vpn]);
		return length;
	}

	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
        if(vaddr <= 0){
            Lib.debug(dbgProcess, "Access NULL pointer");
            return -1;
        }

        if(length==0)
            return 0;

        int amount = 0;
        int vaOffset = vaddr % pageSize;
        if(vaOffset+length > pageSize){
            amount = writeVirtualMemory(vaddr, data, offset, pageSize-vaOffset); 
            if(amount < pageSize-vaOffset)
                return amount;
        }

        int vpn = (vaddr+amount) / pageSize;
        if (vpn>=numPages){
            Lib.debug(dbgProcess, "VPN is out of space:"+vpn);
			return amount;
        }
        VMKernel.pinPage(pageTable[vpn]);

        int paddr = translate(vaddr+amount, true);
        if(paddr == -1)
            return amount;
		byte[] memory = Machine.processor().getMemory();
        System.arraycopy(data, offset+amount, memory, paddr, length-amount);
        pageTable[vpn].used = true;
        pageTable[vpn].dirty = true;

        VMKernel.unPinPage(pageTable[vpn]);

		return length;
	}

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
