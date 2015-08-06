package codevs3;

import codevs3.State.Cell;

public class Hash {
	private static final class XorShift {
		int x = 123456789;
		int y = 362436069;
		int z = 521288629;
		int w = 88675123;

		int nextInt() {
			final int t = x ^ (x << 11);
			x = y;
			y = z;
			z = w;
			return w = (w ^ (w >>> 19)) ^ (t ^ (t >>> 8));
		}

		long nextLong() {
			return ((long) nextInt() << 32) | (long) nextInt();
		}
	}

	public static final long[] hashMap = new long[Parameter.XY * Cell.values().length];
	public static final long[] hashBomb = new long[Parameter.XY * (State.BURST_MAP_INIT + 1)];
	public static final long[] hashPlayer = new long[Parameter.XY * Parameter.CHARACTER_NUM];

	static {
		XorShift x = new XorShift();
		for (int i = 0; i < hashMap.length; ++i)
			hashMap[i] = x.nextLong();
		for (int i = 0; i < hashBomb.length; ++i)
			hashBomb[i] = x.nextLong();
		for (int i = 0; i < hashPlayer.length; ++i)
			hashPlayer[i] = x.nextLong();
	}
}
