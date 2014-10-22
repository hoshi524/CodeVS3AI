package codevs3AI;

class Parameter {

	private Parameter() {
	}

	public static final int X = 13;
	public static final int Y = 11;
	public static final int XY = X * Y;
	public static final int CHARACTER_NUM = 4;
	public static final int MIN_FIRE_TIME_INITIAL = 5;
	public static final int PLAYER = 2;
	public static final int maxLiveDepth = 8;

	public static final boolean DEBUG = false;

	public static final void print(String str) {
		if (!DEBUG)
			return;
		System.err.print(str);
	}

	public static final void println(String str) {
		if (!DEBUG)
			return;
		System.err.println(str);
	}

	public static final void println() {
		if (!DEBUG)
			return;
		System.err.println();
	}

	public static final void print(State s) {
		Parameter.println("map");
		for (int y = 0; y < Parameter.Y; y++) {
			for (int x = 0; x < Parameter.X; x++) {
				boolean flag = true;
				for (Character c : s.characters) {
					if (c.pos == x + y * Parameter.X) {
						if (c.player_id == Parameter.MY_ID)
							Parameter.print("  ap" + c.id);
						else
							Parameter.print("  ep" + c.id);
						flag = false;
						break;
					}
				}
				if (flag)
					Parameter.print(String.format("%5d", s.map[x + y * Parameter.X]));
			}
			Parameter.println();
		}
		int burstMap[] = s.calcBurstMap();
		Parameter.println("burstMap");
		for (int y = 0; y < Parameter.Y; y++) {
			for (int x = 0; x < Parameter.X; x++) {
				Parameter.print(String.format("%5d", burstMap[x + y * Parameter.X]));
			}
			Parameter.println();
		}
	}

	public static int MY_ID = -1;
	public static int ENEMY_ID = -1;

	public static void setMyID(int id) {
		MY_ID = id;
		ENEMY_ID = (MY_ID == 0 ? 1 : 0);
	}
}