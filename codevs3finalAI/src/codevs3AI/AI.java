package codevs3AI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

class AI {

	public static void main(String[] args) {
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);
		AI ai = new AI();
		System.out.println("hoshi524");
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
	static final int MAX_DEPTH = 3;

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

	long start;
	int dfsCount;

	String think(String input) {
		start = System.nanoTime();
		already.clear();
		State state = new State(input);
		dfsCount = 0;

		Next next = dfs(state, MAX_DEPTH, Long.MIN_VALUE, Long.MAX_VALUE);
//		Next test = mtdf(state);
//		if (next.value != test.value) {
//			System.err.println(next.value + " != " + test.value);
//		}
		System.err.println(String.format("%3d : %15d %6d", state.turn, next.value, dfsCount));
		state.operations(next.operations, Parameter.MY_ID, MAX_DEPTH);
		used.add(state.getHash());

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Parameter.PLAYER; i++) {
			sb.append(next.operations[i].toString() + "\n");
		}
		return sb.toString();
	}

	Next mtdf(State now) {
		already.clear();
		long lower = Long.MIN_VALUE + Integer.MAX_VALUE;
		long upper = Long.MAX_VALUE + Integer.MIN_VALUE;
		long bound = (upper + lower) / 2;
		Next next = new Next(lower, operationList.get(0));
		while (lower + 2 < upper && upper > Long.MIN_VALUE / 2 + Integer.MAX_VALUE) {
			next = dfs(now, MAX_DEPTH, bound - 1, bound);
			if (next.value < bound)
				upper = next.value;
			else
				lower = next.value;
			bound = (upper + lower) / 2;
		}
		return next;
	}

	HashSet<Long> used = new HashSet<Long>();

	class Already {
		final Next next;
		final long lower, upper;

		public Already(Next next, long lower, long upper) {
			this.next = next;
			this.lower = lower;
			this.upper = upper;
		}
	}

	HashMap<Long, Already> already = new HashMap<>();

	Next dfs(State now, int depth, long alpha, long beta) {
		dfsCount++;
		long stateHash = now.getHash();
		long lower, upper;
		if (already.containsKey(stateHash)) {
			Already r = already.get(stateHash);
			lower = r.lower;
			upper = r.upper;
			if (lower >= beta || upper <= alpha || lower == upper)
				return r.next;
			alpha = Math.max(alpha, lower);
			beta = Math.min(beta, upper);
		} else {
			lower = Long.MIN_VALUE;
			upper = Long.MAX_VALUE;
		}
		Next best = new Next(alpha, operationList.get(0));

		for (Operation[] allyOperations : operationList) {
			State tmp = new State(now);
			int res = tmp.operations(allyOperations, Parameter.MY_ID, depth);
			if (res == 0 || res == -1 || res == -2)
				continue;
			Next next = new Next(enemyOperation(tmp, depth, best.value, beta), allyOperations);
			if (Parameter.DEBUG && MAX_DEPTH == depth) {
				Parameter.print(depth + " ");
				for (Operation o : allyOperations)
					Parameter.print(o.toString() + " ");
				Parameter.println(next.value + " ");
			}
			if (best.value < next.value
					&& (MAX_DEPTH != depth || best.value == Long.MIN_VALUE || !used.contains(tmp.getHash()))) {
				if (MAX_DEPTH == depth)
					Parameter.println("update");
				best = next;
			}
			if (best.value >= beta)
				break;
		}
		if (best.value <= alpha)
			already.put(stateHash, new Already(best, lower, best.value));
		else if (best.value >= beta)
			already.put(stateHash, new Already(best, best.value, upper));
		else
			already.put(stateHash, new Already(best, best.value, best.value));
		return best;
	}

	long enemyOperation(State now, int depth, long alpha, long beta) {
		long value = beta;
		boolean search = true;
		ArrayList<State> hutuuList = new ArrayList<>();
		ArrayList<State> tumiList = new ArrayList<>();
		boolean timeover = 1 <= (MAX_DEPTH - depth) && (System.nanoTime() - start) > 1200000000L;

		for (Operation[] enemyOperations : operationList) {
			State tmp = new State(now);

			int res = tmp.operations(enemyOperations, Parameter.ENEMY_ID, depth);
			if (res == 0 || res == -2) {
				// 不正な行動
				// 魔法が有効じゃない
				continue;
			}
			tmp.step();
			if (res == 2) {
				// どっちも詰んでない
				if (depth == 0 || timeover) {
					value = Math.min(value, tmp.calcValue());
				} else {
					hutuuList.add(tmp);
				}
			} else if (res == 1) {
				// 相打ち
				search = false;
				value = Math.min(value, tmp.calcFleeValue() + State.AiutiValue);
			} else if (res == 3) {
				// 自分が詰んだ
				return Math.min(value, tmp.calcFleeValue() + Long.MIN_VALUE / 4);
			} else if (res == -1) {
				// 相手が詰んだ
				if (depth == 0 || dead(tmp.characters, Parameter.ENEMY_ID) > 0 || timeover) {
					value = Math.min(value, tmp.calcFleeValue() + Long.MAX_VALUE / 4);
				} else {
					tumiList.add(tmp);
				}
			}
		}
		if (search) {
			if (hutuuList.size() > 0) {
				for (State state : hutuuList) {
					value = Math.min(value, dfs(state, depth - 1, alpha, value).value);
					if (alpha >= value) {
						return value;
					}
				}
			} else if (tumiList.size() > 0) {
				for (State state : tumiList) {
					value = Math.min(value, dfs(state, depth - 1, alpha, value).value);
					if (alpha >= value) {
						return value;
					}
				}
			}
		}
		return value;
	}

	private final int dead(Character[] characters, int player_id) {
		int dead = 0;
		for (Character c : characters)
			if (c.dead && c.player_id == player_id)
				dead++;
		return dead;
	}
}
