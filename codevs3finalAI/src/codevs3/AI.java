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
	}

	static final int MAX_VALUE = Integer.MAX_VALUE - (Integer.MAX_VALUE >> 2);
	static final int MIN_VALUE = Integer.MIN_VALUE - (Integer.MIN_VALUE >> 2);
	static final Operation NONE = new Operation(Move.NONE, false, 5);
	static final int MAX_DEPTH = 2;

	static final Operation[][] operationList;

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
		class innerfunc {
			void operation_dfs(int character_num, Operation[] now, ArrayList<Operation[]> res) {
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
		}
		ArrayList<Operation[]> list = new ArrayList<>();
		new innerfunc().operation_dfs(0, new Operation[Parameter.PLAYER], list);
		operationList = list.toArray(new Operation[0][]);
	}

	public String think(String input) {
		for (int i = 0; i <= MAX_DEPTH; ++i)
			already[i].clear();
		State state = new State(input);
		// Next next = MTDF(state);
		Next next = negamax(state, MAX_DEPTH, MIN_VALUE, MAX_VALUE, true);
		System.err.println(String.format("%3d : %15d", state.turn, next.value));

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
		int g = 0;
		Next n = new Next(MIN_VALUE, operationList[0]);
		while (lower < upper) {
			int b;
			if (g == lower)
				b = g + 1;
			else
				b = g;
			n = negamax(now, MAX_DEPTH, b - 1, b, true);
			g = n.value;
			if (g < b)
				upper = g;
			else
				lower = g;
			// debug(lower, upper);
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

	static boolean target = false;
	Next negamax(State now, int depth, int alpha, int beta, boolean isMe) {
		Next best = new Next(isMe ? MIN_VALUE : MAX_VALUE, operationList[0]);
		if (isMe) {
			long key = now.getHash();
			Already memo = already[depth].get(key);
			if (memo != null) {
				if (beta < memo.lower) {
					// debug("beta", depth, beta, memo.lower);
					return new Next(memo.lower, memo.next.operations);
				}
				if (memo.upper < alpha) {
					// debug("alpha", depth, memo.upper, alpha);
					return new Next(memo.upper, memo.next.operations);
				}
				alpha = Math.max(alpha, memo.lower);
				beta = Math.min(beta, memo.upper);
			} else {
				memo = new Already();
				already[depth].put(key, memo);
			}
			for (Operation[] operations : operationList) {
				State tmp = new State(now);
				int res = tmp.operations(operations, Parameter.MY_ID, depth);
				if (res == 0 || res == -1 || res == -2)
					continue;
				//				if (depth == MAX_DEPTH)
				//					target = operations[0] == this.operations[7] && operations[1] == this.operations[7];
				//				if (target) {
				//					debug("ally", depth, operations, res);
				//				}
				Next n = new Next(negamax(tmp, depth, alpha, beta, !isMe).value, operations);
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
			ArrayList<State> winList = new ArrayList<>();
			ArrayList<State> loseList = new ArrayList<>();

			for (Operation[] operations : operationList) {
				State tmp = new State(now);
				int res = tmp.operations(operations, Parameter.ENEMY_ID, depth);
				//				if (target) {
				//					debug("enemy", depth, operations, res);
				//				}
				if (res == 0 || res == -2) {
					// 不正な行動
					// 魔法が有効じゃない
					continue;
				}
				tmp.step();
				if (res == 2) {
					// どっちも詰んでない
					if (depth == 0) {
						best.value = Math.min(best.value, tmp.calcValue());
					} else {
						hutuuList.add(tmp);
					}
				} else if (res == 1) {
					// 相打ち
					if (depth == 0 || dead(tmp.characters, Parameter.ENEMY_ID) > 0) {
						best.value = Math.min(best.value, tmp.calcFleeValue() + State.AiutiValue);
					} else {
						aiutiList.add(tmp);
					}
				} else if (res == 3) {
					// 自分が詰んだ
					if (depth == 0 || dead(tmp.characters, Parameter.MY_ID) > 0) {
						best.value = Math.min(best.value, tmp.calcFleeValue() + (MIN_VALUE >> 2));
					} else {
						loseList.add(tmp);
					}
				} else if (res == -1) {
					// 相手が詰んだ
					if (depth == 0 || dead(tmp.characters, Parameter.ENEMY_ID) > 0) {
						best.value = Math.min(best.value, tmp.calcFleeValue() + (MAX_VALUE >> 2));
					} else {
						winList.add(tmp);
					}
				}
			}
			if (depth > 0) {
				ArrayList<State> nextList = winList;
				if (loseList.size() > 0)
					nextList = loseList;
				else if (aiutiList.size() > 0)
					nextList = aiutiList;
				else if (hutuuList.size() > 0)
					nextList = hutuuList;
				for (State state : nextList) {
					Next n = negamax(state, depth - 1, alpha, beta, !isMe);
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
		if (characters[0].dead && characters[0].player_id == player_id)
			++dead;
		if (characters[1].dead && characters[1].player_id == player_id)
			++dead;
		if (characters[2].dead && characters[2].player_id == player_id)
			++dead;
		if (characters[3].dead && characters[3].player_id == player_id)
			++dead;
		return dead;
	}

	final static void debug(final Object... obj) {
		System.err.println(Arrays.deepToString(obj));
	}
}
