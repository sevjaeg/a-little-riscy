int inc (int i);

int N = 4;

int main() {
	int n = N;
	int a[5] = {};
	int i = 0;
	for(;i<n;i++) {
		a[i] = i+1;
	}
	
	int r = 0;
	for(i=0;i<n;i++) {
		r += a[i];
	}
	return r;
}

int inc(int i) {
	return i + 1;
}
