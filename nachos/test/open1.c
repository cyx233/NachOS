/*
 * open1.c
 *
 * simple test for open 
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
    return 0;
}
