package codevs3;

import java.util.Arrays;
import java.util.Scanner;

public class State {

	enum Cell {
		NUMBER, POWER, BLANK, BOMB, PUT_BOMB, SOFT_BLOCK, HARD_BLOCK;

		boolean canMove() {
			return this == BLANK || this == NUMBER || this == POWER || this == PUT_BOMB;
		}

		boolean cantMove() {
			return this == BOMB || this == SOFT_BLOCK || this == HARD_BLOCK;
		}

		boolean isBomb() {
			return this == BOMB || this == PUT_BOMB;
		}
	}

	final static int BURST_MAP_INIT = 1 << 4;
	private final static int[] dirs = new int[] { -1, 1, -Parameter.X, Parameter.X };
	private final static int[][] ID = { { 0, 1 }, { 2, 3 } };
	private final static int[][] NEXT = new int[Parameter.XY][];

	private final static boolean isHardBlock(int p) {
		int y = p / Parameter.X, x = p % Parameter.X;
		return y % 2 == 1 && x % 2 == 1;
	}

	static {
		for (int i = 0; i < NEXT.length; ++i) {
			int n[] = new int[0];
			for (int d : dirs) {
				if (isin(d, i + d) && !isHardBlock(i + d)) {
					n = Arrays.copyOf(n, n.length + 1);
					n[n.length - 1] = i + d;
				}
			}
			NEXT[i] = n;
		}
	}

	int turn;
	private Cell map[] = null;
	private Character characters[] = new Character[Parameter.CHARACTER_NUM];
	private int burstMap[] = null;
	private Bomb[] bombList = null; // sort制約

	private static final boolean isin(int dir, int next) {
		int x = next % Parameter.X;
		return 0 <= next && next < Parameter.XY && (x != 0 || dir != 1) && (x + 1 != Parameter.X || dir != -1);
	}

	State(State s) {
		this.turn = s.turn;
		map = Arrays.copyOf(s.map, s.map.length);
		burstMap = Arrays.copyOf(s.burstMap, s.burstMap.length);

		characters[0] = new Character(s.characters[0]);
		characters[1] = new Character(s.characters[1]);
		characters[2] = new Character(s.characters[2]);
		characters[3] = new Character(s.characters[3]);
		bombList = new Bomb[s.bombList.length];
		for (int i = 0; i < bombList.length; ++i) {
			bombList[i] = new Bomb(s.bombList[i]);
		}
	}

