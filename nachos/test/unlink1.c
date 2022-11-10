/*
 * unlink1.c
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
    if (r == -1)
    {
        printf("error occurred when calling unlink");
        exit(-1);
    }

    return 0;
}
