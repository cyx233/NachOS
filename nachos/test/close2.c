/*
 * close2.c
 *
 * simple test for close, test case for closing normally
 */
#include "syscall.h"
#include "stdio.h"

int main(int argc, char** argv)
{
    char * filename1 = "temp.txt";
    int r = open(filename1);
    r = close(r);
    if (r == -1) 
    {
        printf("encountering error when closing an opened file");
        exit(-1);
    }
	return 0;
}
