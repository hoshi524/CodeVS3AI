package codevs3;

import java.util.Arrays;

public class TranspositionTable {
	private static final int SIZE = 1 << 22;
	private static final int MASK = SIZE - 1;

	class Node {
		Next next;
		int lower = AI.MIN_VALUE, upper = AI.MAX_VALUE;
		final long key;
		final Node n;

		Node(long key, Node n) {
			this.key = key;
			this.n = n;
		}

		void init() {
			lower = AI.MIN_VALUE;
			upper = AI.MAX_VALUE;
		}
	}

	private Node[] table = new Node[SIZE];

	Node get(long key) {
		Node res = table[(int) (key & MASK)];
		while (res != null && res.key != key) {
			res = res.n;
		}
		return res;
	}

	Node create(long key) {
		int x = (int) (key & MASK);
		return table[x] = new Node(key, table[x]);
	}

	void delete() {
		Arrays.fill(table, null);
	}
}
