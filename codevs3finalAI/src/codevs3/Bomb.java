package codevs3;

public class Bomb implements Comparable<Bomb> {
	int id, pos, limitTime, fire;

	public Bomb(Bomb b) {
		this.id = b.id;
		this.pos = b.pos;
		this.limitTime = b.limitTime;
		this.fire = b.fire;
	}

	public Bomb(int id, int pos, int limitTime, int fire) {
		this.id = 1 << id;
		this.pos = pos;
		this.limitTime = Math.min(limitTime, Parameter.maxLiveDepth - 2);
		this.fire = fire;
	}

	public void merge(int id, int limitTime, int fire) {
		this.id |= 1 << id;
		this.limitTime = Math.min(this.limitTime, limitTime);
		this.fire = Math.max(this.fire, fire);
	}

	public String toString() {
		return pos + " " + limitTime + " " + fire;
	}

	@Override
	public int compareTo(Bomb b) {
		return limitTime - b.limitTime;
	}
}