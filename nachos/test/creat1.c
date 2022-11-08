/*
 * creat.c
 *
 * simple test for creat 
 */

#include "syscall.h"

int main(int argc, char** argv)
{
    int r = creat(1);
    if (r != 1) {
        printf ("failed to open the named disk file %d\n", r);
        exit(-1);
    }

    return 0;
}
