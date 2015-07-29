package codevs3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

public class State {

	enum Cell {
		NUMBER, POWER, BLANK, BOMB, PUT_BOMB, SOFT_BLOCK, HARD_BLOCK;

		boolean canMove() {
			return this == BLANK || this == NUMBER || this == POWER;
		}

		boolean cantMove() {
			return this == BOMB || this == SOFT_BLOCK || this == HARD_BLOCK;
		}

		boolean isBomb() {
			return this == BOMB || this == PUT_BOMB;
		}
	}

	private final static int BURST_MAP_INIT = 1 << 7;
	private final static int[] dirs = new int[] { -1, 1, -Parameter.X, Parameter.X };
	private final static int[][] ID = { { 0, 1 }, { 2, 3 } };
	static int AiutiValue;
	int turn;
	Cell map[] = new Cell[Parameter.XY];
	Character characters[] = new Character[Parameter.CHARACTER_NUM];
	private int burstMap[] = null;
	private ArrayList<Bomb> bombList = new ArrayList<Bomb>();

	private static final boolean isin(int dir, int next) {
		int x = next % Parameter.X;
		return 0 <= next && next < Parameter.XY && (x != 0 || dir != 1) && (x + 1 != Parameter.X || dir != -1);
	}

	State(State s) {
		this.turn = s.turn;
		System.arraycopy(s.map, 0, map, 0, Parameter.XY);

		characters[0] = new Character(s.characters[0]);
		characters[1] = new Character(s.characters[1]);
		characters[2] = new Character(s.characters[2]);
		characters[3] = new Character(s.characters[3]);
		for (Bomb b : s.bombList) {
			bombList.add(new Bomb(b));
		}
	}

	State(String str) {
		{// Input
			Scanner sc = new Scanner(str);
			{
				int time = sc.nextInt(); // time
				// 時間がある時は相打ちを狙わない、時間がない時は積極的に相打ちを狙う
				AiutiValue = time > 200000 ? AI.MIN_VALUE >> 2 : AI.MAX_VALUE >> 2;
			}
			turn = sc.nextInt(); // turn
			sc.nextInt(); // max_turn
			Parameter.setMyID(sc.nextInt());
			sc.nextInt(); // Y
			sc.nextInt(); // X

			sc.next();
			for (int y = 0; y < Parameter.Y; ++y) {
				String line = sc.next();
				for (int x = 0; x < Parameter.X; ++x) {
					switch (line.charAt(x + 1)) {
					case '#':
						map[x + y * Parameter.X] = Cell.HARD_BLOCK;
						break;
					case '+':
						map[x + y * Parameter.X] = Cell.SOFT_BLOCK;
						break;
					case '@':
					case '.':
						map[x + y * Parameter.X] = Cell.BLANK;
						break;
					}
				}
			}
			sc.next();
			int characters_num = sc.nextInt();
			for (int i = 0; i < characters_num; ++i) {
				int player_id = sc.nextInt();
				int id = sc.nextInt();
				int pos = (sc.nextInt() - 1) * Parameter.X + (sc.nextInt() - 1);
				int fire = sc.nextInt();
				int bomb = sc.nextInt();
				characters[i] = new Character(pos, fire, bomb);
			}

			int bomb_num = sc.nextInt();
			for (int i = 0; i < bomb_num; ++i) {
				int id = sc.nextInt();
				int pos = (sc.nextInt() - 1) * Parameter.X + (sc.nextInt() - 1);
				int limitTime = sc.nextInt();
				int fire = sc.nextInt();
				if (map[pos].isBomb()) {
					for (Bomb b : bombList)
						if (b.pos == pos) {
							b.merge(id, limitTime, fire);
							break;
						}
				} else {
					bombList.add(new Bomb(id, pos, limitTime, fire));
					// characters[id].lastBomb = turn;
					++characters[id].useBomb;
					map[pos] = Cell.BOMB;
				}
			}

			int item_num = sc.nextInt();
			for (int i = 0; i < item_num; ++i) {
				String item_type = sc.next();
				int pos = (sc.nextInt() - 1) * Parameter.X + (sc.nextInt() - 1);
				if (item_type.equals("NUMBER_UP")) map[pos] = Cell.NUMBER;
				if (item_type.equals("POWER_UP")) map[pos] = Cell.POWER;
			}

			sc.close();
		}

		if (turn >= 294) {
			Cell tmpMap[] = new Cell[Parameter.XY];
			System.arraycopy(map, 0, tmpMap, 0, Parameter.XY);
			for (int q = 0; q < 3; q++) {
				int dy[] = new int[] { 0, 1, 0, -1 };
				int dx[] = new int[] { 1, 0, -1, 0 };
				int x = -1, y = 0, i = 0, j = 1;
				while (true) {
					x += dx[i];
					y += dy[i];
					int pos = y * Parameter.X + x;
					if (tmpMap[pos] != Cell.HARD_BLOCK) {
						tmpMap[pos] = Cell.HARD_BLOCK;
						break;
					}
					if (i == 0 && x == Parameter.X - j) i = 1;
					else if (i == 1 && y == Parameter.Y - j) i = 2;
					else if (i == 2 && x == j - 1) i = 3;
					else if (i == 3 && y == j) {
						i = 0;
						j++;
					}
				}
			}
		}
	}

