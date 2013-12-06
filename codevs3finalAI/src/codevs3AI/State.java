package codevs3AI;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

class State {

	final static int NUMBER = 0;
	final static int POWER = 1;
	final static int BLANK = 2;
	final static int BOMB = 3;
	final static int SOFT_BLOCK = 4;
	final static int HARD_BLOCK = 5;
	final static int BURST_MAP_INIT = 1000;
	final static int MAX_INT = Integer.MAX_VALUE / 0xfff;
	final static int[] dirs = new int[] { 0, -1, 1, -Parameter.X, Parameter.X };
	final static int[] mapPosition = new int[Parameter.XY];
	static {
		// 角は評価を下げておく
		mapPosition[0] = mapPosition[1] = mapPosition[11] = mapPosition[12] = mapPosition[13] = mapPosition[25] = mapPosition[117] = mapPosition[129] = mapPosition[130] = mapPosition[131] = mapPosition[141] = mapPosition[142] = Integer.MIN_VALUE / 4;
	}
	static long time;
	static int allyDanger, enemyDanger;
	static int ac1, ac2;
	int turn;

	int map[] = new int[Parameter.XY];
	int burstMap[] = new int[Parameter.XY];
	static int itemMap[] = new int[Parameter.XY];
	static int liveMap[];

	Character characters[] = new Character[Parameter.CHARACTER_NUM];
	int fieldBombCount[] = new int[Parameter.CHARACTER_NUM];

	ArrayList<Bomb> bombList = new ArrayList<Bomb>();
	Set<Integer> nowPutBomb = new HashSet<Integer>();

	public static final boolean isin(int dir, int next) {
		int x = next % Parameter.X;
		return 0 <= next && next < Parameter.XY && !(x == 0 && dir == 1) && !(x == (Parameter.X - 1) && dir == -1);
	}

	State(State s) {
		this.turn = s.turn;
		System.arraycopy(s.map, 0, map, 0, Parameter.XY);
		System.arraycopy(s.burstMap, 0, burstMap, 0, Parameter.XY);
		System.arraycopy(s.fieldBombCount, 0, fieldBombCount, 0, Parameter.CHARACTER_NUM);

		for (int i = 0; i < Parameter.CHARACTER_NUM; i++) {
			characters[i] = new Character(s.characters[i]);
		}
		for (Bomb b : s.bombList) {
			bombList.add(new Bomb(b));
		}
		this.nowPutBomb = new HashSet<Integer>(s.nowPutBomb);
	}

