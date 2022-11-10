/*
 * unlink2.c
 *
 * simple test for unlink
 */
#include "syscall.h"
#include "stdio.h"

int main(int argc, char** argv)
{
	char *filename1 = "temp_toberemoved.txt";
	int r = creat(filename1);
    r = unlink(filename1);
    r = open(filename1);
    if (r != -1)
    {
        printf("error occurred when openning an unlinked file, open() returns %d, which should be -1", r);
        exit(-1);
    }

    return 0;
}
