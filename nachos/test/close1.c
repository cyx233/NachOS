/*
 * close1.c
 *
 * simple test for close, test case for closing not opened file, returning -1 is expected
 */
#include "syscall.h"
#include "stdio.h"

int main(int argc, char** argv)
{
    int r = close(1);
    if (r == -1) 
    {
        exit(0);
    }

	return 0;
}
