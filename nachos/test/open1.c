/*
 * open1.c
 *
 * simple test for open 
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
    return 0;
}
