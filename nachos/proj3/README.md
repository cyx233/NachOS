# Team Work
## Group Member
|Name|PID|Email|
|-|-|-|
|Yuxiang Chen|A59016369|yuc129@ucsd.edu|
|Jiale Xu|A15123298|jix012@ucsd.edu|
## Todo - Yuxiang Chen
- [x] task1
- [x] task2
## Testing - Use Proj2
## Implement
```java
    //for chosing target swap page
    private static int clockPointer;
    private static ArrayList<TranslationEntry> globalPageTable;
    private static HashSet<TranslationEntry> pinedPage;

    //for evicting page into a global file
    private static OpenFile swapFile;
    private static int swapPointer = 0;
    private static HashMap<TranslationEntry, Integer> swapTable; //key: pageTable, value: swap slots
    
    //when there are too many pined pages, the thread will wait on this condition vairable.
    private static Condition2 hasUnPinedPage;

	  // dummy variables to make javac smarter
	  private static VMProcess dummy1 = null;

	  private static final char dbgVM = 'v';

	  private static final int pageSize = Processor.pageSize;

    

    
```