	boolean step() {
		if (map[characters[0].pos] == Cell.NUMBER) ++characters[0].bomb;
		else if (map[characters[0].pos] == Cell.POWER) ++characters[0].fire;
		if (map[characters[1].pos] == Cell.NUMBER) ++characters[1].bomb;
		else if (map[characters[1].pos] == Cell.POWER) ++characters[1].fire;
		if (map[characters[2].pos] == Cell.NUMBER) ++characters[2].bomb;
		else if (map[characters[2].pos] == Cell.POWER) ++characters[2].fire;
		if (map[characters[3].pos] == Cell.NUMBER) ++characters[3].bomb;
		else if (map[characters[3].pos] == Cell.POWER) ++characters[3].fire;

		if (map[characters[0].pos] == Cell.NUMBER || map[characters[0].pos] == Cell.POWER) map[characters[0].pos] = Cell.BLANK;
		if (map[characters[1].pos] == Cell.NUMBER || map[characters[1].pos] == Cell.POWER) map[characters[1].pos] = Cell.BLANK;
		if (map[characters[2].pos] == Cell.NUMBER || map[characters[2].pos] == Cell.POWER) map[characters[2].pos] = Cell.BLANK;
		if (map[characters[3].pos] == Cell.NUMBER || map[characters[3].pos] == Cell.POWER) map[characters[3].pos] = Cell.BLANK;

		turn++;
		if (turn >= 294 && (turn & 1) == 0) {
			int dy[] = new int[] { 0, 1, 0, -1 };
			int dx[] = new int[] { 1, 0, -1, 0 };
			int x = -1, y = 0, i = 0, j = 1;
			while (true) {
				x += dx[i];
				y += dy[i];
				int pos = y * Parameter.X + x;
				if (map[pos] != Cell.HARD_BLOCK) {
					map[pos] = Cell.HARD_BLOCK;
					break;
				}
				if (i == 0 && x == Parameter.X - j) i = 1;
				else if (i == 1 && y == Parameter.Y - j) i = 2;
				else if (i == 2 && x == j - 1) i = 3;
				else if (i == 3 && y == j) {
					i = 0;
					j++;
				}
			}
		}

		boolean attacked[] = new boolean[Parameter.XY];
		int softBlock[] = new int[0xff], ssize = 0;
		Bomb que[] = new Bomb[bombList.size()];
		for (Bomb b : bombList) {
			if (!map[b.pos].isBomb()) continue;
			if (b.limitTime > 0) {
				--b.limitTime;
				continue;
			}
			int qi = 0, qs = 0;
			map[b.pos] = Cell.BLANK;
			for (Bomb nb : bombList)
				if (b.pos == nb.pos) {
					que[qs++] = nb;
					break;
				}
			while (qi < qs) {
				Bomb bb = que[qi++];
				attacked[bb.pos] = true;
				for (int d : dirs) {
					int next_pos = bb.pos;
					for (int j = 0; j < bb.fire; ++j) {
						next_pos += d;
						if (!isin(d, next_pos) || map[next_pos] == Cell.HARD_BLOCK) break;
						attacked[next_pos] = true;
						if (map[next_pos] == Cell.SOFT_BLOCK) {
							softBlock[ssize++] = next_pos;
							break;
						} else if (map[next_pos].isBomb()) {
							map[next_pos] = Cell.BLANK;
							for (Bomb nb : bombList)
								if (next_pos == nb.pos) {
									que[qs++] = nb;
									break;
								}
						}
					}
				}
			}
		}

		for (int i = 0; i < ssize; ++i)
			map[softBlock[i]] = Cell.BLANK;
		for (int i = 0; i < bombList.size(); ++i) {
			Bomb b = bombList.get(i);
			if (map[b.pos].isBomb()) {
				map[b.pos] = Cell.BOMB;
			} else {
				if ((b.id & (1 << 0)) != 0) --characters[0].useBomb;
				if ((b.id & (1 << 1)) != 0) --characters[1].useBomb;
				if ((b.id & (1 << 2)) != 0) --characters[2].useBomb;
				if ((b.id & (1 << 3)) != 0) --characters[3].useBomb;
				bombList.remove(i--);
			}
		}
		burstMap = null;
		return attacked[characters[0].pos] || attacked[characters[1].pos] || attacked[characters[2].pos] || attacked[characters[3].pos];
	}

