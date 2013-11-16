package util;

/**
 * 何も入れてないのにpollとかできて、そういう使い方をすると死ぬ
 * sizeを超えてaddしても死ぬ
 * @author mhoshi
 */
public class IntQueue {
	private static final int size = 1 << 9;
	private static final int mask = size - 1;
	private final int buf[] = new int[size];
	private int start = 0, last = 0;

	public void add(int x) {
		buf[last] = x;
		last = (last + 1) & mask;
	}

	public int peek() {
		return buf[start];
	}

	public int poll() {
		int res = buf[start];
		start = (start + 1) & mask;
		return res;
	}

	public boolean notEmpty() {
		return start != last;
	}
}
