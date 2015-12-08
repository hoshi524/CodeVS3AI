package codevs3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import codevs3.State.Result;

public class AI {

	public static void main(String[] args) {
		try (Scanner sc = new Scanner(System.in)) {
			AI ai = new AI();
			System.out.println("hoshi524");
			while (true) {
				StringBuilder sb = new StringBuilder();
				while (true) {
					String tmp = sc.nextLine();
					sb.append(tmp).append("\n");
					if (tmp.equals("END")) break;
				}
				String input = sb.toString();
				System.out.print(ai.think(input));
			}
		}
	}

	static final int MAX_VALUE = Integer.MAX_VALUE >> 10;
	static final int MIN_VALUE = Integer.MIN_VALUE >> 10;
	static final int MAX_DEPTH = 7;
	static int AiutiValue;

	// 何故か順番に依存してて変更できない
	final static Operation operations[] = { //
	new Operation(Move.NONE, false, 5), //	0
			new Operation(Move.DOWN, false, 5), //	1
			new Operation(Move.LEFT, false, 5), //	2
			new Operation(Move.RIGHT, false, 5), //	3
			new Operation(Move.UP, false, 5), //	4
			new Operation(Move.DOWN, true, 5), //	5
			new Operation(Move.LEFT, true, 5), //	6
			new Operation(Move.RIGHT, true, 5), //	7
			new Operation(Move.UP, true, 5), //		8
			new Operation(Move.NONE, true, 5), //	9
	};

	static final Operation[][] operationList;

	static {
		class innerfunc {
			void operation_dfs(int n, Operation[] now, ArrayList<Operation[]> res) {
				if (Parameter.PLAYER == n) {
					res.add(Arrays.copyOf(now, now.length));
				} else {
					for (Operation operation : operations) {
						now[n] = operation;
						operation_dfs(n + 1, now, res);
					}
				}
			}
		}
		ArrayList<Operation[]> list = new ArrayList<>();
		new innerfunc().operation_dfs(0, new Operation[Parameter.PLAYER], list);
		operationList = list.toArray(new Operation[0][]);
	}

	// DebugDFS debug = new DebugDFS();

	public String think(String input) {
		long start = System.currentTimeMillis();
		table.clear();
		State state = new State(input);
		Next next = MTDF(state);
		// Next next = negamax(state, MAX_DEPTH, MIN_VALUE, MAX_VALUE);
		if (false) {
			table.clear();
			Next test = negamax(state, MAX_DEPTH, MIN_VALUE, MAX_VALUE);
			if (test.value != next.value) {
				debug(next.value, next.operations);
				debug(test.value, test.operations);
			}
		}
		debug(state.turn, next.value, System.currentTimeMillis() - start);

		StringBuilder sb = new StringBuilder();
		{
			Operation[] o = next.operations == null ? operationList[0] : next.operations;
			for (int i = 0; i < Parameter.PLAYER; ++i) {
				sb.append(o[i].toString()).append("\n");
			}
		}
		Timer.print();
		// debug.print();
		Counter.print();
		return sb.toString();
	}

	Next MTDF(State now) {
		int lower = MIN_VALUE, upper = MAX_VALUE, bound = 0;
		Next n = new Next();
		n.value = MIN_VALUE;
		while (lower < upper) {
			n = negamax(now, MAX_DEPTH, bound - 1, bound);
			if (n.value < bound) upper = n.value;
			else lower = n.value;
			if (lower == n.value) bound = n.value + 1;
			else bound = n.value;
		}
		return n;
	}

	TranspositionTable table = new TranspositionTable();

	Next negamax(State now, int depth, int alpha, int beta) {
		boolean bad[][] = new boolean[2][operations.length];
		int enemyMap[] = now.getEnemyMap(Parameter.MY_ID);
		int id0 = State.ID[Parameter.MY_ID][0];
		int id1 = State.ID[Parameter.MY_ID][1];
		for (int i = 0; i < operations.length; ++i) {
			bad[0][i] = now.operations_check(operations[i], id0, enemyMap);
			bad[1][i] = now.operations_check(operations[i], id1, enemyMap);
		}

		long key = now.getHash() ^ Hash.hashDepth[depth];
		Next best = table.get(key);
		if (best != null) {
			if (beta <= best.lower || best.upper <= alpha || best.upper == best.lower) return best;
			alpha = Math.max(alpha, best.lower);
			beta = Math.min(beta, best.upper);
		} else {
			best = table.create(key);
		}
		best.value = MIN_VALUE;

		for (int i = 0; i < operationList.length; ++i) {
			if (bad[0][i / operations.length] || bad[1][i % operations.length]) continue;
			Operation o[] = operationList[i];
			int value = aite(new State(now), depth - 1, alpha, beta, o).value;
			if (best.value < value) {
				best.value = value;
				best.operations = o;
				alpha = Math.max(alpha, best.value);
				if (best.value >= beta) break;
			}
		}
		if (best.value <= alpha) best.upper = best.value;
		else if (best.value >= beta) best.lower = best.value;
		else best.lower = best.upper = best.value;
		return best;
	}

	Next aite(State now, int depth, int alpha, int beta, Operation[] ally) {
		boolean bad[][] = new boolean[2][operations.length];
		int enemyMap[] = now.getEnemyMap(Parameter.ENEMY_ID);
		int id0 = State.ID[Parameter.ENEMY_ID][0];
		int id1 = State.ID[Parameter.ENEMY_ID][1];
		for (int i = 0; i < operations.length; ++i) {
			bad[0][i] = now.operations_check(operations[i], id0, enemyMap);
			bad[1][i] = now.operations_check(operations[i], id1, enemyMap);
		}

		now.operations(ally[0], State.ID[Parameter.MY_ID][0]);
		now.operations(ally[1], State.ID[Parameter.MY_ID][1]);
		long key = now.getHash() ^ Hash.hashDepth[depth];
		Next best = table.get(key);
		if (best != null) {
			if (beta <= best.lower || best.upper <= alpha || best.upper == best.lower) return best;
			alpha = Math.max(alpha, best.lower);
			beta = Math.min(beta, best.upper);
		} else {
			best = table.create(key);
		}
		best.value = MAX_VALUE;

		for (int i = 0; i < operationList.length; ++i) {
			if (bad[0][i / operations.length] || bad[1][i % operations.length]) continue;
			Operation o[] = operationList[i];
			State tmp = new State(now);
			tmp.operations(o[0], id0);
			tmp.operations(o[1], id1);
			int value = 0;
			if (depth == 0 || tmp.anyDead()) {
				Result res = tmp.getResult();
				if (res == Result.Continue) value = tmp.value();
				else if (res == Result.Draw) value = tmp.value() + AiutiValue;
				else if (res == Result.Win) {
					value = tmp.win() + (MAX_VALUE >> 1);
				} else if (res == Result.Lose) {
					value = tmp.lose() + (MIN_VALUE >> 1);
				}
			} else {
				tmp.step();
				value = negamax(tmp, depth - 1, alpha, beta).value;
			}
			if (best.value > value) {
				best.value = value;
				beta = Math.min(beta, best.value);
				if (alpha >= best.value) break;
			}
		}
		if (best.value <= alpha) best.upper = best.value;
		else if (best.value >= beta) best.lower = best.value;
		else best.lower = best.upper = best.value;
		return best;
	}

	final static void debug(final Object... obj) {
		System.err.println(Arrays.deepToString(obj));
	}
}
