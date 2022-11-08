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

int
main (int argc, char *argv[])
{
    char *prog = "exit1.coff";
    int pid, r, status = 0;

    printf ("execing %s...\n", prog);
    pid = exec (prog, 0, 0);
    if (pid > 0) {
	printf ("...passed\n");
    } else {
	printf ("...failed (pid = %d)\n", pid); 
	exit (-1);
    }

    printf ("child %d. try to join %d...\n", pid, pid+1);
    r = join (pid+1, &status);
    if (r > 0) {
	printf ("...failed. join a unexisted child\n", pid+1);
    return -1;
    } else if (r == 0) {
	printf ("...failed. join a unexisted child\n", pid+1);
    return -1;
    } else {
	printf ("...pass. (r = %d) when join the unexisted child %d\n", r, pid+1);
    }

    // the return value from main is used as the status to exit
    return 0;
}
