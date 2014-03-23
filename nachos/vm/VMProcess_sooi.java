package nachos.vm;

import java.util.ArrayList;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess_sooi extends UserProcess {
	/**
	 * Allocate a new process.
	 */
    public VMProcess_sooi() {
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
		//invalidate tlb entries here
		//super.restoreState();
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
		return super.loadSections();
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
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionTLBMiss:
			handleTLBMiss(Machine.processor().readRegister(Processor.regBadVAddr));
			break;
		default:
			super.handleException(cause);
			break;
		}
	}

	private boolean load(String name, String[] args) {
		//decide the number of pages required for this process
		//identify pagenums of stack and args
		//what to do about the args if we are not loading the args page ???
		//read the coff sections and write it to swap
		//can also create a page for args and stack and write to swap
		
		return false;
	}
	
	
	public boolean execute(String name, String[] args) {
		load(null, null);
		return false;
	}
	private boolean invalidateTLB(TranslationEntry tle)
	{ 
		boolean updated=false;
	    int tlbsize= Machine.processor().getTLBSize();
		for(int i=0;i<tlbsize;i++)
     	{
     		TranslationEntry tlbentry=Machine.processor().readTLBEntry(i);
     		if(tlbentry.valid && (tle.ppn==tlbentry.ppn))
     		{
     			updated=true;
     			tlbentry.valid=false;
     			Machine.processor().writeTLBEntry(i, tlbentry);
     			break;
     		}
   	  	}
		return updated;
	}
	private boolean addTLBentry(TranslationEntry tle)
	{

		
		    boolean updated=false;
	        int tlbsize= Machine.processor().getTLBSize();
	     	//remove invalidated page table entries from TLB
	     	
	     	
	     	//check if tlb is full or not
	     	for(int j=0;j<tlbsize;j++)
	     	{
	     		TranslationEntry tlbentry=Machine.processor().readTLBEntry(j);
	     		if(tlbentry.valid == false)
	     		{
	     			updated=true;
	     			tle.valid=true;
	     			Machine.processor().writeTLBEntry(j, tle);
	     			break;
	     		}
	     	}
	     	if(updated)
	     	  return updated;
	     	else
	     	{
	     		int min_pos=VMKernel.getTLBReplacePosition();
	     		tle.valid=true;
	     		updated=true;
	     		Machine.processor().writeTLBEntry(min_pos, tle);
	     		VMKernel.setNewTLBEntry(min_pos);
	     	}	
	     	return updated;	     	
	     	
	     	
	}
	
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		// Invalid virtual address return 0 or if length to be read is 0
		if (vaddr < 0 || length == 0)
			return 0;
		
		byte[] memory = Machine.processor().getMemory();
		
		int noOfPagesToRead = length/pageSize;
		
		int vOffset = findvoffset(vaddr);
		
		int vpn = vaddrtovpn(vaddr);
		
		if(vOffset > 0)
			noOfPagesToRead++;
		
		int amountRead = 0;
		
		/* Reading pages one by one*/
		while(noOfPagesToRead > 0)
		{
			int paddr= VMKernel.getPPN(pid, vpn) + vOffset;
			
			if (paddr >= memory.length)
				return amountRead;
			
			int lengthToRead = Math.min(pageSize - vOffset, length - amountRead);
			
			System.arraycopy(memory, paddr, data, offset, lengthToRead);
			
			amountRead += lengthToRead;
			offset += lengthToRead;
			
			vOffset = 0;
			vpn++;
			
			noOfPagesToRead--;
		}

		return amountRead;
	}
	
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		// Invalid virtual address return 0 or if length to be read is 0
		if (vaddr < 0 || length == 0)
			return 0;
		
		byte[] memory = Machine.processor().getMemory();
		
		int noOfPagesToWrite = length/pageSize;
		
		int vOffset = findvoffset(vaddr);
		
		int vpn = vaddrtovpn(vaddr);
		
		if(vOffset > 0)
			noOfPagesToWrite++;
		
		int amountWritten = 0;
		
		/* Writing pages one by one*/
		while(noOfPagesToWrite > 0)
		{
			int paddr= VMKernel.getPPN(pid, vpn) + vOffset;
			
			/* Should also check for read only pages*/
			if (paddr >= memory.length)
				return amountWritten;
			
			int lengthToWrite = Math.min(pageSize - vOffset, length - amountWritten);
			
			System.arraycopy(data, offset, memory, paddr, lengthToWrite);
			
			amountWritten += lengthToWrite;
			offset += lengthToWrite;
			
			vOffset = 0;
			vpn++;
			
			noOfPagesToWrite--;
		}

		return amountWritten;
	}
	
	private int findvoffset(int vaddr)
    {
    	int voffset=vaddr % pageSize;
		return voffset;
    }
	
	private int vaddrtovpn(int vaddr)
	{
		int vpn=vaddr/pageSize;
		return vpn;
	}

	private void handleTLBMiss(int virtAddress){
		//get from pageTable
		//add it to tlb using a replacement policy   
	    //if tlb is full, insert using a replacement algorithm
	            	
	     	
	     		
	     	
	}
	
	private TranslationEntry getEntryFromPageTable(int virtAddr){
		//get the entry from inverted page table
		TranslationEntry entry = null;
		
		//case 1: PageTable hit
		//case 2: PageTable miss
		
		return entry;
	}
	
	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
	
	private int pid = -1;
	
}