package codevs3;

public class Timer {
	private Timer() {}

	private static final boolean none = false;
	private static final int max = 0xff;
	private static final long prev[] = new long[max];
	private static final long sum[] = new long[max];

	public static final void start(int i) {
		if (none) return;
		prev[i] = System.nanoTime();
	}

	public static final void end(int i) {
		if (none) return;
		sum[i] += System.nanoTime() - prev[i];
	}

	public static final void print() {
		if (none) return;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < max; ++i) {
			if (sum[i] > 0) {
				sb.append(i).append(" : ").append(sum[i]).append("\n");
			}
		}
		System.err.print(sb.toString());
	}
}
