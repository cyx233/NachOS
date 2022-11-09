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
//	printf("managed to open");
    char *str = "introduction is good, threads is ok, process and multiprogramming is unknown, have better performance next time\n";

	while (*str)
    {
        int r = read (file_descriptor, str, 1);
	write(1, str, 1);
        if (r == -1)
        {
            printf("failed to read character (r = %d)\n", r);
            exit(-1);
        }
        str++;
    }

	return 0;
}
