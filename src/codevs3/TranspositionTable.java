package codevs3;

import java.util.Arrays;

public class TranspositionTable {
	private static final int SIZE = 1 << 26;
	private static final int MASK = SIZE - 1;
	private static final int BUFFER_SIZE = 1 << 20;

	private Next[] buffer = new Next[BUFFER_SIZE];
	private Next[] table = new Next[SIZE];
	private int index = 0;
	{
		for (int i = 0; i < BUFFER_SIZE; ++i)
			buffer[i] = new Next();
	}

	Next get(long key) {
		Next res = table[(int) (key & MASK)];
		while (res != null && res.key != key) {
			res = res.n;
		}
		return res;
	}

	Next create(long key) {
		int x = (int) (key & MASK);
		buffer[index].init(key, table[x]);
		return table[x] = buffer[index++];
	}

	void clear() {
		Arrays.fill(table, null);
		index = 0;
	}
}