	State(String str) {
		{// Input
			Scanner sc = new Scanner(str);

			time = sc.nextLong(); // time
			turn = sc.nextInt(); // turn
			sc.nextInt(); // max_turn
			Parameter.setMyID(sc.nextInt());
			sc.nextInt(); // Y
			sc.nextInt(); // X

			sc.next();
			for (int y = 0; y < Parameter.Y; y++) {
				String line = sc.next();
				for (int x = 0; x < Parameter.X; x++) {
					switch (line.charAt(x + 1)) {
					case '#':
						map[x + y * Parameter.X] = HARD_BLOCK;
						break;
					case '+':
						map[x + y * Parameter.X] = SOFT_BLOCK;
						break;
					case '@':
					case '.':
						map[x + y * Parameter.X] = BLANK;
						break;
					}
				}
			}
			sc.next();
			int characters_num = sc.nextInt();
			for (int i = 0; i < characters_num; i++) {
				characters[i] = new Character(sc.nextInt(), sc.nextInt(), (sc.nextInt() - 1) * Parameter.X
						+ (sc.nextInt() - 1), sc.nextInt(), sc.nextInt());
			}

			int bomb_num = sc.nextInt();
			for (int i = 0; i < bomb_num; i++) {
				int id = sc.nextInt();
				int pos = (sc.nextInt() - 1) * Parameter.X + (sc.nextInt() - 1);
				int limitTime = sc.nextInt();
				int fire = sc.nextInt();
				Bomb b = new Bomb(id, pos, limitTime, fire);

				fieldBombCount[id]++;
				bombList.add(b);
				map[pos] = BOMB;
			}

			int item_num = sc.nextInt();
			Arrays.fill(itemMap, 0);
			int diff = Integer.MAX_VALUE >> 8;
			for (int i = 0; i < item_num; i++) {
				String item_type = sc.next();
				int pos = (sc.nextInt() - 1) * Parameter.X + (sc.nextInt() - 1);
				if (item_type.equals("NUMBER_UP")) {
					map[pos] = NUMBER;
				}
				if (item_type.equals("POWER_UP")) {
					map[pos] = POWER;
				}

				ArrayDeque<Integer> que = new ArrayDeque<Integer>();
				que.add(pos);
				itemMap[pos] = Integer.MAX_VALUE >> 5;

				while (!que.isEmpty()) {
					int q_pos = que.poll();
					for (int dir : dirs) {
						int next_pos = q_pos + dir;
						if (!isin(dir, next_pos) || map[next_pos] > BLANK)
							continue;
						if (itemMap[next_pos] < itemMap[q_pos] - diff) {
							itemMap[next_pos] = itemMap[q_pos] - diff;
							que.add(next_pos);
						}
					}
				}
			}

			sc.close();

			Parameter.println("map");
			for (int y = 0; y < Parameter.Y; y++) {
				for (int x = 0; x < Parameter.X; x++) {
					boolean flag = true;
					for (Character c : characters) {
						if (c.pos == x + y * Parameter.X) {
							if (c.player_id == Parameter.MY_ID)
								Parameter.print("   ap");
							else
								Parameter.print("   ep");
							flag = false;
							break;
						}
					}
					if (flag)
						Parameter.print(String.format("%5d", map[x + y * Parameter.X]));
				}
				Parameter.println();
			}
		}

		calcBurstMap();
		Parameter.println("burstMap");
		for (int y = 0; y < Parameter.Y; y++) {
			for (int x = 0; x < Parameter.X; x++) {
				Parameter.print(String.format("%5d", burstMap[x + y * Parameter.X]));
			}
			Parameter.println();
		}
		liveMap = calcLiveMap();

		if (turn >= 294) {
			int tmpMap[] = new int[Parameter.XY];
			System.arraycopy(map, 0, tmpMap, 0, Parameter.XY);
			for (int q = 0; q < 3; q++) {
				int dy[] = new int[] { 0, 1, 0, -1 };
				int dx[] = new int[] { 1, 0, -1, 0 };
				int x = -1, y = 0, i = 0, j = 1;
				while (true) {
					x += dx[i];
					y += dy[i];
					int pos = y * Parameter.X + x;
					if (tmpMap[pos] != HARD_BLOCK) {
						tmpMap[pos] = HARD_BLOCK;
						mapPosition[pos] = (Integer.MIN_VALUE / 4);
						break;
					}
					if (j > Parameter.X) {
						// 無限ループ回避でとりあえず入れておく
						// jがそこまで大きくならない可能性で実行されない？
						System.err.println("error hard block");
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
	}

	void step() {
		nowPutBomb.clear();
		for (Character c : characters) {
			if (map[c.pos] == NUMBER)
				c.bomb++;
			if (map[c.pos] == POWER)
				c.fire++;
		}
		for (Character c : characters) {
			if (map[c.pos] == NUMBER || map[c.pos] == POWER)
				map[c.pos] = BLANK;
		}

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
				if (map[pos] != HARD_BLOCK) {
					map[pos] = HARD_BLOCK;
					for (int bi = 0; bi < bombList.size(); bi++) {
						if (bombList.get(bi).pos == pos) {
							use[bi] = true;
						}
					}
					break;
				}
				if (j > Parameter.X) {
					// 無限ループ回避でとりあえず入れておく
					// jがそこまで大きくならない可能性で実行されない？
					System.err.println("error hard block");
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

		for (int b1 = 0; b1 < size; b1++) {
			Bomb b = bombList.get(b1);
			if (use[b1])
				continue;
			if (b.limitTime > 0) {
				b.limitTime--;
				continue;
			}
			Queue<Bomb> que = new ArrayDeque<Bomb>();
			que.add(b);
			use[b1] = true;
			attacked[b.pos] = true;
			while (!que.isEmpty()) {
				Bomb bb = que.poll();
				for (int d : dirs) {
					int next_pos = bb.pos;
					for (int j = 0; j < bb.fire; j++) {
						next_pos += d;
						if (!isin(d, next_pos) || map[next_pos] == HARD_BLOCK)
							break;
						attacked[next_pos] = true;
						if (map[next_pos] == SOFT_BLOCK)
							break;
						if (map[next_pos] == BOMB) {
							for (int b2 = 0; b2 < size; b2++) {
								Bomb nb = bombList.get(b2);
								if (next_pos == nb.pos && !use[b2]) {
									use[b2] = true;
									que.add(nb);
								}
							}
						}
					}
				}
			}
		}

		for (Character c : characters)
			if (attacked[c.pos])
				c.dead = true;

		for (int pos = 0; pos < Parameter.XY; pos++) {
			if (attacked[pos] && map[pos] > BLANK)
				map[pos] = BLANK;
		}
		ArrayList<Bomb> next_bl = new ArrayList<Bomb>();
		for (int i = 0; i < size; i++) {
			if (!use[i]) {
				next_bl.add(bombList.get(i));
			} else {
				fieldBombCount[bombList.get(i).id]--;
			}
		}
		bombList = next_bl;
		calcBurstMap();
	}

	private void calcBurstMap() {
		Arrays.fill(burstMap, BURST_MAP_INIT);
		int size = bombList.size();
		boolean use[] = new boolean[size];
		Collections.sort(bombList);
		Queue<Bomb> bq = new ArrayDeque<Bomb>();

		for (int i = 0; i < size; i++) {
			if (use[i])
				continue;
			bq.add(bombList.get(i));
			use[i] = true;
			int limitTime = bombList.get(i).limitTime;
			while (!bq.isEmpty()) {
				Bomb bb = bq.poll();
				burstMap[bb.pos] = Math.min(burstMap[bb.pos], limitTime);

				for (int d : dirs) {
					int next_pos = bb.pos;
					for (int j = 0; j < bb.fire; j++) {
						next_pos += d;
						if (!isin(d, next_pos) || map[next_pos] == HARD_BLOCK)
							break;
						burstMap[next_pos] = Math.min(burstMap[next_pos], limitTime);
						if (map[next_pos] == SOFT_BLOCK)
							break;
						if (map[next_pos] == BOMB) {
							for (int k = 0; k < size; k++) {
								Bomb b = bombList.get(k);
								if (next_pos == b.pos && !use[k]) {
									use[k] = true;
									bq.add(b);
								}
							}
						}
					}
				}
			}
		}
	}

	private int[] calcLiveMap() {
		int liveMap[] = new int[Parameter.XY];
		Arrays.fill(liveMap, MAX_INT);

		for (int pos = 0; pos < Parameter.XY; pos++) {
			if (map[pos] <= BLANK && burstMap[pos] == BURST_MAP_INIT) {
				liveMap[pos] = 0;
			}
		}

		for (int i = 0; i < 20; i++) {
			for (int pos = 0; pos < Parameter.XY; pos++) {
				if (liveMap[pos] == i && map[pos] <= BLANK) {
					for (int dir : dirs) {
						int next_pos = pos + dir;
						if (isin(dir, next_pos) && map[next_pos] <= BOMB && liveMap[pos] < liveMap[next_pos]) {
							liveMap[next_pos] = liveMap[pos] + 1;
						}
					}
				}
			}
		}
		return liveMap;
	}

	private final static int posLen(int p1, int p2) {
		int x1 = p1 % Parameter.X, y1 = p1 / Parameter.X;
		int x2 = p2 % Parameter.X, y2 = p2 / Parameter.X;
		return Math.abs(x1 - x2) + Math.abs(y1 - y2);
	}

	long calcValue() {
		Character c1 = characters[ac1], c2 = characters[ac2];
		return -0xffffff * Math.max(0, 10 - posLen(c1.pos, c2.pos)) - allyDanger + enemyDanger
				+ (long) mapPosition[c1.pos] + c1.bombCount * 0xffffffffL + (long) itemMap[c1.pos]
				+ (long) mapPosition[c2.pos] + c2.bombCount * 0xffffffffL + (long) itemMap[c2.pos];
	}

	long calcFleeValue() {
		Character c1 = characters[ac1], c2 = characters[ac2];
		return -allyDanger + (c1.bombCount == 0 ? 10 : c1.bombCount) * 0xfffff
				+ (c2.bombCount == 0 ? 10 : c2.bombCount) * 0xfffff;
	}

	int operations(Operation[] operations, int player_id) {
		// 移動処理
		for (Character character : characters) {
			if (character.player_id != player_id)
				continue;
			Operation operation = operations[character.id & 1];
			int next_pos = character.pos + operation.move.dir;
			if (!isin(operation.move.dir, next_pos) || map[next_pos] == SOFT_BLOCK || map[next_pos] == HARD_BLOCK
					|| (map[next_pos] == BOMB && next_pos != character.pos && !nowPutBomb.contains(next_pos))) {
				// Parameter.println("移不");
				return 0;
			}
			character.pos = next_pos;
		}

		int now_danger;
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
					// Parameter.println("魔不");
					return 0;
				}
				if (map[character.pos] == BOMB) {
					// Parameter.println("魔重複");
					return 0;
				}
				posBuf[character.id & 1] = character.pos;
				fireBuf[character.id & 1] = character.fire;

				bombList.add(new Bomb(character.id, character.pos, operation.burstTime, character.fire));
				map[character.pos] = BOMB;
				character.bombCount++;
				fieldBombCount[character.id]++;
				nowPutBomb.add(character.pos);
			}
			now_danger = enemyDanger(player_id);
			if (!softBlockBomb(posBuf, fireBuf)) {
				if (baseDanger >= now_danger) {
					// System.out.println(baseDanger + " >= " + danger);
					// Parameter.println("魔無効");
					return -2;
				}
			}
		} else {
			now_danger = enemyDanger(player_id);
		}
		if (player_id == Parameter.MY_ID)
			enemyDanger = now_danger;
		else
			allyDanger = now_danger;

		{// liveDFS
			int memo[][] = new int[Parameter.maxLiveDepth + 1][Parameter.XY];
			for (int i = 0; i < Parameter.maxLiveDepth; i++) {
				Arrays.fill(memo[i], -1);
			}
			int burstMemo[] = new int[Parameter.XY];
			int blockMemo[] = new int[Parameter.XY];

			for (int pos = 0; pos < Parameter.XY; pos++) {
				if (map[pos] == HARD_BLOCK) {
					blockMemo[pos] = -1;
				}
			}
			boolean softBlockClash[] = new boolean[Parameter.XY];
			boolean usedBomb[] = new boolean[Parameter.XY];
			int bombCount = 0, allBombCount = bombList.size();
			int liveDepth;
			for (liveDepth = 0; liveDepth < Parameter.maxLiveDepth && bombCount < allBombCount; liveDepth++) {
				boolean tmpSoftBlockClash[] = new boolean[Parameter.XY];
				for (Bomb bomb : bombList) {
					if (bomb.limitTime == liveDepth && !usedBomb[bomb.pos]) {
						Queue<Bomb> que = new ArrayDeque<Bomb>();
						for (Bomb bomb2 : bombList) {
							if (!bomb.equals(bomb2) && bomb.pos == bomb2.pos && !usedBomb[bomb2.pos]) {
								bombCount++;
								que.add(bomb2);
							}
						}
						usedBomb[bomb.pos] = true;
						bombCount++;
						burstMemo[bomb.pos] |= 1 << liveDepth;
						que.add(bomb);
						while (!que.isEmpty()) {
							Bomb bb = que.poll();
							for (int d : dirs) {
								int next_pos = bb.pos;
								for (int j = 0; j < bb.fire; j++) {
									next_pos += d;
									if (!isin(d, next_pos) || map[next_pos] == HARD_BLOCK)
										break;
									burstMemo[next_pos] |= 1 << liveDepth;
									if (map[next_pos] == SOFT_BLOCK && !softBlockClash[next_pos]) {
										tmpSoftBlockClash[next_pos] = true;
										break;
									}
									if (map[next_pos] == BOMB) {
										for (Bomb bomb2 : bombList) {
											if (next_pos == bomb2.pos && !usedBomb[bomb2.pos]) {
												usedBomb[bomb2.pos] = true;
												bombCount++;
												que.add(bomb2);
											}
										}
									}
								}
							}
						}
					}
				}
				for (Bomb bomb : bombList)
					if (!usedBomb[bomb.pos])
						blockMemo[bomb.pos] |= 1 << liveDepth;
				for (int pos = 0; pos < Parameter.XY; pos++) {
					if (map[pos] == SOFT_BLOCK && !softBlockClash[pos])
						blockMemo[pos] |= 1 << liveDepth;
					softBlockClash[pos] |= tmpSoftBlockClash[pos];
				}
			}

			int deadTime[] = new int[Parameter.CHARACTER_NUM];
			Arrays.fill(deadTime, Integer.MAX_VALUE);
			for (int i = 0; i < Parameter.CHARACTER_NUM; i++) {
				Character character = this.characters[i];
				if ((burstMemo[character.pos] & 1) != 0)
					deadTime[i] = 0;
				else
					deadTime[i] = liveDFS(character.pos, 0, memo, burstMemo, blockMemo, liveDepth);
			}
			int minDeadTime = liveDepth;
			for (int time : deadTime)
				minDeadTime = Math.min(minDeadTime, time);
			if (minDeadTime < liveDepth) {
				int allyDead = 0, enemyDead = 0;
				for (int i = 0; i < Parameter.CHARACTER_NUM; i++)
					if (deadTime[i] == minDeadTime) {
						Parameter.println("dead id: " + characters[i].id);
						if (characters[i].player_id == player_id)
							allyDead++;
						else
							enemyDead++;
					}
				Parameter.println(player_id + " " + minDeadTime + " " + liveDepth + " " + allyDead + " " + enemyDead);
				if (allyDead > 0 && allyDead == enemyDead) {
					Parameter.println("相打");
					return 1;
				}
				if (allyDead > enemyDead) {
					Parameter.println("自詰");
					return -1;
				}
				if (allyDead < enemyDead) {
					Parameter.println("相詰");
					return 3;
				}
			}
		}

		// Parameter.println("正常");
		return 2;
	}

	private int liveDFS(int pos, int depth, int memo[][], int burstMemo[], int blockMemo[], int liveDepth) {
		if (memo[depth][pos] != -1)
			return memo[depth][pos];
		if (depth == liveDepth)
			return memo[depth][pos] = liveDepth;
		int bit = 1 << (depth + 1);
		int res = depth;
		for (int d : dirs) {
			int next_pos = pos + d;
			if (!isin(d, next_pos) || (burstMemo[next_pos] & bit) != 0 || ((blockMemo[next_pos] & bit) != 0 && d != 0))
				continue;
			res = Math.max(res, liveDFS(next_pos, depth + 1, memo, burstMemo, blockMemo, liveDepth));
			if (res == liveDepth) {
				return memo[depth][pos] = res;
			}
		}
		return memo[depth][pos] = res;
	}

	private int enemyDanger(int player_ip) {
		calcBurstMap();
		int res = 0;
		for (Character c : characters) {
			if (c.player_id == player_ip)
				continue;

			int enemyMap[] = new int[Parameter.XY];
			ArrayDeque<Integer> que = new ArrayDeque<Integer>();
			enemyMap[c.pos] = 4;
			que.add(c.pos);
			while (!que.isEmpty()) {
				int now_pos = que.poll();
				for (int d : dirs) {
					int next_pos = now_pos + d;
					if (isin(d, next_pos) && map[next_pos] <= BLANK && enemyMap[next_pos] < enemyMap[now_pos]) {
						enemyMap[next_pos] = enemyMap[now_pos] - 1;
						if (enemyMap[next_pos] > 2) {
							que.add(next_pos);
						}
					}
				}
			}
			int enemyDanger = 0;
			for (int p = 0; p < Parameter.XY; p++) {
				if (enemyMap[p] > 0)
					enemyDanger -= 0xffff;
			}
			for (int p = 0; p < Parameter.XY; p++) {
				enemyDanger += enemyMap[p] * Math.max(10 - burstMap[p], 0);
			}
			res += enemyDanger;
		}
		return res;
	}

	private boolean softBlockBomb(int[] pos, int fire[]) {
		int res = 0;
		for (int i = 0; i < 2; i++) {
			if (fire[i] == 0) {
				res |= 1 << i;
				continue;
			}
			int posi = pos[i];
			int firei = fire[i];

			base: for (int d : dirs) {
				int next_pos = posi;
				for (int j = 0; j < firei; j++) {
					next_pos += d;
					if (!isin(d, next_pos) || map[next_pos] == HARD_BLOCK)
						break;
					if (map[next_pos] == SOFT_BLOCK) {
						res |= 1 << i;
						break base;
					}
				}
			}
		}
		return res == 0x3;
	}

	public long getHash() {
		long res = 0;
		for (int pos = 0; pos < Parameter.XY; pos++) {
			res ^= Hash.hashMap[Math.max(0, map[pos] - 2) * Parameter.XY + pos];
			res ^= Hash.hashBomb[Math.min(burstMap[pos], 20) * Parameter.XY + pos];
		}
		for (Character c : characters) {
			res ^= Hash.hashPlayer[c.id * Parameter.XY + c.pos];
		}
		return res;
	}
}