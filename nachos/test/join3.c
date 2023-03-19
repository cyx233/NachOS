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

    char *prog2 = "join.coff";
    char* temp = "1";
    char* args[1] = {temp};
    int pid2 = exec (prog2, 1, args);

    join(pid2,&status);
    if(status>0){
        printf ("...failed. child %d join child %d...\n", pid2, pid);
        return -1;
    }
    else if(status==0){
        printf("...failed. child %d exit abnormally\n", pid);
        return -1;
    }
    else if(status==-1){
        printf ("...pass. child %d failed to join child %d (status = %d)...\n", pid2, pid, status);
    }

    // the return value from main is used as the status to exit
    return 0;
}
