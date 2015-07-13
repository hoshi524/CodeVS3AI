package codevs3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class State {

	enum Cell {
		NUMBER, POWER, BLANK, BOMB, SOFT_BLOCK, HARD_BLOCK;

		boolean canMove() {
			return this == BLANK || this == NUMBER || this == POWER;
		}

		boolean cantMove() {
			return this == BOMB || this == SOFT_BLOCK || this == HARD_BLOCK;
		}
	}

	private final static int BURST_MAP_INIT = 1000;
	private final static int[] dirs = new int[] { -1, 1, -Parameter.X, Parameter.X };
	private final static int[] mapPosition = new int[Parameter.XY];
	static {
		// 角は評価を下げておく
		mapPosition[0] = mapPosition[1] = mapPosition[11] = mapPosition[12] = mapPosition[13] = mapPosition[25] = mapPosition[117] = mapPosition[129] = mapPosition[130] = mapPosition[131] = mapPosition[141] = mapPosition[142] = -0xff;
	}
	static int AiutiValue;
	static int ac1, ac2;
	int turn;

	Cell prevmap[] = new Cell[Parameter.XY];
	Cell map[] = new Cell[Parameter.XY];

	Character characters[] = new Character[Parameter.CHARACTER_NUM];
	private int fieldBombCount[] = new int[Parameter.CHARACTER_NUM];
	private int burstMap[] = null;

	private ArrayList<Bomb> bombList = new ArrayList<Bomb>();

	private static final boolean isin(int dir, int next) {
		int x = next % Parameter.X;
		return 0 <= next && next < Parameter.XY && !(x == 0 && dir == 1) && !(x == (Parameter.X - 1) && dir == -1);
	}

	State(State s) {
		this.turn = s.turn;
		System.arraycopy(s.map, 0, map, 0, Parameter.XY);
		System.arraycopy(s.prevmap, 0, prevmap, 0, Parameter.XY);
		System.arraycopy(s.fieldBombCount, 0, fieldBombCount, 0, Parameter.CHARACTER_NUM);

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
				AiutiValue = time > 100000 ? AI.MIN_VALUE >> 4 : AI.MAX_VALUE >> 4;
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
				characters[i] = new Character(sc.nextInt(), sc.nextInt(), (sc.nextInt() - 1) * Parameter.X + (sc.nextInt() - 1),
						sc.nextInt(), sc.nextInt());
			}

			int bomb_num = sc.nextInt();
			for (int i = 0; i < bomb_num; ++i) {
				int id = sc.nextInt();
				int pos = (sc.nextInt() - 1) * Parameter.X + (sc.nextInt() - 1);
				int limitTime = sc.nextInt();
				int fire = sc.nextInt();
				Bomb b = new Bomb(id, pos, limitTime, fire);

				fieldBombCount[id]++;
				bombList.add(b);
				map[pos] = Cell.BOMB;
			}

			int item_num = sc.nextInt();
			for (int i = 0; i < item_num; ++i) {
				String item_type = sc.next();
				int pos = (sc.nextInt() - 1) * Parameter.X + (sc.nextInt() - 1);
				if (item_type.equals("NUMBER_UP")) {
					map[pos] = Cell.NUMBER;
				}
				if (item_type.equals("POWER_UP")) {
					map[pos] = Cell.POWER;
				}
			}

			sc.close();

			Parameter.print(this);
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
						mapPosition[pos] = (Integer.MIN_VALUE / 4);
						break;
					}
					if (i == 0 && x == Parameter.X - j)
						i = 1;
					else if (i == 1 && y == Parameter.Y - j)
						i = 2;
					else if (i == 2 && x == j - 1)
						i = 3;
					else if (i == 3 && y == j) {
						i = 0;
						j++;
					}
				}
			}
		}
		if (Parameter.MY_ID == 0) {
			ac1 = 0;
			ac2 = 1;
		} else {
			ac1 = 2;
			ac2 = 3;
		}
		prevmap = Arrays.copyOf(map, map.length);
	}

	void step() {
		if (map[characters[0].pos] == Cell.NUMBER)
			++characters[0].bomb;
		else if (map[characters[0].pos] == Cell.POWER)
			++characters[0].fire;
		if (map[characters[1].pos] == Cell.NUMBER)
			++characters[1].bomb;
		else if (map[characters[1].pos] == Cell.POWER)
			++characters[1].fire;
		if (map[characters[2].pos] == Cell.NUMBER)
			++characters[2].bomb;
		else if (map[characters[2].pos] == Cell.POWER)
			++characters[2].fire;
		if (map[characters[3].pos] == Cell.NUMBER)
			++characters[3].bomb;
		else if (map[characters[3].pos] == Cell.POWER)
			++characters[3].fire;

		if (map[characters[0].pos] == Cell.NUMBER || map[characters[0].pos] == Cell.POWER)
			map[characters[0].pos] = Cell.BLANK;
		if (map[characters[1].pos] == Cell.NUMBER || map[characters[1].pos] == Cell.POWER)
			map[characters[1].pos] = Cell.BLANK;
		if (map[characters[2].pos] == Cell.NUMBER || map[characters[2].pos] == Cell.POWER)
			map[characters[2].pos] = Cell.BLANK;
		if (map[characters[3].pos] == Cell.NUMBER || map[characters[3].pos] == Cell.POWER)
			map[characters[3].pos] = Cell.BLANK;

		int size = bombList.size();
		boolean use[] = new boolean[size];
		boolean attacked[] = new boolean[Parameter.XY];

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
					for (int bi = 0; bi < bombList.size(); bi++) {
						if (bombList.get(bi).pos == pos) {
							use[bi] = true;
						}
					}
					break;
				}
				if (i == 0 && x == Parameter.X - j)
					i = 1;
				else if (i == 1 && y == Parameter.Y - j)
					i = 2;
				else if (i == 2 && x == j - 1)
					i = 3;
				else if (i == 3 && y == j) {
					i = 0;
					j++;
				}
			}
		}
		List<Integer> softBlockList = new ArrayList<Integer>();
		Bomb que[] = new Bomb[size];
		for (int b1 = 0; b1 < size; ++b1) {
			if (use[b1])
				continue;
			Bomb b = bombList.get(b1);
			if (b.limitTime > 0) {
				--b.limitTime;
				continue;
			}
			int qi = 0, qs = 1;
			que[0] = b;
			use[b1] = true;
			while (qi < qs) {
				Bomb bb = que[qi++];
				attacked[bb.pos] = true;
				for (int d : dirs) {
					int next_pos = bb.pos;
					for (int j = 0; j < bb.fire; ++j) {
						next_pos += d;
						if (!isin(d, next_pos) || map[next_pos] == Cell.HARD_BLOCK)
							break;
						attacked[next_pos] = true;
						if (map[next_pos] == Cell.SOFT_BLOCK) {
							softBlockList.add(next_pos);
							break;
						}
						if (map[next_pos] == Cell.BOMB) {
							map[next_pos] = Cell.BLANK;
							for (int b2 = 0; b2 < size; ++b2) {
								Bomb nb = bombList.get(b2);
								if (next_pos == nb.pos && !use[b2]) {
									use[b2] = true;
									que[qs++] = nb;
								}
							}
						}
					}
				}
			}
		}

		if (attacked[characters[0].pos])
			characters[0].dead = true;
		if (attacked[characters[1].pos])
			characters[1].dead = true;
		if (attacked[characters[2].pos])
			characters[2].dead = true;
		if (attacked[characters[3].pos])
			characters[3].dead = true;

		for (int pos : softBlockList)
			map[pos] = Cell.BLANK;
		ArrayList<Bomb> next_bl = new ArrayList<Bomb>();
		for (int i = 0; i < size; ++i) {
			if (!use[i]) {
				next_bl.add(bombList.get(i));
			} else {
				--fieldBombCount[bombList.get(i).id];
				map[bombList.get(i).pos] = Cell.BLANK;
			}
		}
		bombList = next_bl;
		burstMap = null;
		prevmap = Arrays.copyOf(map, map.length);
	}

	int[] calcBurstMap() {
		if (burstMap != null) {
			return burstMap;
		}
		burstMap = new int[Parameter.XY];
		Arrays.fill(burstMap, BURST_MAP_INIT);
		int size = bombList.size();
		boolean use[] = new boolean[size];
		Collections.sort(bombList);
		Bomb que[] = new Bomb[size];
		int qi, qs;

		for (int i = 0; i < size; ++i) {
			if (use[i])
				continue;
			que[0] = bombList.get(i);
			qi = 0;
			qs = 1;
			use[i] = true;
			int limitTime = bombList.get(i).limitTime;
			while (qi < qs) {
				Bomb bb = que[qi++];
				burstMap[bb.pos] = Math.min(burstMap[bb.pos], limitTime);

				for (int d : dirs) {
					int next_pos = bb.pos;
					for (int j = 0; j < bb.fire; ++j) {
						next_pos += d;
						if (!isin(d, next_pos) || map[next_pos] == Cell.HARD_BLOCK)
							break;
						burstMap[next_pos] = Math.min(burstMap[next_pos], limitTime);
						if (map[next_pos] == Cell.SOFT_BLOCK)
							break;
						if (map[next_pos] == Cell.BOMB) {
							for (int k = 0; k < size; ++k) {
								Bomb b = bombList.get(k);
								if (next_pos == b.pos && !use[k]) {
									use[k] = true;
									que[qs++] = b;
								}
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
				length[p1][p2] = -0xff * Math.max(0, 10 - (Math.abs(div[p1] - div[p2]) + Math.abs(mod[p1] - mod[p2])));
			}
		}
	}

	int calcValue() {
		Character c1 = characters[ac1], c2 = characters[ac2];
		return length[c1.pos][c2.pos] + mapPosition[c1.pos] + c1.bombCount + mapPosition[c2.pos] + c2.bombCount
				+ ((c1.bomb + c1.fire + c2.bomb + c2.fire) << 4);
	}

	int calcFleeValue() {
		return calcValue();
	}

	int operations(Operation[] operations, int player_id, int depth) {
		// 移動処理
		for (Character character : characters) {
			if (character.player_id != player_id)
				continue;
			Operation operation = operations[character.id & 1];
			int next_pos = character.pos + operation.move.dir;
			if (operation.move != Move.NONE && (!isin(operation.move.dir, next_pos) || prevmap[next_pos].cantMove())) {
				return 0;
			}
			character.pos = next_pos;
		}

		// 爆弾処理
		if (operations[0].magic || operations[1].magic) {
			int[] posBuf = new int[2], fireBuf = new int[2];
			int baseDanger = enemyDanger(player_id);
			Arrays.fill(posBuf, -1);
			for (Character character : characters) {
				Operation operation = operations[character.id & 1];
				if (character.player_id != player_id || !operation.magic)
					continue;
				if (fieldBombCount[character.id] >= character.bomb) {
					return 0;
				}
				posBuf[character.id & 1] = character.pos;
				fireBuf[character.id & 1] = character.fire;

				bombList.add(new Bomb(character.id, character.pos, operation.burstTime, character.fire));
				map[character.pos] = Cell.BOMB;
				character.bombCount |= 0x3fff << depth;
				++fieldBombCount[character.id];
			}
			int now_danger = enemyDanger(player_id);
			if (baseDanger >= now_danger && !softBlockBomb(posBuf, fireBuf)) {
				return -2;
			}
		}

		{// liveDFS
			int memo[][] = new int[Parameter.maxLiveDepth + 1][Parameter.XY];
			for (int i = 0; i < Parameter.maxLiveDepth; ++i) {
				Arrays.fill(memo[i], -1);
			}
			int burstMemo[] = new int[Parameter.XY];
			int blockMemo[] = new int[Parameter.XY];

			int softBlockList[] = new int[Parameter.XY], sfi = 0;
			for (int pos = 0; pos < Parameter.XY; ++pos) {
				if (map[pos] == Cell.HARD_BLOCK) {
					blockMemo[pos] = -1;
				} else if (map[pos] == Cell.SOFT_BLOCK) {
					softBlockList[sfi++] = pos;
				}
			}
			softBlockList = Arrays.copyOf(softBlockList, sfi);
			boolean softBlockClash[] = new boolean[Parameter.XY], tmpSoftBlockClash[] = new boolean[Parameter.XY];
			boolean usedBomb[] = new boolean[Parameter.XY];
			Bomb que[] = new Bomb[Parameter.XY];
			List<Bomb> bl = new ArrayList<>(bombList);
			int liveDepth = 0;
			for (; liveDepth < Parameter.maxLiveDepth && !bl.isEmpty(); ++liveDepth) {
				for (Bomb bomb : bl) {
					if (bomb.limitTime == liveDepth && !usedBomb[bomb.pos]) {
						int qi = 0, qs = 0;
						for (Bomb bomb2 : bl)
							if (bomb.pos == bomb2.pos)
								que[qs++] = bomb2;
						usedBomb[bomb.pos] = true;
						while (qi < qs) {
							Bomb bb = que[qi++];
							burstMemo[bb.pos] |= 1 << liveDepth;
							for (int d : dirs) {
								int next_pos = bb.pos;
								for (int j = 0; j < bb.fire; j++) {
									next_pos += d;
									if (!isin(d, next_pos) || map[next_pos] == Cell.HARD_BLOCK)
										break;
									burstMemo[next_pos] |= 1 << liveDepth;
									if (map[next_pos] == Cell.SOFT_BLOCK && !softBlockClash[next_pos]) {
										tmpSoftBlockClash[next_pos] = true;
										break;
									} else if (map[next_pos] == Cell.BOMB && !usedBomb[next_pos]) {
										usedBomb[next_pos] = true;
										for (Bomb bomb2 : bl)
											if (next_pos == bomb2.pos)
												que[qs++] = bomb2;
									}
								}
							}
						}
					}
				}
				for (int i = 0; i < bl.size(); ++i) {
					int pos = bl.get(i).pos;
					if (!usedBomb[pos]) {
						blockMemo[pos] |= 1 << liveDepth;
					} else {
						bl.remove(i--);
					}
				}
				for (int p : softBlockList) {
					if (!softBlockClash[p]) {
						blockMemo[p] |= 1 << liveDepth;
						softBlockClash[p] |= tmpSoftBlockClash[p];
					}
				}
			}

			int minDeadTime = liveDepth;
			for (Character c : this.characters) {
				c.deadTime = liveDFS(c.pos, 0, memo, burstMemo, blockMemo, liveDepth);
				minDeadTime = Math.min(minDeadTime, c.deadTime);
			}
			if (minDeadTime < liveDepth) {
				int allyDead = 0, enemyDead = 0;
				for (Character c : this.characters)
					if (c.deadTime == minDeadTime) {
						if (c.player_id == player_id)
							++allyDead;
						else
							++enemyDead;
					}
				if (allyDead == enemyDead) {
					return 1;
				} else if (allyDead > enemyDead) {
					return -1;
				} else if (allyDead < enemyDead) {
					return 3;
				}
			}
		}
		return 2;
	}

	private int liveDFS(int pos, int depth, int memo[][], int burstMemo[], int blockMemo[], int liveDepth) {
		if (memo[depth][pos] != -1)
			return memo[depth][pos];
		if (depth == liveDepth)
			return memo[depth][pos] = liveDepth;
		int bit = 1 << depth;
		int res = depth;
		if ((burstMemo[pos] & bit) == 0) {
			res = Math.max(res, liveDFS(pos, depth + 1, memo, burstMemo, blockMemo, liveDepth));
			if (res == liveDepth) {
				return memo[depth][pos] = res;
			}
		}
		if (depth > 0) {
			for (int d : dirs) {
				int next_pos = pos + d;
				if (!isin(d, next_pos) || (burstMemo[next_pos] & bit) != 0 || (blockMemo[next_pos] & bit) != 0)
					continue;
				res = Math.max(res, liveDFS(next_pos, depth + 1, memo, burstMemo, blockMemo, liveDepth));
				if (res == liveDepth) {
					return memo[depth][pos] = res;
				}
			}
		}
		return memo[depth][pos] = res;
	}

	private int enemyDanger(int player_ip) {
		burstMap = null;
		int burstMap[] = calcBurstMap();
		int res = 0, que[] = new int[0x1f], qi, qs;
		for (Character c : characters) {
			if (c.player_id == player_ip)
				continue;
			int enemyMap[] = new int[Parameter.XY];
			enemyMap[c.pos] = 4;
			que[0] = c.pos;
			qi = 0;
			qs = 1;
			int enemyDanger = 0;
			while (qi < qs) {
				int now_pos = que[qi++];
				enemyDanger += enemyMap[now_pos] * Math.max(10 - burstMap[now_pos], 0);
				for (int d : dirs) {
					int next_pos = now_pos + d;
					if (isin(d, next_pos) && map[next_pos].canMove() && enemyMap[next_pos] < enemyMap[now_pos]) {
						enemyMap[next_pos] = enemyMap[now_pos] - 1;
						if (enemyMap[next_pos] > 1) {
							que[qs++] = next_pos;
						}
					}
				}
			}
			res += enemyDanger + 1000 / qs;
		}
		return res;
	}

	private boolean softBlockBomb(int[] pos, int fire[]) {
		int res = 0;
		int burstMap[] = calcBurstMap();
		for (int i = 0; i < 2; ++i) {
			if (fire[i] == 0) {
				res |= 1 << i;
				continue;
			}
			int posi = pos[i];
			int firei = fire[i];

			base: for (int d : dirs) {
				int next_pos = posi;
				for (int j = 0; j < firei; ++j) {
					next_pos += d;
					if (!isin(d, next_pos) || map[next_pos] == Cell.HARD_BLOCK)
						break;
					if (map[next_pos] == Cell.SOFT_BLOCK && burstMap[next_pos] == BURST_MAP_INIT) {
						res |= 1 << i;
						break base;
					}
				}
			}
		}
		return res == 0x3;
	}

	public long getHash() {
		int burstMap[] = calcBurstMap();
		long res = 0;
		for (int pos = 0; pos < Parameter.XY; ++pos) {
			res ^= Hash.hashMap[Math.max(0, map[pos].ordinal() - 2) * Parameter.XY + pos];
			res ^= Hash.hashBomb[Math.min(burstMap[pos], 10) * Parameter.XY + pos];
		}
		for (Character c : characters) {
			res ^= Hash.hashPlayer[c.id * Parameter.XY + c.pos];
		}
		return res;
	}
}