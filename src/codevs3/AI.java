package codevs3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import codevs3.State.Result;
import codevs3.TranspositionTable.Node;

public class AI {

	public static void main(String[] args) {
		try (Scanner sc = new Scanner(System.in)) {
			AI ai = new AI();
			System.out.println("hoshi524");
			// next.AI test = new next.AI();
			// long s0 = 0, s1 = 0;
			while (true) {
				StringBuilder sb = new StringBuilder();
				while (true) {
					String tmp = sc.nextLine();
					sb.append(tmp).append("\n");
					if (tmp.equals("END")) break;
				}
				String input = sb.toString(), output;
				// long t0 = System.nanoTime();
				output = ai.think(input);
				//				long t1 = System.nanoTime();
				//				output = test.think(input);
				//				long t2 = System.nanoTime();
				//				s0 += t1 - t0;
				//				s1 += t2 - t1;
				//				System.err.println(s0 + " " + s1);
				System.out.print(output);
			}
		}
	}

	static final int MAX_VALUE = Integer.MAX_VALUE >> 10;
	static final int MIN_VALUE = Integer.MIN_VALUE >> 10;
	static int MAX_DEPTH = 7; // 奇数制約
	static int AiutiValue;

	static final Operation[][] operationList;

	// 何故か順番に依存してて変更できない
	final static Operation operations[] = {//
	new Operation(Move.NONE, false, 5), //			0
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
		for (int i = 0; i <= MAX_DEPTH; ++i)
			table[i].delete();
		State state = new State(input);
		Next next = MTDF(state);
		// Next next = negamax(state, MAX_DEPTH, MIN_VALUE, MAX_VALUE);
		if (false) {
			for (int i = 0; i <= MAX_DEPTH; ++i)
				table[i].delete();
			Next test = negamax(state, MAX_DEPTH, MIN_VALUE, MAX_VALUE);
			if (test.value != next.value) {
				debug(next.value, next.operations);
				debug(test.value, test.operations);
			}
		}
		debug(state.turn, next.value);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Parameter.PLAYER; ++i) {
			sb.append(next.operations[i].toString()).append("\n");
		}
		Timer.print();
		// debug.print();
		return sb.toString();
	}

	Next MTDF(State now) {
		int lower = MIN_VALUE, upper = MAX_VALUE, bound = 0;
		Next n = new Next(MIN_VALUE, operationList[0]);
		while (lower < upper) {
			n = negamax(now, MAX_DEPTH, bound - 1, bound);
			if (n.value < bound) upper = n.value;
			else lower = n.value;
			if (lower == n.value) bound = n.value + 1;
			else bound = n.value;
		}
		return n;
	}

	TranspositionTable table[] = new TranspositionTable[MAX_DEPTH + 1];
	{
		for (int i = 0; i <= MAX_DEPTH; ++i) {
			table[i] = new TranspositionTable();
		}
	}

	Next negamax(State now, int depth, int alpha, int beta) {
		boolean isMe = depth % 2 == 1;
		Next best = new Next(isMe ? MIN_VALUE : MAX_VALUE, operationList[0]);
		long key = now.getHash();
		Node memo = table[depth].get(key);
		if (memo != null) {
			if (beta <= memo.lower) return new Next(memo.lower, memo.next.operations);
			if (memo.upper <= alpha || memo.upper == memo.lower) return new Next(memo.upper, memo.next.operations);
			alpha = Math.max(alpha, memo.lower);
			beta = Math.min(beta, memo.upper);
		} else {
			memo = table[depth].create(key);
		}
		if (isMe) {
			for (Operation[] o : operationList) {
				State tmp = new State(now);
				if (tmp.operations(o, Parameter.MY_ID)) {
					// debug.addNode(Arrays.deepToString(new Object[] { MAX_DEPTH - depth, o }), MAX_DEPTH - depth);
					Next n = new Next(negamax(tmp, depth - 1, alpha, beta).value, o);
					if (best.value < n.value) {
						best = n;
						if (best.value >= beta) break;
						alpha = Math.max(alpha, best.value);
					}
				}
			}
		} else {
			for (Operation[] o : operationList) {
				State tmp = new State(now);
				if (tmp.operations(o, Parameter.ENEMY_ID)) {
					// debug.addNode(Arrays.deepToString(new Object[] { MAX_DEPTH - depth, o, tmp.getResult() }), MAX_DEPTH - depth);
					if (depth == 0 || tmp.anyDead()) {
						Result res = tmp.getResult();
						int value = 0;
						if (res == Result.Continue) value = tmp.value();
						else if (res == Result.Draw) value = tmp.value() + AiutiValue;
						else if (res == Result.Win) value = tmp.value() + (MAX_VALUE >> 1);
						else if (res == Result.Lose) value = tmp.value() + (MIN_VALUE >> 1);
						best.value = Math.min(best.value, value);
					} else {
						tmp.step();
						Next n = new Next(negamax(tmp, depth - 1, alpha, beta).value, o);
						if (best.value > n.value) {
							best = n;
							if (alpha >= best.value) break;
							beta = Math.min(beta, best.value);
						}
					}
				}
			}
		}
		memo.next = best;
		if (best.value <= alpha) memo.upper = best.value;
		else if (best.value >= beta) memo.lower = best.value;
		else memo.lower = memo.upper = best.value;
		return best;
	}

	final static void debug(final Object... obj) {
		System.err.println(Arrays.deepToString(obj));
	}
}