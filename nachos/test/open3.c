/*
 * open3.c
 *
 * test 3 for open, test whether a process can use 16 file descriptors
 */

#include "syscall.h"

int main (int argc, char *argv[])
{
    char *filename1 = "test_filename.txt";
    int r = open(filename1);
    if (r == -1)
    {
        return -1;
    }

    char *filename2 = 0;
    r = open(filename2);
    if (r == -1)
    {
        return -1;
    }

    char *array[5] = {"open testing 1", "open testing 2", "open testing 3", "open testing 4", "open testing 5"};
    int int_array[5] = {0};
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

    return 0;
}
