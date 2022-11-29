/*
 * write1.c
 *
 * Write a string to stdout, one byte at a time.  Does not require any
 * of the other system calls to be implemented.
 *
 * Geoff Voelker
 * 11/9/15
 */

#include "syscall.h"

int main (int argc, char *argv[])
{
    char *str = "0987654321";
    char* fname = "/pipe/test";
    int fd = creat (fname);
    if(fd == -1)
        fd = open (fname);
    int r = write (fd, str, 10);
    if (r != 10) {
        exit (-1);
    }
    return 0;
}
