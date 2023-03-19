/*
 * read2.c
 *
 * simple test for read, incorrect file descriptor
 */
#include "syscall.h"
#include "stdio.h"

int main(int argc, char** argv)
{
	char *filename1 = "temp.txt";
	int file_descriptor = open(filename1);
    char *str = "\nintroduction is good\nthreads is ok\nprocess and multiprogramming is unknown\nhave better performance next time\n\n";

	while (*str)
    {
        int r = read (1, str, 1);
        if (r == -1)
        {
            printf("wrong file descriptor error is caught by the return value of read()");
            exit(0);
        }
        str++;
    }

	return 0;
}