	int[] calcBurstMap() {
		if (burstMap != null) return burstMap;
		burstMap = new int[Parameter.XY];
		Arrays.fill(burstMap, BURST_MAP_INIT);
		int size = bombList.size();
		boolean used[] = new boolean[Parameter.XY];
		Collections.sort(bombList);
		Bomb que[] = new Bomb[size];

		for (Bomb t : bombList) {
			if (used[t.pos]) continue;
			que[0] = t;
			int qi = 0, qs = 1, limitTime = t.limitTime;
			used[t.pos] = true;
			while (qi < qs) {
				Bomb bb = que[qi++];
				burstMap[bb.pos] = Math.min(burstMap[bb.pos], limitTime);
				for (int d : dirs) {
					int next_pos = bb.pos;
					for (int j = 0; j < bb.fire; ++j) {
						next_pos += d;
						if (!isin(d, next_pos) || map[next_pos] == Cell.HARD_BLOCK) break;
						burstMap[next_pos] = Math.min(burstMap[next_pos], limitTime);
						if (map[next_pos] == Cell.SOFT_BLOCK) break;
						else if (map[next_pos].isBomb() && !used[next_pos]) {
							used[next_pos] = true;
							for (Bomb b : bombList)
								if (next_pos == b.pos) {
									que[qs++] = b;
									break;
								}
						}
					}
				}
			}
		}
		return burstMap;
	}

	private static final int length[][] = new int[Parameter.XY][Parameter.XY];
	static {
		int div[] = new int[Parameter.XY], mod[] = new int[Parameter.XY];
		for (int p = 0; p < Parameter.XY; ++p) {
			div[p] = p / Parameter.X;
			mod[p] = p % Parameter.X;
		}
		for (int p1 = 0; p1 < Parameter.XY; ++p1) {
			for (int p2 = 0; p2 < Parameter.XY; ++p2) {
				length[p1][p2] = -0xff * Math.max(0, 8 - (Math.abs(div[p1] - div[p2]) + Math.abs(mod[p1] - mod[p2])));
			}
		}
	}

