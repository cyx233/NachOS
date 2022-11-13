package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.io.EOFException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
        childProcesses = new HashMap<Integer, UserProcess>();
        emptyFD = new LinkedList<Integer>();
        finished = false;
        for (int i=2; i<maxOpenFiles; i++)
            emptyFD.add(i);
        fileTable[0] = UserKernel.console.openForReading();
        fileTable[1] = UserKernel.console.openForWriting();
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
        String name = Machine.getProcessClassName ();

		// If Lib.constructObject is used, it quickly runs out
		// of file descriptors and throws an exception in
		// createClassLoader.  Hack around it by hard-coding
		// creating new processes of the appropriate type.

		if (name.equals ("nachos.userprog.UserProcess")) {
		    return new UserProcess ();
		} else if (name.equals ("nachos.vm.VMProcess")) {
		    return new VMProcess ();
		} else {
		    return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
		}
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
        if (!load(name, args)){
            finished = true;
            cleanMem();
			return false;
        }

		thread = new UThread(this);
		thread.setName(name).fork();
        int num = UserKernel.processStart();
        ID = UserKernel.newID();

		return true;
	}

	public void clean() {
        finished = true;
        cleanFiles();
        cleanMem();
	}

	public void cleanFiles() {
        Lib.debug(dbgProcess, "before clean, OpenFiles:"+UserKernel.fileSystem.getOpenCount());
        for(OpenFile f : fileTable)
            if(f != null)
                f.close();
        Lib.debug(dbgProcess, "after clean, OpenFiles:"+UserKernel.fileSystem.getOpenCount());
	}

	public void cleanMem() {
        Lib.debug(dbgProcess, "before clean, Empty PPN:"+UserKernel.getEmptyPPN());
        for(int i=0; i<pageTable.length; ++i)
            if(pageTable[i] != null){
                UserKernel.releasePPN(pageTable[i].ppn);
                pageTable[i] = null;
            }
        Lib.debug(dbgProcess, "after clean, Empty PPN:"+UserKernel.getEmptyPPN());
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}
        Lib.debug(dbgProcess, "Failed to load String");
        if(bytesRead>257)
            Lib.debug(dbgProcess, "Exceed maxLength = " + maxLength);
        else
            Lib.debug(dbgProcess, "String buffer load size:" + bytesRead);
		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
        if(length==0)
            return 0;
        int amount = 0;

        int vaOffset = vaddr % pageSize;
        if(vaOffset+length > pageSize){
            amount = readVirtualMemory(vaddr, data, offset, pageSize-vaOffset); 
            if(amount < pageSize - vaOffset)
                return amount;
        }

        int paddr = translate(vaddr+amount, false);
        if(paddr == -1)
            return amount;
		byte[] memory = Machine.processor().getMemory();
		System.arraycopy(memory, paddr, data, offset+amount, length-amount);
		return length;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
        if(length==0)
            return 0;
        int amount = 0;

        int vaOffset = vaddr % pageSize;
        if(vaOffset+length > pageSize){
            amount = writeVirtualMemory(vaddr, data, offset, pageSize-vaOffset); 
            if(amount < pageSize-vaOffset)
                return amount;
        }

        int paddr = translate(vaddr+amount, true);
        if(paddr == -1)
            return amount;
		byte[] memory = Machine.processor().getMemory();
        System.arraycopy(data, offset+amount, memory, paddr, length-amount);
		return length;
	}

    private int translate(int vaddr, boolean write){
        if(vaddr <= 0){
            Lib.debug(dbgProcess, "Access NULL pointer");
            return -1;
        }
        int vpn = vaddr / pageSize;
        int vaOffset = vaddr % pageSize;
        if (vpn>numPages || pageTable[vpn]==null || !pageTable[vpn].valid){
            Lib.debug(dbgProcess, "Access invalid VPN:"+vpn);
			return -1;
        }
        if (write && pageTable[vpn].readOnly){
            Lib.debug(dbgProcess, "RO VPN:"+vpn);
			return -1;
        }
        pageTable[vpn].used = true;
        if(write)
            pageTable[vpn].dirty = true;
        return pageTable[vpn].ppn*pageSize + vaOffset;
    }

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
		Lib.debug(dbgProcess, "args:"+String.join(", ", args));

		OpenFile executable = UserKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
                executable.close();
				return false;
			}
			numPages += section.getLength();
		}

        if (!loadSections()){
            executable.close();
			return false;
        }

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
            executable.close();
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
        for(int i=0; i<stackPages; ++i){
            int vpn = numPages + i;

            Integer ppn = UserKernel.getPPN();
            if(ppn==null){
                executable.close();
                return false;
            }
            pageTable[vpn] = new TranslationEntry(vpn, ppn, true, false, false, false);
        }
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
        Integer ppn = UserKernel.getPPN();
        if(ppn==null){
            executable.close();
            return false;
        }
        pageTable[numPages] = new TranslationEntry(numPages, ppn, true, false, false, false);
		numPages++;

        Lib.debug(dbgProcess, "Total pages:"+numPages);

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

        executable.close();
		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");
            
			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

                Integer ppn = UserKernel.getPPN();
                if(ppn==null)
                    return false;
                pageTable[vpn] = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false);
				section.loadPage(i, ppn);
			}
		}
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < Processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {
        if(ID != 0){
            Lib.debug(dbgProcess, "Not root, hault failed");
            return 0;
        }

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	private int handleExec(int filenamePointer, int argc, int argvP) {
        if(argc<0)
            return -1;
        String filename = readVirtualMemoryString(filenamePointer, maxArgLen);
        if(filename==null)
            return -1;
        UserProcess child = newUserProcess();
        byte[] argv = new byte[4*argc];
        String[] args = new String[argc];
        if(argc>0){
            if(readVirtualMemory(argvP, argv) < 4*argc)
                return -1;
            for(int i=0; i<argc; ++i){
                args[i] = readVirtualMemoryString(Lib.bytesToInt(argv, 4*i), maxArgLen);
                if(args[i]==null)
                    return -1;
            }
        }
        if(child.execute(filename, args)){
            childProcesses.put(child.ID, child);
            return child.ID;
        }
        else{
            Lib.debug(dbgProcess, "Exec "+filename+" failed, args:"+String.join(" ", args));
            return -1;
        }
	}

	private int handleJoin(int processID, int statusPointer) {
        UserProcess child = childProcesses.remove(processID);
        if(child == null)
            return -1;
        if(!child.finished)
            child.thread.join();
        if(child.exitCode == null)
            return 0;
        if(4 != writeVirtualMemory(statusPointer, Lib.bytesFromInt(child.exitCode)))
            return -1;
        else
            return 1;
	}
	private void exit() {
        clean();
        int num = UserKernel.processFinish();
        if(num>0)
            UThread.finish();
        else
            Kernel.kernel.terminate();
        Lib.assertNotReached("Exit failed");
	}

	/**
	 * Handle the exit() system call.
	 */
	private int handleExit(int status) {
        // Do not remove this call to the autoGrader...
		Machine.autoGrader().finishingCurrentProcess(status);
		// ...and leave it as the top of handleExit so that we
		// can grade your implementation.
        exitCode = status;
        clean();
		Lib.debug(dbgProcess, "UserProcess.handleExit (" + status + ")");
        exit();
        return 0;
	}

	private int openFile(int filenamePointer, boolean create) {
        String filename = readVirtualMemoryString(filenamePointer, maxArgLen);
        if(filename == null)
            return -1;
        Integer fd = null;
        OpenFile file = null;
        if(filename.substring(0,6).equals("/pipe/")){
            String pipename = filename.substring(6);
            Lib.debug(dbgProcess, "open pipe:"+pipename);
            UserKernel.KernelPipe p = null;
            if(create)
                p = UserKernel.createPipe(pipename);
            else
                p = UserKernel.openPipe(pipename);
            if(p == null)
                return -1;
            file = new Pipe(pipename, p);
            fd = emptyFD.poll();
        }
        else{
            Lib.debug(dbgProcess, "open file:"+filename);
            file = UserKernel.fileSystem.open(filename, create);
            if(file == null){
                Lib.debug(dbgProcess, "File system error. Failed to open files.");
                return -1;
            }
            fd = emptyFD.poll();
        }
        if(fd == null){
            if(filename.substring(0,6).equals("/pipe/"))
                Lib.debug(dbgProcess, "Exceed maxOpenFiles="+maxOpenFiles);
            else
                Lib.debug(dbgProcess, "Exceed maxPipes");
            return -1;
        }
        fileTable[fd] = file;
        return fd;
    }

	private int handleCreate(int filenamePointer) {
        return openFile(filenamePointer, true);
	}

	private int handleOpen(int filenamePointer) {
        return openFile(filenamePointer, false);
	}

	private int handleRead(int fd, int bufferPointer, int count) {
        if(count<0 || fd<0 || fd>=maxOpenFiles)
            return -1;
        OpenFile file = fileTable[fd];
        if(file == null)
            return -1;
        int amount = 0;
        while(count > amount){
            int length = Math.min(buffer.length, count-amount);
            int new_read = file.read(buffer, 0, length);
            switch(new_read){
            case -1:
                return -1;
            case 0:
                return amount;
            default:
                if(new_read > writeVirtualMemory(bufferPointer+amount, buffer, 0, length))
                    return -1;
                amount += new_read;
            }
        }
        return amount;
	}

	private int handleWrite(int fd, int bufferPointer, int count) {
        if(count<0 || fd<0 || fd>=maxOpenFiles)
            return -1;
        OpenFile file = fileTable[fd];
        if(file == null)
            return -1;
        int amount = 0;
        while(count > amount){
            int length = Math.min(buffer.length, count-amount);
            int new_read = readVirtualMemory(bufferPointer+amount, buffer, 0, length);
            if(new_read<length || new_read>file.write(buffer, 0, length))
                return -1;
            amount += new_read;
        }
        if(amount == count)
            return amount;
        return -1;
	}

	private int handleClose(int fd) {
        if(fd<0 || fd>=maxOpenFiles || fileTable[fd] == null){
            Lib.debug(dbgProcess, "invalid fd:" + fd);
            return -1;
        }
        fileTable[fd].close();
        fileTable[fd] = null;
        emptyFD.add(fd);
        Lib.debug(dbgProcess, "close file:"+fd);
		return 0;
	}

	private int handleUnlink(int filenamePointer) {
        String filename = readVirtualMemoryString(filenamePointer, maxArgLen);
        if(filename != null && UserKernel.fileSystem.remove(filename))
            return 0;
        return -1;
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
        case syscallExec:
			return handleExec(a0, a1, a2);
        case syscallJoin:
			return handleJoin(a0, a1);
		case syscallExit:
			return handleExit(a0);
        case syscallCreate:
            return handleCreate(a0);
        case syscallOpen:
            return handleOpen(a0);
        case syscallRead:
            return handleRead(a0, a1, a2);
        case syscallWrite:
            return handleWrite(a0, a1, a2);
        case syscallClose:
            return handleClose(a0);
        case syscallUnlink:
            return handleUnlink(a0);
		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
            return -1;
		}
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;
		case Processor.exceptionPageFault:
			Lib.debug(dbgProcess,"Page Fault:" + processor.readRegister(Processor.regBadVAddr));
            exit();
            break;
		default:
			Lib.debug(dbgProcess, "Unexpected exception: " + Processor.exceptionNames[cause]);
            exit();
		}
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	/** The thread that executes the user-level program. */
    protected UThread thread;
    
	private int initialPC, initialSP;

	private int argc, argv;

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final int maxArgLen = 256;

	private static final int maxOpenFiles = 16;

    private OpenFile[] fileTable = new OpenFile[maxOpenFiles];

    private LinkedList<Integer> emptyFD;

    private byte[] buffer = new byte[pageSize]; 

    private Integer exitCode;

    private boolean finished;

    private HashMap<Integer, UserProcess> childProcesses;

    private int ID;

	private class Pipe extends OpenFile {
		public Pipe(String name, UserKernel.KernelPipe p) {
			super(null, name);
            this.p = p;
            begin = p.ppn*pageSize;
		}

		public void close() {
            UserKernel.closePipe(getName());
		}

		public int read(byte[] buf, int offset, int length) {
            p.lock.acquire();
            while(p.size == 0){
                p.full.wakeAll();
                p.empty.sleep();
            }

            byte[] memory = Machine.processor().getMemory();
            System.arraycopy(memory, begin, buf, offset, length);
            p.size -= length; 
            System.arraycopy(memory, length, memory, begin, p.size);

            p.full.wakeAll();
            p.lock.release();
            return length;
		}

		public int write(byte[] buf, int offset, int length) {
            p.lock.acquire();
            int amount = 0;
            byte[] memory = Machine.processor().getMemory();

            while(amount < length){
                while(p.size == pageSize){
                    p.empty.wakeAll();
                    p.full.sleep();
                }
                int writeLen = Math.min(length-amount, pageSize - p.size);
                System.arraycopy(buf, offset+amount, memory, begin+p.size, writeLen);
                p.size += writeLen; 
                amount += writeLen;
            }
            p.empty.wakeAll();
            p.lock.release();
            return length;
		}
        UserKernel.KernelPipe p;
        int begin;
	};
}
