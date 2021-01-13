int inc (int i);

int N = 4;

int main() {
	int a[5] = {};
	int i = 0;
	for(;i<N;i++) {
		a[i] = i+1;
	}
	
	int r = 0;
	for(i=0;i<N;i++) {
		r += a[i];
	}
	return r;
}

int inc(int i) {
	return i + 1;
}