	int calcValue() {
		Character a1 = characters[ID[Parameter.MY_ID][0]], a2 = characters[ID[Parameter.MY_ID][1]];
		Character e1 = characters[ID[Parameter.ENEMY_ID][0]], e2 = characters[ID[Parameter.ENEMY_ID][1]];
		return length[a1.pos][a2.pos] - length[e1.pos][e2.pos] - (a1.lastBomb << 3) - (a2.lastBomb << 3)
				+ ((a1.bomb + a1.fire + a2.bomb + a2.fire) << 2);
	}

	int operations(Operation[] operations, int player_id) {
		// 移動処理
		for (int id : ID[player_id]) {
			Operation operation = operations[id & 1];
			Character c = characters[id];
			int next_pos = c.pos + operation.move.dir;
			if (operation.move != Move.NONE && (!isin(operation.move.dir, next_pos) || map[next_pos].cantMove())) return 0;
			c.pos = next_pos;
		}

		// 爆弾処理
		if (operations[0].magic || operations[1].magic) {
			int danger = enemyDanger(player_id), count = 0;
			for (int id : ID[player_id]) {
				Operation operation = operations[id & 1];
				if (!operation.magic) continue;
				Character c = characters[id];
				if (c.useBomb >= c.bomb) return 0;
				int pos = c.pos, fire = c.fire;
				if (map[pos].isBomb()) {
					for (Bomb b : bombList)
						if (pos == b.pos) {
							if (b.fire < fire || b.limitTime > operation.burstTime) break;
							else return 0;
						}
				}
				++count;
				base: for (int d : dirs) {
					int next_pos = pos;
					for (int j = 0; j < fire; ++j) {
						next_pos += d;
						if (!isin(d, next_pos) || map[next_pos] == Cell.HARD_BLOCK) break;
						else if (map[next_pos] == Cell.SOFT_BLOCK && burstMap[next_pos] == BURST_MAP_INIT) {
							--count;
							break base;
						}
					}
				}
				if (map[pos].isBomb()) {
					for (Bomb b : bombList)
						if (b.pos == pos) {
							b.merge(id, operation.burstTime, fire);
							break;
						}
				} else {
					bombList.add(new Bomb(id, pos, operation.burstTime, fire));
					map[pos] = Cell.PUT_BOMB;
				}
				++c.useBomb;
				c.lastBomb = turn;
				burstMap = null;

				int update = enemyDanger(player_id);
				if (update <= danger && count > 0) return -2;
				danger = update;
			}
		}
		// liveDFS
		if (bombList.size() > 0) {
			final int burstMemo[] = new int[Parameter.XY], blockMemo[] = new int[Parameter.XY];
			final int burstMap[] = calcBurstMap();
			for (int pos = 0; pos < Parameter.XY; ++pos) {
				if (map[pos] == Cell.HARD_BLOCK) {
					blockMemo[pos] = -1;
				} else if (map[pos] == Cell.SOFT_BLOCK) {
					if (burstMap[pos] == BURST_MAP_INIT) blockMemo[pos] = -1;
					else blockMemo[pos] = (1 << (burstMap[pos] + 1)) - 1;
				}
			}
			boolean usedBomb[] = new boolean[Parameter.XY];
			Bomb que[] = new Bomb[bombList.size()];
			Collections.sort(bombList);
			for (Bomb t : bombList) {
				if (usedBomb[t.pos]) continue;
				int qi = 0, qs = 0, bit = 1 << t.limitTime, mask = bit - 1;
				for (Bomb b : bombList)
					if (t.pos == b.pos) {
						que[qs++] = b;
						break;
					}
				usedBomb[t.pos] = true;
				while (qi < qs) {
					Bomb bb = que[qi++];
					burstMemo[bb.pos] |= bit;
					blockMemo[bb.pos] |= mask;
					for (int d : dirs) {
						int next_pos = bb.pos;
						for (int j = 0; j < bb.fire; j++) {
							next_pos += d;
							if (!isin(d, next_pos) || (blockMemo[next_pos] & bit) != 0) break;
							burstMemo[next_pos] |= bit;
							if (map[next_pos].isBomb() && !usedBomb[next_pos]) {
								for (Bomb b : bombList)
									if (next_pos == b.pos) {
										que[qs++] = b;
										break;
									}
								usedBomb[next_pos] = true;
							}
						}
					}
				}
			}
			final int endDepth = bombList.get(bombList.size() - 1).limitTime + 1;
			final int memo[][] = new int[endDepth + 1][Parameter.XY];
			Arrays.fill(memo[endDepth], endDepth);
			class Inner {
				int liveDFS(int pos, int depth) {
					if (memo[depth][pos] != 0) return memo[depth][pos];
					int bit = 1 << depth, res = depth;
					if ((burstMemo[pos] & bit) == 0 && (res = Math.max(res, liveDFS(pos, depth + 1))) == endDepth) return memo[depth][pos] = res;
					for (int d : dirs) {
						int next_pos = pos + d;
						if (isin(d, next_pos) && (burstMemo[next_pos] & bit) == 0 && (blockMemo[next_pos] & bit) == 0
								&& (res = Math.max(res, liveDFS(next_pos, depth + 1))) == endDepth) return memo[depth][pos] = res;
					}
					return memo[depth][pos] = res;
				}
			}
			Inner func = new Inner();
			int minDeadTime = endDepth, deadTime[] = new int[Parameter.CHARACTER_NUM];
			for (int id = 0; id < Parameter.CHARACTER_NUM; ++id) {
				Character c = characters[id];
				if ((burstMemo[c.pos] & 1) != 0) deadTime[id] = 0;
				else deadTime[id] = func.liveDFS(c.pos, 1);
				minDeadTime = Math.min(minDeadTime, deadTime[id]);
			}
			if (minDeadTime < endDepth) {
				int allyDead = (deadTime[ID[player_id][0]] == minDeadTime ? 1 : 0) + (deadTime[ID[player_id][1]] == minDeadTime ? 1 : 0);
				int enemy_id = player_id == 0 ? 1 : 0;
				int enemyDead = (deadTime[ID[enemy_id][0]] == minDeadTime ? 1 : 0) + (deadTime[ID[enemy_id][1]] == minDeadTime ? 1 : 0);
				if (allyDead == enemyDead) return 1;
				else if (allyDead > enemyDead) return -1;
				else if (allyDead < enemyDead) return 3;
			}
		}
		return 2;
	}

