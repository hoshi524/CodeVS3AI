package codevs3;


public class Next {
	int value, lower, upper;;
	Operation operations[];
	long key;
	Next n;

	void init(long key, Next n) {
		lower = AI.MIN_VALUE;
		upper = AI.MAX_VALUE;
		this.key = key;
		this.n = n;
	}
}