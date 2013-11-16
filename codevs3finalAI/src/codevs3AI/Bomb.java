package codevs3AI;

class Bomb implements Comparable<Bomb> {
	int id, pos, limitTime, fire;

	Bomb(Bomb b) {
		this.id = b.id;
		this.pos = b.pos;
		this.limitTime = b.limitTime;
		this.fire = b.fire;
	}

	Bomb(int id, int pos, int limitTime, int fire) {
		this.id = id;
		this.pos = pos;
		this.limitTime = limitTime;
		this.fire = fire;
	}

	String toString(int x) {
		return pos + " " + limitTime + " " + fire;
	}

	@Override
	public int compareTo(Bomb arg) {
		return limitTime - arg.limitTime;
	}
}