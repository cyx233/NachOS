/*
 * read1.c
 *
 * simple test for read
 */
#include "syscall.h"
#include "stdio.h"

int main(int argc, char** argv)
{
	char *filename1 = "/pipe/test";
	int file_descriptor = open(filename1);
    if(file_descriptor == -1)
        file_descriptor = creat(filename1);

    char *prog = "pipe_write.coff";
    exec (prog, 0, 0);


    char str[11] = "1234567890\n";
    int r = read (file_descriptor, str, 10);
    if (r == -1)
    {
        printf("failed to read character (r = %d)\n", r);
        exit(-1);
    }
    write(1, str, 10);

	return 0;
}