	State(String str) {
		{// Input
			Scanner sc = new Scanner(str);
			{
				int time = sc.nextInt(); // time
				// 時間がある時は相打ちを狙わない、時間がない時は積極的に相打ちを狙う
				AI.AiutiValue = time > 200000 ? AI.MIN_VALUE >> 2 : AI.MAX_VALUE >> 2;
				// どうしても時間が切れるので時間が無くなったらdepthを減らす
				AI.MAX_DEPTH = time > 100000 ? AI.INIT_MAX_DEPTH : AI.INIT_MAX_DEPTH - 2; // 奇数制約
			}
			turn = sc.nextInt(); // turn
			sc.nextInt(); // max_turn
			Parameter.setMyID(sc.nextInt());
			sc.nextInt(); // Y
			sc.nextInt(); // X

			map = new Cell[Parameter.XY];
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

			bombList = new Bomb[0];
			for (int i = 0, bomb_num = sc.nextInt(); i < bomb_num; ++i) {
				int id = sc.nextInt();
				int pos = (sc.nextInt() - 1) * Parameter.X + (sc.nextInt() - 1);
				int limitTime = sc.nextInt();
				int fire = sc.nextInt();
				if (map[pos].isBomb()) {
					getBomb(pos).merge(id, limitTime, fire);
				} else {
					bombList = add(bombList, new Bomb(id, pos, limitTime, fire));
					// characters[id].lastBomb = turn;
					++characters[id].useBomb;
					map[pos] = Cell.BOMB;
				}
			}
			Arrays.sort(bombList);

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

	boolean anyDead() {
		return burstMap[characters[0].pos] == 0 || burstMap[characters[1].pos] == 0 || burstMap[characters[2].pos] == 0
				|| burstMap[characters[3].pos] == 0;
	}

	void step() {
		Counter.add("step");
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

		boolean anyBomb = false;
		for (int i = 0; i < bombList.length; ++i) {
			Bomb b = bombList[i];
			if (burstMap[b.pos] == 0) {
				anyBomb = true;
				if ((b.id & (1 << 0)) != 0) --characters[0].useBomb;
				if ((b.id & (1 << 1)) != 0) --characters[1].useBomb;
				if ((b.id & (1 << 2)) != 0) --characters[2].useBomb;
				if ((b.id & (1 << 3)) != 0) --characters[3].useBomb;
				bombList = remove(bombList, i--);
				map[b.pos] = Cell.BLANK;
			} else {
				--b.limitTime;
				map[b.pos] = Cell.BOMB;
			}
		}
		if (anyBomb) {
			for (int p = 0; p < Parameter.XY; ++p)
				if (map[p] == Cell.SOFT_BLOCK && burstMap[p] == 0) map[p] = Cell.BLANK;
			burstMap = null;
		} else {
			for (int p = 0; p < Parameter.XY; ++p) {
				if (burstMap[p] != BURST_MAP_INIT) --burstMap[p];
			}
		}

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
	}

	int[] calcBurstMap() {
		Counter.add("calcBurstMap");
		if (burstMap != null) return burstMap;
		burstMap = new int[Parameter.XY];
		Arrays.fill(burstMap, BURST_MAP_INIT);
		boolean used[] = new boolean[Parameter.XY];
		Bomb que[] = new Bomb[bombList.length];

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
							Bomb b = getBomb(next_pos);
							que[qs++] = b;
							if (b.fire + j + 1 >= bb.fire) break;
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

	int value() {
		Character a1 = characters[ID[Parameter.MY_ID][0]], a2 = characters[ID[Parameter.MY_ID][1]];
		Character e1 = characters[ID[Parameter.ENEMY_ID][0]], e2 = characters[ID[Parameter.ENEMY_ID][1]];
		return length[a1.pos][a2.pos] - length[e1.pos][e2.pos] - (a1.lastBomb + a2.lastBomb) + (a1.bomb + a1.fire + a2.bomb + a2.fire);
	}

	int[] getEnemyMap(int player_id) {
		Counter.add("getEnemyMap");
		int enemyMap[] = new int[Parameter.XY], qi, qs, que[] = new int[Parameter.XY];
		for (int id : ID[player_id == 0 ? 1 : 0]) {
			Character c = characters[id];
			enemyMap[c.pos] = 6;
			que[0] = c.pos;
			qi = 0;
			qs = 1;
			while (qi < qs) {
				int now_pos = que[qi++];
				for (int next_pos : NEXT[now_pos]) {
					if (map[next_pos].canMove() && enemyMap[next_pos] + 1 < enemyMap[now_pos]) {
						enemyMap[next_pos] = enemyMap[now_pos] - 1;
						if (enemyMap[next_pos] > 1) que[qs++] = next_pos;
					}
				}
			}
		}
		return enemyMap;
	}

	boolean operations(Operation[] operations, int player_id, int enemyMap[]) {
		Counter.add("operations");
		{// 移動処理
			Operation o = operations[0];
			if (o.move != Move.NONE) {
				Character c = characters[ID[player_id][0]];
				c.pos += o.move.dir;
				if (!isin(o.move.dir, c.pos) || map[c.pos].cantMove()) return false;
			}
			o = operations[1];
			if (o.move != Move.NONE) {
				Character c = characters[ID[player_id][1]];
				c.pos += o.move.dir;
				if (!isin(o.move.dir, c.pos) || map[c.pos].cantMove()) return false;
			}
		}

		// 爆弾処理
		if (operations[0].magic || operations[1].magic) {
			for (int id : ID[player_id]) {
				Operation o = operations[id & 1];
				if (!o.magic) continue;
				Character c = characters[id];
				if (c.useBomb >= c.bomb) return false;
				int pos = c.pos, fire = c.fire;
				Bomb put;
				if (map[pos].isBomb()) {
					Bomb b = getBomb(pos);
					if (b.fire >= fire && burstMap[pos] <= o.burstTime) return false;
					b.merge(id, o.burstTime, fire);
					put = b;
				} else {
					put = new Bomb(id, pos, o.burstTime, fire);
					bombList = add(bombList, put);
					map[pos] = Cell.PUT_BOMB;
				}
				{// 爆弾の有効性チェック
					boolean notValid = enemyMap[pos] == 0;
					Bomb que[] = new Bomb[bombList.length];
					que[0] = put;
					boolean used[] = new boolean[Parameter.XY];
					used[pos] = true;
					int qi = 0, qs = 1, limitTime = Math.min(put.limitTime, burstMap[pos]);
					burstMap[pos] = limitTime;
					while (qi < qs) {
						Bomb bb = que[qi++];
						for (int d : dirs) {
							int next_pos = bb.pos;
							for (int j = 0; j < bb.fire; ++j) {
								next_pos += d;
								if (!isin(d, next_pos) || map[next_pos] == Cell.HARD_BLOCK) break;
								int burst = burstMap[next_pos];
								if (burstMap[next_pos] > limitTime) {
									burstMap[next_pos] = limitTime;
									notValid &= enemyMap[next_pos] == 0;
									if (map[next_pos].isBomb() && !used[next_pos]) {
										used[next_pos] = true;
										Bomb b = getBomb(next_pos);
										que[qs++] = b;
										if (b.fire + j + 1 >= bb.fire) break;
									}
								}
								if (map[next_pos] == Cell.SOFT_BLOCK) {
									notValid &= burst != BURST_MAP_INIT;
									break;
								}
							}
						}
					}
					if (notValid) return false;
				}
				++c.useBomb;
				c.lastBomb = turn;
			}
			Arrays.sort(bombList);
		}
		return true;
	}

	Result getResult() {
		Counter.add("getResult");
		if (bombList.length > 0) {
			if (anyDead()) {
				int allyDead = (burstMap[characters[ID[Parameter.MY_ID][0]].pos] == 0 ? 1 : 0)
						+ (burstMap[characters[ID[Parameter.MY_ID][1]].pos] == 0 ? 1 : 0);
				int enemyDead = (burstMap[characters[ID[Parameter.ENEMY_ID][0]].pos] == 0 ? 1 : 0)
						+ (burstMap[characters[ID[Parameter.ENEMY_ID][1]].pos] == 0 ? 1 : 0);
				if (allyDead < enemyDead) return Result.Win;
				else if (allyDead > enemyDead) return Result.Lose;
				else return Result.Draw;
			}
			final int burstMemo[] = new int[Parameter.XY], blockMemo[] = new int[Parameter.XY];
			for (int pos = 0; pos < Parameter.XY; ++pos) {
				if (map[pos] == Cell.HARD_BLOCK) {
					blockMemo[pos] = -1;
				} else if (map[pos] == Cell.SOFT_BLOCK) {
					if (burstMap[pos] == BURST_MAP_INIT) blockMemo[pos] = -1;
					else blockMemo[pos] = (1 << (burstMap[pos] + 1)) - 1;
				} else if (map[pos].isBomb()) {
					blockMemo[pos] = (1 << burstMap[pos]) - 1;
				}
			}
			boolean used[] = new boolean[Parameter.XY];
			Bomb que[] = new Bomb[bombList.length];
			for (Bomb t : bombList) {
				if (used[t.pos]) continue;
				int qi = 0, qs = 1, bit = 1 << t.limitTime;
				que[0] = t;
				used[t.pos] = true;
				burstMemo[t.pos] |= bit;
				while (qi < qs) {
					Bomb bb = que[qi++];
					for (int d : dirs) {
						int next_pos = bb.pos;
						for (int j = 0; j < bb.fire; j++) {
							next_pos += d;
							if (!isin(d, next_pos) || (blockMemo[next_pos] & bit) != 0) break;
							burstMemo[next_pos] |= bit;
							if (map[next_pos].isBomb() && !used[next_pos]) {
								used[next_pos] = true;
								Bomb b = getBomb(next_pos);
								que[qs++] = b;
								if (b.fire + j + 1 >= bb.fire) break;
							}
						}
					}
				}
			}
			final int endDepth = bombList[bombList.length - 1].limitTime + 1;
			final int memo[][] = new int[endDepth + 1][Parameter.XY];
			Arrays.fill(memo[endDepth], endDepth);
			class Inner {
				int liveDFS(int pos, int depth) {
					if (memo[depth][pos] != 0) return memo[depth][pos];
					int bit = 1 << depth, mask = ~(bit - 1), res = depth;
					if ((burstMemo[pos] & mask) == 0) return memo[depth][pos] = endDepth;
					if ((burstMemo[pos] & bit) == 0 && (res = Math.max(res, liveDFS(pos, depth + 1))) == endDepth) return memo[depth][pos] = res;
					for (int next_pos : NEXT[pos]) {
						if ((burstMemo[next_pos] & bit) == 0 && (blockMemo[next_pos] & bit) == 0
								&& (res = Math.max(res, liveDFS(next_pos, depth + 1))) == endDepth) return memo[depth][pos] = res;
					}
					return memo[depth][pos] = res;
				}
			}
			Inner func = new Inner();
			int minDeadTime = endDepth, deadTime[] = new int[Parameter.CHARACTER_NUM];
			minDeadTime = Math.min(minDeadTime, deadTime[0] = func.liveDFS(characters[0].pos, 1));
			minDeadTime = Math.min(minDeadTime, deadTime[1] = func.liveDFS(characters[1].pos, 1));
			minDeadTime = Math.min(minDeadTime, deadTime[2] = func.liveDFS(characters[2].pos, 1));
			minDeadTime = Math.min(minDeadTime, deadTime[3] = func.liveDFS(characters[3].pos, 1));
			if (minDeadTime < endDepth) {
				int allyDead = (deadTime[ID[Parameter.MY_ID][0]] == minDeadTime ? 1 : 0)
						+ (deadTime[ID[Parameter.MY_ID][1]] == minDeadTime ? 1 : 0);
				int enemyDead = (deadTime[ID[Parameter.ENEMY_ID][0]] == minDeadTime ? 1 : 0)
						+ (deadTime[ID[Parameter.ENEMY_ID][1]] == minDeadTime ? 1 : 0);
				if (allyDead < enemyDead) return Result.Win;
				else if (allyDead > enemyDead) return Result.Lose;
				else return Result.Draw;
			}
		}
		return Result.Continue;
	}

	enum Result {
		Draw, Win, Lose, Continue;
	}

	private final Bomb getBomb(int pos) {
		for (Bomb b : bombList)
			if (b.pos == pos) return b;
		throw new RuntimeException();
	}

	public long getHash() {
		Counter.add("getHash");
		int burstMap[] = calcBurstMap();
		long res = 0;
		for (int pos = 0; pos < Parameter.XY; ++pos) {
			res ^= Hash.hashMap[map[pos].ordinal() * Parameter.XY + pos];
			res ^= Hash.hashBomb[burstMap[pos] * Parameter.XY + pos];
		}
		for (int id = 0; id < Parameter.CHARACTER_NUM; ++id) {
			res ^= Hash.hashPlayer[id * Parameter.XY + characters[id].pos];
		}
		return res;
	}

	private static final <T> T[] add(T[] src, T t) {
		src = Arrays.copyOf(src, src.length + 1);
		src[src.length - 1] = t;
		return src;
	}

	private static final <T> T[] remove(T[] src, int i) {
		T[] res = Arrays.copyOf(src, src.length - 1);
		if (i < res.length) System.arraycopy(src, i + 1, res, i, res.length - i);
		return res;
	}
}