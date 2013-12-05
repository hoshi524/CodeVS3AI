package codevs3AI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

class AI {

	public static void main(String[] args) {
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);
		AI ai = new AI();
		System.out.println("TEAM-LAB");
		while (true) {
			StringBuilder sb = new StringBuilder();
			while (true) {
				String tmp = sc.nextLine();
				sb.append(tmp + "\n");
				if (tmp.equals("END"))
					break;
			}
			System.out.print(ai.think(sb.toString()));
		}
	}

	static final Operation NONE = new Operation(Move.NONE, false, 5);
	static final int MAX_DEPTH = 1;

	static final ArrayList<Operation[]> operationList = new ArrayList<Operation[]>();

	// 何故か順番に依存してて変更できない
	final static Operation operations[] = { NONE,//0
			new Operation(Move.DOWN, false, 5),//1
			new Operation(Move.LEFT, false, 5),//2
			new Operation(Move.RIGHT, false, 5),//3
			new Operation(Move.UP, false, 5),//4
			new Operation(Move.DOWN, true, 5),//5
			new Operation(Move.LEFT, true, 5),//6
			new Operation(Move.RIGHT, true, 5),//7
			new Operation(Move.UP, true, 5),//8
			new Operation(Move.NONE, true, 5),//9
	};

	static {
		operation_dfs(0, new Operation[Parameter.PLAYER], operationList);
	}

	static void operation_dfs(int character_num, Operation[] now, ArrayList<Operation[]> res) {
		if (Parameter.PLAYER == character_num) {
			Operation[] push = new Operation[now.length];
			for (int i = 0; i < now.length; i++) {
				push[i] = now[i];
			}
			res.add(push);
			return;
		}

		for (Operation operation : operations) {
			now[character_num] = operation;
			operation_dfs(character_num + 1, now, res);
		}
	}

	String think(String input) {
		State state = new State(input);
		Next next;
		if (State.time < 50000)
			next = dfs(state, 0, Long.MIN_VALUE + Integer.MAX_VALUE, Long.MAX_VALUE - Integer.MAX_VALUE);
		else
			next = dfs(state, MAX_DEPTH, Long.MIN_VALUE + Integer.MAX_VALUE, Long.MAX_VALUE - Integer.MAX_VALUE);
		System.err.println(state.turn + " : " + next.value);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Parameter.PLAYER; i++) {
			sb.append(next.operations[i].toString() + "\n");
		}
		return sb.toString();
	}

	HashSet<Long> used = new HashSet<Long>();

	// @SuppressWarnings("unused")
	Next dfs(State now, int depth, long a, long b) {
		Next best = new Next(a, operationList.get(0));

		for (Operation[] allyOperations : operationList) {
			State tmp = new State(now);
			int res = tmp.operations(allyOperations, Parameter.MY_ID);
			if (res == 0 || res == -1 || res == -2)
				continue;
			Next next = new Next(enemyOperation(tmp, depth, best.value, b), allyOperations);
			for (Operation o : allyOperations)
				if (o.equals(NONE))
					next.value -= 0xffffff;
			if (Parameter.DEBUG) {
				debug = MAX_DEPTH == depth;
				Parameter.print(depth + " ");
				for (Operation o : allyOperations)
					Parameter.print(o.toString() + " ");
				Parameter.println(next.value + " ");
				debug = false;
			}
			if (best.value < next.value && (MAX_DEPTH != depth || best.value == Long.MIN_VALUE || !used.contains(tmp.getHash()))) {
				if (MAX_DEPTH == depth)
					Parameter.println("update");
				best = next;
			}
			if (best.value >= b)
				break;
		}
		if (MAX_DEPTH == depth) {
			State tmp = new State(now);
			tmp.operations(best.operations, Parameter.MY_ID);
			used.add(tmp.getHash());
		}
		return best;
	}

	static boolean debug = false;

	long enemyOperation(State now, int depth, long a, long b) {
		long value = b;
		boolean flag = true;
		boolean aiuti = true, kati = true;
		ArrayList<State> hutuuList = new ArrayList<State>();
		ArrayList<State> tumiList = new ArrayList<State>();
		for (Operation[] enemyOperations : operationList) {
			State tmp = new State(now);

			int res = tmp.operations(enemyOperations, Parameter.ENEMY_ID);
			if (res == 0 || res == -2) {
				// 不正な行動
				// 魔法が有効じゃない
				// 自分のキャラクターが死んだ
				continue;
			}
			tmp.step();
			int nowAllyDead = 0, nowEnemyDead = 0;
			for (Character c : tmp.characters)
				if (c.dead)
					if (c.player_id == Parameter.MY_ID)
						nowAllyDead++;
					else
						nowEnemyDead++;
			aiuti &= nowAllyDead > 0 && nowEnemyDead >= nowAllyDead;
			kati &= nowEnemyDead > nowAllyDead;
			if (nowAllyDead > nowEnemyDead) {
				return (Long.MIN_VALUE / 2) - Integer.MAX_VALUE;
			}

			if (res == 2) {
				// どっちも詰んでない
				if (depth == 0) {
					value = Math.min(value, tmp.calcValue());
				} else {
					hutuuList.add(tmp);
				}
			} else if (res == 1) {
				// 相打ち
				if (depth == 0 || (nowAllyDead > 0 && nowAllyDead == nowEnemyDead))
					value = Math.min(value, tmp.calcFleeValue() + (Long.MIN_VALUE / 4));
				else
					hutuuList.add(tmp);
			} else if (res == 3) {
				// 自分が詰んだ
				value = Math.min(value, tmp.calcFleeValue() + (Long.MIN_VALUE / 2));
				flag = false;
			} else if (res == -1) {
				// 相手が詰んだ
				if (depth == 0 || nowEnemyDead > 0) {
					value = Math.min(value, tmp.calcFleeValue() + (Long.MAX_VALUE / 2));
				} else {
					tumiList.add(tmp);
				}
			}
		}
		if (!kati && aiuti) {
			return (Long.MIN_VALUE / 4) - Integer.MAX_VALUE;
		}
		if (flag) {
			if (hutuuList.size() > 0) {
				for (State state : hutuuList) {
					value = Math.min(value, dfs(state, depth - 1, a, value).value);
					if (a >= value) {
						return value;
					}
				}
			} else if (tumiList.size() > 0) {
				for (State state : tumiList) {
					value = Math.min(value, dfs(state, depth - 1, a, value).value);
					if (a >= value) {
						return value;
					}
				}
			}
		}
		return value;
	}
}