	private int enemyDanger(int player_id) {
		int burstMap[] = calcBurstMap();
		int res = 0, que[] = new int[0x4f], qi, qs;
		int enemyMap[] = new int[Parameter.XY];
		for (int id : ID[player_id == 0 ? 1 : 0]) {
			Character c = characters[id];
			enemyMap[c.pos] = 6;
			que[0] = c.pos;
			qi = 0;
			qs = 1;
			while (qi < qs) {
				int now_pos = que[qi++];
				res += enemyMap[now_pos] * Math.max(10 - burstMap[now_pos], 0);
				for (int d : dirs) {
					int next_pos = now_pos + d;
					if (isin(d, next_pos) && map[next_pos].canMove() && enemyMap[next_pos] < enemyMap[now_pos]) {
						enemyMap[next_pos] = enemyMap[now_pos] - 1;
						if (enemyMap[next_pos] > 1) que[qs++] = next_pos;
					}
				}
			}
			res -= qs << 7;
		}
		return res;
	}

	public long getHash() {
		int burstMap[] = calcBurstMap();
		long res = 0;
		for (int pos = 0; pos < Parameter.XY; ++pos) {
			res ^= Hash.hashMap[map[pos].ordinal() * Parameter.XY + pos];
			res ^= Hash.hashBomb[Math.min(burstMap[pos], 10) * Parameter.XY + pos];
		}
		for (int id = 0; id < Parameter.CHARACTER_NUM; ++id) {
			res ^= Hash.hashPlayer[id * Parameter.XY + characters[id].pos];
		}
		return res;
	}
}