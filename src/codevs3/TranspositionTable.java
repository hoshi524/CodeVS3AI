package codevs3;

import java.util.Arrays;

public class TranspositionTable {
	/*
	 * Javaでないと、この糞設計は成立しないな・・・
	 * out of rangeするとException吐くことを前提になっている
	 * C++でやったら死ぬ未来しか見えない。(assert入れてデバッグとかは可能ではありそうだけど・・・)
	 */
	private static final int SIZE = 1 << 20;
	private static final int MASK = SIZE - 1;
	private Next[] table = new Next[SIZE];

	Next get(long key) {
		Next res = table[(int) (key & MASK)];
		while (res != null && res.key != key) {
			res = res.n;
		}
		return res;
	}

	Next create(long key) {
		int x = (int) (key & MASK);
		return table[x] = new Next(key, table[x]);
	}

	void clear() {
		Arrays.fill(table, null);
	}
}
