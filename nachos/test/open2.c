/*
 * open2.c
 *
 * simple test for open 
 */

#include "syscall.h"
#include "stdio.h"

int main (int argc, char *argv[])
{
    char *array[3] = {"open1.c", "open2.c", "open3.c"};
    int int_array[3] = {0};
    for (int i = 0; i < 3; i++)
    {
        int_array[i] = open(array[i]);
        if (int_array[i] == -1)
        {
	    printf("error with -1 file desriptor");
            return -1;
        }
        for (int j = 0; j < i; j++)
        {
            if (int_array[j] == int_array[i])
            {
		printf("error with same file descriptor for two openned file");
                return -1;
            }
        }
    }

    return 0;
}
