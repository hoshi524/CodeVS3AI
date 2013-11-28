package codevs3AI;

class Character {
	final int player_id, id;
	int fire, bomb, bombCount, pos;
	boolean dead;

	Character(int player_id, int id, int pos, int fire, int bomb) {
		this.player_id = player_id;
		this.id = id;
		this.pos = pos;
		this.fire = fire;
		this.bomb = bomb;
	}

	Character(Character c) {
		this.player_id = c.player_id;
		this.id = c.id;
		this.pos = c.pos;
		this.fire = c.fire;
		this.bomb = c.bomb;
		this.bombCount = c.bombCount * 2;
	}
}