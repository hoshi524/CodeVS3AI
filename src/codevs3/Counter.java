package codevs3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

public class Counter {
	private Counter() {}

	private static final boolean debug = false;
	private static final HashMap<String, Integer> count = new HashMap<>();

	static final void add(String s) {
		if (debug) count.put(s, count.containsKey(s) ? count.get(s) + 1 : 1);
	}

	static final void print() {
		if (debug) for (Entry<String, Integer> e : count.entrySet()) {
			System.err.println(Arrays.deepToString(new Object[] { e.getKey(), e.getValue() }));
		}
	}

	static final void init() {
		if (debug) count.clear();
	}
}
