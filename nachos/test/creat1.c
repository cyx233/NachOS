/*
 * creat1.c
 *
 * simple test for creat
 */
#include "syscall.h"
#include "stdio.h"

int main(int argc, char** argv)
{
	char *filename1 = "temp.txt";
	int r = creat(filename1);
	if (r == -1)
	{
		printf("failed to open the named disk file %s\n", filename1);
		exit(-1);
	}

	return 0;
}
