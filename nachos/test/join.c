/*
 * join1.c
 *
 * Simple program for testing join.  After exec-ing the child, it
 * waits for the child to exit.
 *
 * Geoff Voelker
 * 11/9/15
 */

#include "syscall.h"
#include "stdlib.h"

int main (int argc, char *argv[])
{
    if(argc != 1)
        return -1;
    char *prog = "exit1.coff";
    int r, status = 0;
    int pid = atoi(argv[0]);

    printf ("joining %d...\n", pid);
    r = join (pid, &status);
    if (r > 0) {
	printf ("...passed (status from child = %d)\n", status);
    } else if (r == 0) {
	printf ("...child exited with unhandled exception\n");
    } else {
	printf ("...failed (r = %d)\n", r);
    }

    // the return value from main is used as the status to exit
    return r;
}
