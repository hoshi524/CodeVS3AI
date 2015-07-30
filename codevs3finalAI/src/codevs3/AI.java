package codevs3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

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
	static final int MAX_DEPTH = 5; // 奇数制約

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
		System.err.println(String.format("%3d:%12d", state.turn, next.value));

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Parameter.PLAYER; ++i) {
			sb.append(next.operations[i].toString()).append("\n");
		}
		Timer.print();
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

	// DebugDFS test = new DebugDFS();

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
		class Piar {
			final int value;
			final State s;
			final Operation[] o;

			Piar(int value, State s, Operation[] o) {
				this.value = value;
				this.s = s;
				this.o = o;
			}
		}
		Piar[] moves = new Piar[128];
		int msize = 0;
		if (isMe) {
			for (Operation[] o : operationList) {
				State tmp = new State(now);
				int res = tmp.operations(o, Parameter.MY_ID);
				if (res == 0) continue;
				// test.addNode(Arrays.deepToString(new Object[] { depth, operations, res }), MAX_DEPTH - depth);
				int value = 0;
				if (res == 2) value = tmp.calcValue();
				else if (res == 1) value = tmp.calcValue() + State.AiutiValue;
				else if (res == 3) value = tmp.calcValue() + (MIN_VALUE >> 1);
				else if (res == -1) value = tmp.calcValue() + (MAX_VALUE >> 1);
				moves[msize++] = new Piar(value, tmp, o);
			}
			moves = Arrays.copyOf(moves, msize);
			Arrays.sort(moves, (o1, o2) -> o2.value - o1.value);
			for (Piar p : moves) {
				Next n = new Next(negamax(p.s, depth - 1, alpha, beta).value, p.o);
				if (best.value < n.value) {
					best = n;
					if (best.value >= beta) break;
					alpha = Math.max(alpha, best.value);
				}
			}
		} else {
			for (Operation[] o : operationList) {
				State tmp = new State(now);
				int res = tmp.operations(o, Parameter.ENEMY_ID);
				if (res == 0) continue;
				// test.addNode(Arrays.deepToString(new Object[] { depth, operations, res }), MAX_DEPTH - depth);
				boolean anyDead = tmp.step();
				int value = 0;
				if (res == 2) value = tmp.calcValue();
				else if (res == 1) value = tmp.calcValue() + State.AiutiValue;
				else if (res == 3) value = tmp.calcValue() + (MIN_VALUE >> 1);
				else if (res == -1) value = tmp.calcValue() + (MAX_VALUE >> 1);
				if (anyDead) {
					best.value = Math.min(best.value, value);
				} else {
					moves[msize++] = new Piar(value, tmp, o);
				}
			}
			moves = Arrays.copyOf(moves, msize);
			if (depth == 0) {
				for (Piar p : moves)
					best.value = Math.min(best.value, p.value);
			} else if (alpha < best.value) {
				Arrays.sort(moves, (o1, o2) -> o1.value - o2.value);
				beta = Math.min(beta, best.value);
				for (Piar p : moves) {
					Next n = new Next(negamax(p.s, depth - 1, alpha, beta).value, p.o);
					if (best.value > n.value) {
						best = n;
						if (alpha >= best.value) break;
						beta = Math.min(beta, best.value);
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
