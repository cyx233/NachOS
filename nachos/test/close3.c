/*
 * close3.c
 *
 * simple test for close, test case for reading or writing to a file descriptor that was closed, expecting -1 to be returned
 */
#include "syscall.h"
#include "stdio.h"

int read_closed()
{
    char * filename1 = "temp.txt";
    int r = open(filename1);
    r = close(r);
    char *str = "\nintroduction is good\nthreads is ok\nprocess and multiprogramming is unknown\nhave better performance next time\n\n";

    return read(r, str, 1);
}

int write_closed()
{
    char * filename1 = "temp.txt";
    int r = open(filename1);
    r = close(r);
    char *str = "\nintroduction is good\nthreads is ok\nprocess and multiprogramming is unknown\nhave better performance next time\n\n";

    return write(r, str, 1);
}

int main(int argc, char** argv)
{
    if (read_closed() == -1)
    {
        printf("exception of writing closed file descriptor is caught");
        exit(0);
    }
    else
    {
        exit(-1);
    }

    if (write_closed() == -1) 
    {
        printf("exception of reading closed file descriptor is caught");
        exit(-0);
    }
    else
    {
        exit(-1);
    }
	return 0;
}
