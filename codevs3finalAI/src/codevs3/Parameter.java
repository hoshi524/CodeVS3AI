package codevs3;

public class Parameter {

	private Parameter() {}

	public static final int X = 13;
	public static final int Y = 11;
	public static final int XY = X * Y;
	public static final int CHARACTER_NUM = 4;
	public static final int MIN_FIRE_TIME_INITIAL = 5;
	public static final int PLAYER = 2;
	public static final int maxLiveDepth = 8;
	public static final int MAX_TURN = 1000;

	public static final boolean DEBUG = false;

	public static final void print(String str) {
		if (!DEBUG) return;
		System.err.print(str);
	}

	public static final void println(String str) {
		if (!DEBUG) return;
		System.err.println(str);
	}

	public static final void println() {
		if (!DEBUG) return;
		System.err.println();
	}

	public static int MY_ID = -1;
	public static int ENEMY_ID = -1;

	public static void setMyID(int id) {
		MY_ID = id;
		ENEMY_ID = (MY_ID == 0 ? 1 : 0);
	}
}