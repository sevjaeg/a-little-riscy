int calculate(int a, int b, int c, int d);

int A = 1;
int B = 6;
int C = 22;
int D = 5;

int main() {
	int x = calculate(A, B, C, D);
	return x;
}

int calculate(int a, int b, int c, int x) {
	int d = a + c;
	int e = c - b;
	int f = e << 3;
	int g = e + x;
	return g + f - d;
}