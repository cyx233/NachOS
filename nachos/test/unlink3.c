/*
 * unlink3.c
 *
 * simple test for unlink, test case for handling invalid filename argument
 */
#include "syscall.h"
#include "stdio.h"

int main(int argc, char** argv)
{
	char *filename1 = "some_randomfilename.txt";
    int r = unlink(filename1);
    if (r != -1)
    {
        printf("error occurred when unlinking a non-exist file, unlink() returns %d, which should be -1", r);
        exit(-1);
    }

    return 0;
}