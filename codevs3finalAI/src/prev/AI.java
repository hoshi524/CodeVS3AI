package prev;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import codevs3.Character;
import codevs3.Hash;
import codevs3.Move;
import codevs3.Next;
import codevs3.Operation;
import codevs3.Parameter;

public class AI {

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
			for (int i = 0; i < now.length; ++i) {
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

	String think(String input) {
		start = System.nanoTime();
		already.clear();
		State state = new State(input);

		// Next next = negamax(state, MAX_DEPTH, Long.MIN_VALUE, Long.MAX_VALUE, true);
		//		Next test = mtdf(state);
		//		if (next.value != test.value) {
		//			System.err.println(next.value + " != " + test.value);
		//		}
		Next next = MTDF(state);
		System.err.println(String.format("%3d : %20d", state.turn, next.value));

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Parameter.PLAYER; ++i) {
			sb.append(next.operations[i].toString()).append("\n");
		}
		return sb.toString();
	}

	private long prevG = 0;

	Next MTDF(State now) {
		already.clear();
		long lower = Long.MIN_VALUE;
		long upper = Long.MAX_VALUE;
		long g = prevG;
		Next best = new Next(Long.MIN_VALUE, operationList.get(0));
		while (lower < upper) {
			long b;
			if (g == lower)
				b = g + 1;
			else
				b = g;
			Next next = negamax(now, MAX_DEPTH, b - 1, b, true);
			g = next.value;
			if (next.value < b)
				upper = g;
			else
				lower = g;
			if (best.value < next.value) {
				best = next;
			}
			// System.err.println(lower + " " + upper);
		}
		prevG = best.value;
		return best;
	}

	HashSet<Long> used = new HashSet<Long>();

	class Already {
		Next next;
		long lower = Long.MIN_VALUE, upper = Long.MAX_VALUE;
	}

	HashMap<Long, Already> already = new HashMap<>();

	Next negamax(State now, int depth, long alpha, long beta, boolean isMy) {
		Next best = new Next(isMy ? Long.MIN_VALUE : Long.MAX_VALUE, operationList.get(0));
		if (isMy) {
			long key = now.getHash() ^ Hash.hashMap[depth];
			Already memo;
			if (already.containsKey(key)) {
				memo = already.get(key);
				if (beta <= memo.lower)
					return new Next(memo.lower, memo.next.operations);
				if (memo.upper <= alpha)
					return new Next(memo.upper, memo.next.operations);
				alpha = Math.max(alpha, memo.lower);
				beta = Math.min(beta, memo.upper);
			} else {
				memo = new Already();
				already.put(key, memo);
			}
			for (Operation[] operations : operationList) {
				State tmp = new State(now);
				int res = tmp.operations(operations, Parameter.MY_ID, depth);
				if (res == 0 || res == -1 || res == -2)
					continue;
				Next n = new Next(negamax(tmp, depth, alpha, beta, !isMy).value, operations);
				//				if (Parameter.DEBUG && MAX_DEPTH == depth) {
				//					Parameter.print(depth + " ");
				//					for (Operation o : operations)
				//						Parameter.print(o.toString() + " ");
				//					Parameter.println(n.value + " ");
				//				}
				if (best.value < n.value) {
					best = n;
					if (best.value >= beta) {
						memo.next = best;
						memo.lower = best.value;
						return best;
					}
					alpha = Math.max(alpha, best.value);
				}
			}
			memo.next = best;
			memo.lower = memo.upper = best.value;
		} else {
			ArrayList<State> aiutiList = new ArrayList<>();
			ArrayList<State> hutuuList = new ArrayList<>();
			ArrayList<State> tumiList = new ArrayList<>();

			for (Operation[] operations : operationList) {
				State tmp = new State(now);
				int res = tmp.operations(operations, Parameter.ENEMY_ID, depth);
				if (res == 0 || res == -2) {
					// 不正な行動
					// 魔法が有効じゃない
					continue;
				}
				tmp.step();
				// Parameter.print(tmp);
				if (res == 2) {
					// どっちも詰んでない
					if (depth == 0) {
						best.value = Math.min(best.value, tmp.calcValue());
					} else {
						hutuuList.add(tmp);
					}
				} else if (res == 1) {
					// 相打ち
					if (depth == 0
							|| (dead(tmp.characters, Parameter.ENEMY_ID) > 0 && dead(tmp.characters, Parameter.ENEMY_ID) == dead(
									tmp.characters, Parameter.MY_ID))) {
						best.value = Math.min(best.value, tmp.calcFleeValue() + State.AiutiValue);
					} else {
						aiutiList.add(tmp);
					}
				} else if (res == 3) {
					// 自分が詰んだ
					best.value = Math.min(best.value, tmp.calcFleeValue() + Long.MIN_VALUE / 4);
					return best;
				} else if (res == -1) {
					// 相手が詰んだ
					if (depth == 0 || dead(tmp.characters, Parameter.ENEMY_ID) > 0) {
						best.value = Math.min(best.value, tmp.calcFleeValue() + Long.MAX_VALUE / 4);
					} else {
						tumiList.add(tmp);
					}
				}
			}
			if (aiutiList.size() > 0) {
				for (State state : aiutiList) {
					Next n = negamax(state, depth - 1, alpha, beta, !isMy);
					if (best.value > n.value) {
						best = n;
						if (alpha >= best.value) {
							return best;
						}
						beta = Math.min(beta, best.value);
					}
				}
			} else if (hutuuList.size() > 0) {
				for (State state : hutuuList) {
					Next n = negamax(state, depth - 1, alpha, beta, !isMy);
					if (best.value > n.value) {
						best = n;
						if (alpha >= best.value) {
							return best;
						}
						beta = Math.min(beta, best.value);
					}
				}
			} else if (tumiList.size() > 0) {
				for (State state : tumiList) {
					Next n = negamax(state, depth - 1, alpha, beta, !isMy);
					if (best.value > n.value) {
						best = n;
						if (alpha >= best.value) {
							return best;
						}
						beta = Math.min(beta, best.value);
					}
				}
			}
		}
		return best;
	}

	private final int dead(Character[] characters, int player_id) {
		int dead = 0;
		for (Character c : characters)
			if (c.dead && c.player_id == player_id)
				++dead;
		return dead;
	}
}
