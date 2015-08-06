package test;

public class SameLogic {
	public static void main(String[] args) {
		new SameLogic().Test();
	}

	void Test() {
		codevs3.AI ai1 = new codevs3.AI();
		ai1.think("600000\n0\n500\n0\n13 15\n###############\n#...+...++...+#\n#.#+#+#+#.#.#+#\n#............+#\n#+#.#.#.#+#.#+#\n#+++....++.+..#\n#.#+#.#.#+#+#.#\n#...+.........#\n#.#+#+#+#+#.#.#\n#...++.++++.+.#\n#+#.#.#+#.#.#.#\n#.+....+..++..#\n###############\n4\n0 0 1 1 2 1\n0 1 2 1 2 1\n1 2 11 13 2 1\n1 3 10 13 2 1\n0\n0\nEND\n");
	}
}
