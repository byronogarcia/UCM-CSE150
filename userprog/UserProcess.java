package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.*;

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
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
        for (int i=0; i<numPhysPages; i++)
            pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
        files[0] = UserKernel.console.openForReading();
        files[1] = UserKernel.console.openForWriting();
        random_PID++;
        finished = new Semaphore(0);
        total_processes.put(current_PID, this);
        for(int i = 2; i < files.length; i++) {
            files[i] = null; //initialize to null
        }
    }

    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return  a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
        return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param   name    the name of the file containing the executable.
     * @param   args    the arguments to pass to the executable.
     * @return  <tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        if (!load(name, args))
            return false;

        new UThread(this).setName(name).fork();

        return true;
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
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param   vaddr   the starting virtual address of the null-terminated
     *          string.
     * @param   maxLength   the maximum number of characters in the string,
     *              not including the null terminator.
     * @return  the string read, or <tt>null</tt> if no null terminator was
     *      found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength+1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length=0; length<bytesRead; length++) {
            if (bytes[length] == 0)
                return new String(bytes, 0, length);
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param   vaddr   the first byte of virtual memory to read.
     * @param   data    the array where the data will be stored.
     * @return  the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
        return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param   vaddr   the first byte of virtual memory to read.
     * @param   data    the array where the data will be stored.
     * @param   offset  the first byte to write in the array.
     * @param   length  the number of bytes to transfer from virtual memory to
     *          the array.
     * @return  the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
//         Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

//         byte[] memory = Machine.processor().getMemory();

//     // for now, just assume that virtual addresses equal physical addresses
//         if (vaddr < 0 || vaddr >= memory.length)
//             return 0;

//         int amount = Math.min(length, memory.length-vaddr);
//         System.arraycopy(memory, vaddr, data, offset, amount);

//         return amount;
	    
	    	Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		int firstVPN = Processor.pageFromAddress(vaddr);
		int firstOffset = Processor.offsetFromAddress(vaddr);
		int lastVPN = Processor.pageFromAddress(vaddr + length);

		TranslationEntry entry = getTranslationEntry(firstVPN, false);

		if (entry == null)
			return 0;

		int amount = Math.min(length, pageSize - firstOffset);
		System.arraycopy(memory, Processor.makeAddress(entry.ppn, firstOffset), data, offset, amount);
		offset += amount;

		for (int i = firstVPN + 1; i <= lastVPN; i++) {
			entry = getTranslationEntry(i, false);
			if (entry == null)
				return amount;
			int len = Math.min(length - amount, pageSize);
			System.arraycopy(memory, Processor.makeAddress(entry.ppn, 0), data, offset, len);
			offset += len;
			amount += len;
		}

		return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param   vaddr   the first byte of virtual memory to write.
     * @param   data    the array containing the data to transfer.
     * @return  the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param   vaddr   the first byte of virtual memory to write.
     * @param   data    the array containing the data to transfer.
     * @param   offset  the first byte to transfer from the array.
     * @param   length  the number of bytes to transfer from the array to
     *          virtual memory.
     * @return  the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
        int length) {
//         Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

//         byte[] memory = Machine.processor().getMemory();

//     // for now, just assume that virtual addresses equal physical addresses
//         if (vaddr < 0 || vaddr >= memory.length)
//             return 0;

//         int amount = Math.min(length, memory.length-vaddr);
//         System.arraycopy(data, offset, memory, vaddr, amount);

//         return amount;
	    
	    Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		int firstVPN = Processor.pageFromAddress(vaddr);
		int firstOffset = Processor.offsetFromAddress(vaddr);
		int lastVPN = Processor.pageFromAddress(vaddr + length);

		TranslationEntry entry = getTranslationEntry(firstVPN, true);

		if (entry == null)
			return 0;

		int amount = Math.min(length, pageSize - firstOffset);
		System.arraycopy(data, offset, memory, Processor.makeAddress(entry.ppn, firstOffset), amount);
		offset += amount;

		for (int i = firstVPN + 1; i <= lastVPN; i++) {
			entry = getTranslationEntry(i, true);
			if (entry == null)
				return amount;
			int len = Math.min(length - amount, pageSize);
			System.arraycopy(data, offset, memory, Processor.makeAddress(entry.ppn, 0), len);
			offset += len;
			amount += len;
		}

		return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param   name    the name of the file containing the executable.
     * @param   args    the arguments to pass to the executable.
     * @return  <tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
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
        for (int s=0; s<coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

    // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i=0; i<args.length; i++) {
            argv[i] = args[i].getBytes();
        // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

    // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();   

    // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages*pageSize;

    // and finally reserve 1 page for arguments
        numPages++;

        if (!loadSections())
            return false;

    // store arguments in last page
        int entryOffset = (numPages-1)*pageSize;
        int stringOffset = entryOffset + args.length*4;

        this.argc = args.length;
        this.argv = entryOffset;

        for (int i=0; i<argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
                argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return  <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        if (numPages > Machine.processor().getNumPhysPages()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }

    // load sections
        for (int s=0; s<coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                + " section (" + section.getLength() + " pages)");

            for (int i=0; i<section.getLength(); i++) {
                int vpn = section.getFirstVPN()+i;

        // for now, just assume virtual addresses=physical addresses
                section.loadPage(i, vpn);
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
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

    // by default, everything's 0
        for (int i=0; i<processor.numUserRegisters; i++)
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

        Machine.halt();

        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }

    /**
     * Handle the exit() system call. 
     */
    private void handleExit(int a0) {
    	for(int i = 0; i < files.length; i++) {
            if(files[i] != null){
                files[i].close();
                files[i] = null;
            }
        }
        for(int j = 0; j < parents_child.size(); j++){
            parents_child.get(j).parent.current_PID = -1;
        }
    	exit_status = a0;
        unloadSections();
        finished.V();
        total_processes.remove(a0);
        if(total_processes.isEmpty()){
            Kernel.kernel.terminate();
        }
        	UThread.currentThread().finish();
    }

    /**
     * Handle the exec() system call. 
     */
    private int handleExec(int a0, int a1, int a2) {
    	String file = readVirtualMemoryString(a0, max);
    	if(a1 < 0 || file == null || !file.endsWith(".coff")) {
    		return -1;
    	}	
    	String[] child_argv = new String[a1];
    	byte[] data = new byte[4];
    	for(int i = 0; i < a1; i++) {
            int temp = readVirtualMemory(a2 + i * 4, data);
                if(temp != 4){
                    return -1;
                }
    			int addresses = Lib.bytesToInt(data, 0);
                child_argv[i] = readVirtualMemoryString(addresses, max);
        }

    	UserProcess child = newUserProcess(); 	// make new userprocess call child
    	child.current_PID = random_PID;			//assign random pid to it
    	parents_child.put(child.current_PID, child);			//add child to this process's child linked list
    	
    	if(!child.execute(file, child_argv)) {
            parents_child.remove(child.current_PID);
            child.exit_status = -1;
            return -1;
        }
	    return child.current_PID;
    }
    
    /**
     * Handle the join() system call. 
     */
    private int handleJoin(int a0, int a1) {
        UserProcess temp = null;
        if(!parents_child.containsKey(a0)){
            return -1;
        }
        temp = parents_child.get(a0);
        temp.finished.P();
        int tempstatus = temp.exit_status;
        byte status[] = new byte[4];
        status = Lib.bytesFromInt(temp.exit_status);
        int current_status = writeVirtualMemory(a1, status, 0, 4);
        parents_child.remove(a0);

        if(current_status != 4){
            return -1;
        }
        if(tempstatus != -1){
            return 1;
        }
        return 0;
    }
    
    /**
     * Handle the creat() system call. 
     */
    private int handleCreat(int a0) {
        if(a0 < 0) {
            return -1;
        }
        String name = readVirtualMemoryString(a0, 256);
        for(OpenFile file: files) {
            if(file != null && file.getName().equals(name)) {
                return -1;
            }
        }
        OpenFile toAdd = ThreadedKernel.fileSystem.open(name, true);
        if(toAdd == null) {
            return -1;
        }
        System.out.println("at creat, "+toAdd.getName());
        for(int i = 2; i < files.length; i++) {
            if(files[i] == null) {
                files[i] = toAdd;
                return i;
            }
        }

        return -1;
    }
    
    /**
     * Handle the open() system call. 
     */
    private int handleOpen(int a0) {
        if(a0 < 0) {
            return -1;
        }
        String name = readVirtualMemoryString(a0, 256);
        // for(OpenFile file: files) {
        //     if(file != null && file.getName().equals(name)) {
        //         return -1;
        //     }
        // }
        OpenFile toAdd = ThreadedKernel.fileSystem.open(name, false);
        if(toAdd == null) {
            return -1;
        }
        System.out.println("at open, "+toAdd.getName());
        for(int i = 2; i < files.length; i++) {
            if(files[i] == null) {
                files[i] = toAdd;
                return i;
            }
        }

        return -1;
    }
    
    /**
     * Handle the read() system call. 
     */
    private int handleRead(int a0, int a1, int a2) {
        byte[] buf = new byte[a2];
        int numread;
        System.out.println("at read");
        if(a0 < 0 || a0 >= files.length) {
            return -1;
        }
        numread = files[a0].read(buf, 0, a2);
        writeVirtualMemory(a1, buf);
        return numread;
    }
    
    /**
     * Handle the write() system call. 
     */
    private int handleWrite(int a0, int a1, int a2) {
        byte[] buf = new byte[a2];
        System.out.println("at write");
        if(a0 < 0 || a0 >= files.length) {
            return -1;
        }
        if(readVirtualMemory(a1, buf) == -1) {
            return -1;
        }
        return files[a0].write(buf, 0, a2);
    }
    
    /**
     * Handle the close() system call. 
     */
    private int handleClose(int a0) {
        System.out.println("at close, " +files[a0].getName());
        if(a0 < 0 || a0 >= files.length) {
            return -1;
        }
        files[a0].close();
        files[a0] = null;
        return 0;
    }
    
    /**
     * Handle the unlink() system call. 
     */
    private int handleUnlink(int a0) {
        if(a0 < 0) {
            return -1;
        }
        String name = readVirtualMemoryString(a0, 256);
        System.out.println("at unlink, " + name);
        int ind = -1;
        for(int i = 2; i < files.length; i++) {
            if(files[i] != null && files[i].getName().equals(name)) {
                ind = i;
            }
        }
        ThreadedKernel.fileSystem.remove(name);
        if(ind == -1) { //nothing to close, unlink done.
            return 0;
        }
        handleClose(ind);
        return 0;
    }


    private static final int
        syscallHalt = 0,
        syscallExit = 1,
        syscallExec = 2,
        syscallJoin = 3,
        syscallCreate = 4,
        syscallOpen = 5,
        syscallRead = 6,
        syscallWrite = 7,
        syscallClose = 8,
        syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     *                              </tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *                              </tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *                              </tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param   syscall the syscall number.
     * @param   a0  the first syscall argument.
     * @param   a1  the second syscall argument.
     * @param   a2  the third syscall argument.
     * @param   a3  the fourth syscall argument.
     * @return  the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        System.out.println(Integer.toString(a0) + " - a0, "+Integer.toString(a1) + " - a1, "+Integer.toString(a2) + " - a2, "+Integer.toString(a3) + " - a3");
        switch (syscall) {
            case syscallHalt:
                if(id == 1) {
                    return handleHalt();
                } return -1;
            case syscallExit:
                handleExit(a0);
            case syscallExec:
                return handleExec(a0, a1, a2);
            case syscallJoin:
                return handleJoin(a0, a1);
            case syscallCreate:
                return handleCreat(a0);
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
                System.out.println(Integer.toString(a0) + " - a0, "+Integer.toString(a1) + " - a1");
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param   cause   the user exception that occurred.
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

            default:
                Lib.debug(dbgProcess, "Unexpected exception: " +
                    Processor.exceptionNames[cause]);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    protected OpenFile[] files = new OpenFile[16];

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;

    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static int numproc = 0;
    private int id = numproc++;
    
    private int current_PID;
    private UserProcess parent;
    private int random_PID;
    private int exit_status;
    private static final int max = 256;
    private Hashtable<Integer,UserProcess> parents_child = new Hashtable<Integer, UserProcess>();
    private Hashtable<Integer,UserProcess> total_processes = new Hashtable<Integer, UserProcess>();
    protected Semaphore finished;
    private KThread joinThread;
}

