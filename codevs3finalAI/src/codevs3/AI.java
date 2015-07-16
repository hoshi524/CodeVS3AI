package codevs3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

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

	static final int MAX_VALUE = Integer.MAX_VALUE - (Integer.MAX_VALUE >> 2);
	static final int MIN_VALUE = Integer.MIN_VALUE - (Integer.MIN_VALUE >> 2);
	static final int MAX_DEPTH = 3; // 奇数制約

	static final Operation[][] operationList;

	// 何故か順番に依存してて変更できない
	final static Operation operations[] = {//
	new Operation(Move.NONE, false, 5), //0
			new Operation(Move.DOWN, false, 5), //1
			new Operation(Move.LEFT, false, 5), //2
			new Operation(Move.RIGHT, false, 5), //3
			new Operation(Move.UP, false, 5), //4
			new Operation(Move.DOWN, true, 5), //5
			new Operation(Move.LEFT, true, 5), //6
			new Operation(Move.RIGHT, true, 5), //7
			new Operation(Move.UP, true, 5), //8
			new Operation(Move.NONE, true, 5), //9
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
			already[i].clear();
		State state = new State(input);
		Next next = MTDF(state);
		// Next next = negamax(state, MAX_DEPTH, MIN_VALUE, MAX_VALUE);
		if (false) {
			Next test = negamax(state, MAX_DEPTH, MIN_VALUE, MAX_VALUE);
			if (test.value != next.value) {
				debug(next.value, next.operations);
				debug(test.value, test.operations);
			}
		}
		System.err.println(String.format("%3d : %12d", state.turn, next.value));

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Parameter.PLAYER; ++i) {
			sb.append(next.operations[i].toString()).append("\n");
		}
		Timer.print();
		return sb.toString();
	}

	/*
	 * 何かバグらせていそう
	 * negamax(state, MAX_DEPTH, MIN_VALUE, MAX_VALUE, true)
	 * と結果が一致して性能が良いことをテストしないと・・・
	 */
	Next MTDF(State now) {
		int lower = MIN_VALUE;
		int upper = MAX_VALUE;
		int bound = 0;
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

	class Already {
		Next next;
		int lower = MIN_VALUE, upper = MAX_VALUE;
	}

	@SuppressWarnings("unchecked")
	HashMap<Long, Already> already[] = new HashMap[MAX_DEPTH + 1];
	{
		for (int i = 0; i <= MAX_DEPTH; ++i) {
			already[i] = new HashMap<>();
		}
	}

	// DebugDFS test = new DebugDFS();

	Next negamax(State now, int depth, int alpha, int beta) {
		boolean isMe = depth % 2 == 1;
		Next best = new Next(isMe ? MIN_VALUE : MAX_VALUE, operationList[0]);
		long key = now.getHash();
		Already memo = already[depth].get(key);
		if (memo != null) {
			if (beta < memo.lower) return new Next(memo.lower, memo.next.operations);
			if (memo.upper < alpha) return new Next(memo.upper, memo.next.operations);
			alpha = Math.max(alpha, memo.lower);
			beta = Math.min(beta, memo.upper);
		} else {
			memo = new Already();
			already[depth].put(key, memo);
		}
		if (isMe) {
			for (Operation[] operations : operationList) {
				State tmp = new State(now);
				int res = tmp.operations(operations, Parameter.MY_ID, depth);
				// test.addNode(Arrays.deepToString(new Object[] { depth, operations, res }), MAX_DEPTH - depth);
				if (res == 0 || res == -2) continue;
				Next n = new Next(negamax(tmp, depth - 1, alpha, beta).value, operations);
				if (best.value < n.value) {
					best = n;
					if (best.value >= beta) break;
					alpha = Math.max(alpha, best.value);
				}
			}
		} else {
			ArrayList<State> hutuuList = new ArrayList<>();
			ArrayList<State> winList = new ArrayList<>();
			ArrayList<State> loseList = new ArrayList<>();

			for (Operation[] operations : operationList) {
				State tmp = new State(now);
				int res = tmp.operations(operations, Parameter.ENEMY_ID, depth);
				if (res == 0 || res == -2) continue;
				// test.addNode(Arrays.deepToString(new Object[] { depth, operations, res }), MAX_DEPTH - depth);
				tmp.step();
				// int allyDead = dead(tmp.characters, Parameter.MY_ID), enemyDead = dead(tmp.characters, Parameter.ENEMY_ID);
				// if (depth == 0 || allyDead > 0 || enemyDead > 0) {
				if (res == 2) {
					if (depth == 0) {
						best.value = Math.min(best.value, tmp.calcValue());
					} else {
						hutuuList.add(tmp);
					}
				} else if (res == 1) {
					if (depth == 0 || dead(tmp.characters, Parameter.ENEMY_ID) > 0) {
						best.value = Math.min(best.value, tmp.calcFleeValue() + State.AiutiValue);
					} else {
						hutuuList.add(tmp);
					}
				} else if (res == 3) {
					if (depth == 0 || dead(tmp.characters, Parameter.MY_ID) > 0) {
						best.value = Math.min(best.value, tmp.calcFleeValue() + (MIN_VALUE >> 2));
					} else {
						loseList.add(tmp);
					}
				} else if (res == -1) {
					if (depth == 0 || dead(tmp.characters, Parameter.ENEMY_ID) > 0) {
						best.value = Math.min(best.value, tmp.calcFleeValue() + (MAX_VALUE >> 2));
					} else {
						winList.add(tmp);
					}
				}
				//				} else {
				//					Next n = negamax(tmp, depth - 1, alpha, beta);
				//					if (best.value > n.value) {
				//						best = n;
				//						if (alpha >= best.value) break;
				//						beta = Math.min(beta, best.value);
				//					}
				//				}
			}
			if (depth > 0) {
				ArrayList<State> nextList = winList;
				if (loseList.size() > 0) nextList = loseList;
				else if (hutuuList.size() > 0) nextList = hutuuList;
				for (State state : nextList) {
					Next n = negamax(state, depth - 1, alpha, beta);
					if (best.value > n.value) {
						best = n;
						if (alpha >= best.value) break;
						beta = Math.min(beta, best.value);
					}
				}
			}
		}
		memo.next = best;
		if (best.value < alpha) memo.upper = best.value;
		else if (best.value >= beta) memo.lower = best.value;
		else memo.lower = memo.upper = best.value;
		return best;
	}

	private final int dead(Character[] characters, int player_id) {
		int dead = 0;
		if (characters[0].dead && characters[0].player_id == player_id) ++dead;
		if (characters[1].dead && characters[1].player_id == player_id) ++dead;
		if (characters[2].dead && characters[2].player_id == player_id) ++dead;
		if (characters[3].dead && characters[3].player_id == player_id) ++dead;
		return dead;
	}

	final static void debug(final Object... obj) {
		System.err.println(Arrays.deepToString(obj));
	}

	final static String repeat(String s, int x) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < x; ++i)
			sb.append(s);
		return sb.toString();
	}
}
