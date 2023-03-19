# Team Work
## Group Member
|Name|PID|Email|
|-|-|-|
|Yuxiang Chen|A59016369|yuc129@ucsd.edu|
|Jiale Xu|A15123298|jix012@ucsd.edu|
## Todo - Yuxiang Chen
- [x] task1
- [x] task2
- [x] task3
## Testing - Yuxiang Chen, Jiale Xu
- [] task1 - Jiale Xu
- [x] task2 - Yuxiang Chen
- [x] task3.- Yuxiang Chen
## Implementation
### task1
```java
private OpenFile[] fileTable = new OpenFile[maxOpenFiles];
private LinkedList<Integer> emptyFD;
```
Use a Array to save opened files. Use a LinkedList to manage file descriptors.

At `handleCreate` and `handleOpen`, poll a new file descriptor from `emptyFD`.
At `handleClose`, add the closed file descriptor to `emptyFD`.

```java
private byte[] buffer = new byte[pageSize];
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
```
buffer size = `pageSize`. When read/write across 2 pages, it will delivery the data in 2 steps.  
`translate` is a function for virtual address translation.

### task2
```java
//UserKernel.java
private static LinkedList<Integer> emptyPPN;
```
Use a LinkedList to manage physical pages. `getPPN()` will poll a page from `emptyPPN`, `releasePPN(int ppn)` will
add `ppn` to `emptyPPN`.

### task3
Process ID can be found in KThread. In `handleExec` ID of the new child process will be returned.

```java
//UserKernel.java
private static int nextID;
private static int runningProcess;
```
When a process successfully calls `execute`, it will call `UserKernel.nextID` and get its ID. In `handleExec`, the child ID will be put into the `status` pointer.

When a process successfully calls `execute`, it will call `UserKernel.processStart` and increase `runningProcess`. 
When it calls `exit`, it will call `UserKernel.processFinish` and decrease `runningProcess`. 
The return value of  `processStart` and `processFinish` is the number of exist running processes. The `root` process will get `1` in `execute`. The last process will get `0` in `exit`. This return value will influence the behavior of `handleExit` and `handleHalt`.

```java
private HashMap<Integer, UserProcess> childProcesses;
private boolean finished;
private Integer exitCode;
```
When a process successfully calls `execute`, it will set `finished` to `false`.
In `exit`, a process calls `clean` and sets `finished` to `true`. In `handleExit`, a process will do one more thing: assign the `exitCode` with `status`. 

If `exitCode` is `null` and `finished` is `true`, it shows that this process exit abnormally. These 2 values are considered in `handleJoin`.
