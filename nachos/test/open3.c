/*
 * open3.c
 *
 * test 3 for open, test whether a process can use 16 file descriptors
 */

#include "syscall.h"
#include "stdio.h"

int main (int argc, char *argv[])
{
    char *filename1 = "open1.c";
    int r = open(filename1);
    if (r == -1)
    {
        return -1;
    }

    char *filename2 = "open2.c";
    r = open(filename2);
    if (r == -1)
    {
        return -1;
    }

    char *array[3] = {"open1.c", "open2.c", "open3.c"};
    int int_array[3] = {0};
    for (int i = 0; i < i; i++)
    {
        int_array[i] = open(array[i]);
        if (int_array[i] == -1)
        {
            return -1;
        }
        for (int j = 0; j < i; j++)
        {
            if (int_array[j] == int_array[i])
            {
                return -1;
            }
        }
    }
	
    // open extra times to meet the max 16 opened files
    int int_array_16[11] = {0};
    char *filename3 = "creat1.c";
    for (int i = 0; i < 11; i++) 
    {
        int_array_16[i] = open(filename3);
        if (int_array_16[i] == -1)
        {
	    printf("error open returning -1\n");
            return -1;
        }
        for (int j = 0; j < i; j++)
        {
            if (int_array_16[j] == int_array_16[i])
            {
		printf("file %d has descriptor %d, file %d has descriptor %d, error open returning duplicate file descriptor\n", i, int_array_16[i], j, int_array_16[j]);
                return -1;
            }
        }
    }

    return 0;
}
