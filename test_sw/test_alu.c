# define WIN 0

# if WIN
#include <stdio.h>
# endif

int A = 13;
int B = 2;
int C = 11;
int D = 7;

int main() {
	int a = A;
	int b = B;
	int c = C;
	int d = D;
	int r =  ((((a+5-b) << 3) >> b) - ((c | a) - 9)) ^ (d+2);
	
#if WIN
	printf("%d\n", r);
#endif
	
	return r;
}