package next;

import java.util.ArrayList;

import codevs3.Parameter;

public class Bomb implements Comparable<Bomb> {

	public int pos, limitTime, fire;
	public ArrayList<Integer> id;

	public Bomb(Bomb b) {
		this.id = new ArrayList<>(b.id);
		this.pos = b.pos;
		this.limitTime = b.limitTime;
		this.fire = b.fire;
	}

	public Bomb(int id, int pos, int limitTime, int fire) {
		this.id = new ArrayList<>();
		this.id.add(id);
		this.pos = pos;
		this.limitTime = Math.min(limitTime, Parameter.maxLiveDepth - 2);
		this.fire = fire;
	}

	public String toString() {
		return pos + " " + limitTime + " " + fire;
	}

	@Override
	public int compareTo(Bomb b) {
		return limitTime - b.limitTime;
	}
}