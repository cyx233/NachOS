#include "syscall.h"

int main (int argc, char *argv[])
{
    char *str = "This is a very \
        very very very very very very very very very very \
        very very very very very very very very very very \
        very very very very very very very very very very \
        very very very very very very very very very very \
        very very very very very very very very very very \
        long string. \
        ";
    int r = open (str);
    if (r != 1) {
        printf ("failed to write character (r = %d)\n", r);
        exit (0);
    }
    return 1;
}
