package codevs3;

public class Next {
	int value, lower, upper;
	Operation operations[];
	final long key;
	Next n;

	public Next() {
		key = 0;
	}

	public Next(long key, Next n) {
		lower = AI.MIN_VALUE;
		upper = AI.MAX_VALUE;
		this.key = key;
		this.n = n;
	}
}