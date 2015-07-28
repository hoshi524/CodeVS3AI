package codevs3;

public class Character {
	public int fire, bomb, pos, useBomb, lastBomb;

	public Character(int pos, int fire, int bomb) {
		this.pos = pos;
		this.fire = fire;
		this.bomb = bomb;
		this.useBomb = 0;
		this.lastBomb = 1000;
	}

	public Character(Character c) {
		this.pos = c.pos;
		this.fire = c.fire;
		this.bomb = c.bomb;
		this.useBomb = c.useBomb;
		this.lastBomb = c.lastBomb;
	}
}