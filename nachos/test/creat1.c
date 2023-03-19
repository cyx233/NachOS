/*
 * creat1.c
 *
 * simple test for creat, check if existing file is opened when calling creat
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
		return -1;
	}

	return 0;
}
