/*
 * creat2.c
 *
 * simple test for creat, check if creat creates the new file if it does not exist
 */
#include "syscall.h"
#include "stdio.h"

int main(int argc, char** argv)
{
	char *filename1 = "temp.txt";
	int r = open(filename1);
	if (r == -1)
	{
		creat(filename1);
        r = open(filename1);
        if (r == -1) 
        {
            return -1;
        }
	}

	return 0;
}
