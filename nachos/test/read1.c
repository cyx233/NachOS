/*
 * read1.c
 *
 * simple test for read
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
        int r = read (0, str, 1);
        if (r != 1)
        {
            printf("failed to read character (r = %d)\n", r);
            exit(-1);
        }
        str++;
    }

	return 0;
}
