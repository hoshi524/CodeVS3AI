package next;

import java.util.ArrayList;

import codevs3.Parameter;

public class BombList {
	public final ArrayList<Bomb> bombList = new ArrayList<Bomb>();
	public final Bomb[] map = new Bomb[Parameter.XY];

	public BombList() {
	}

	public BombList(BombList bl) {
		for (Bomb t : bl.bombList) {
			Bomb b = new Bomb(t);
			bombList.add(b);
			map[b.pos] = b;
		}
	}

	void add(Bomb b) {
		if (map[b.pos] == null) {
			bombList.add(b);
			map[b.pos] = b;
		} else {
			Bomb t = map[b.pos];
			t.id.addAll(b.id);
			t.fire = Math.max(t.fire, b.fire);
			t.limitTime = Math.min(t.limitTime, b.limitTime);
		}
	}

	void remove(int i) {
		map[bombList.remove(i).pos] = null;
	}
}
