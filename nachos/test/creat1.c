/*
 * creat.c
 *
 * simple test for creat 
 */

#include "syscall.h"

int main(int argc, char** argv)
{
    char * filename1 = "creat1.c"
    int r = creat(filename1);
    if (r != 1) {
        printf ("failed to open the named disk file %s\n", filename1);
        exit(-1);
    }


    return 0;
}
