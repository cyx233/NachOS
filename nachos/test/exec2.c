#include "syscall.h"

int
main (int argc, char *argv[])
{
    char *prog = "exit1.coff";
    int pid1, pid2;

    pid1 = exec (prog, 0, 0);
    if (pid1 < 0) {
	exit (-1);
    }
    pid2 = exec (prog, 0, 0);
    if (pid2 < 0) {
	exit (-2);
    }
    if (pid2 == pid1) {
    printf("The same PID!");
    exit(-3);
    }

    exit (0);
}
