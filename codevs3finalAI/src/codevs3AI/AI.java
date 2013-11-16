package codevs3AI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

class AI {
	static final Random random = new Random();
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

	boolean putNotGridBomb(State now, Operation operations[], int id) {
		for (Character character : now.characters) {
			if (character.player_id != id)
				continue;
			Operation operation = operations[character.id & 1];
			if (operation.magic && (character.pos & 1) == 1) {
				return true;
			}
		}
		return false;
	}

	String think(String input) {
		Next next = dfs(new State(input), MAX_DEPTH);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Parameter.PLAYER; i++) {
			sb.append(next.operations[i].toString() + "\n");
		}
		return sb.toString();
	}

	HashSet<Long> used = new HashSet<Long>();

	Next dfs(State now, int depth) {
		Next best = new Next(Long.MIN_VALUE, operationList.get(0));

		class SortList implements Comparable<SortList> {
			final int result;
			final State now;
			final Operation[] o;

			public SortList(int result, State now, Operation[] o) {
				this.result = result;
				this.now = now;
				this.o = o;
			}

			@Override
			public int compareTo(SortList param) {
				return param.result - result;
			}
		}
		ArrayList<SortList> nodes = new ArrayList<SortList>();
		final long resultMax[] = new long[] { -1,//ここは参照しないはず
				Long.MIN_VALUE / 16, //相打ちの時
				Long.MAX_VALUE / 4, // 正常時
				Long.MAX_VALUE, // 相手詰み時
		};
		for (Operation[] allyOperations : operationList) {
			State tmp = new State(now);
			int res = tmp.operations(allyOperations, Parameter.MY_ID);
			if (res == 0 || res == -1 || res == -2)
				continue;
			nodes.add(new SortList(res, tmp, allyOperations));
		}
		Collections.sort(nodes);
		for (SortList node : nodes) {
			if (resultMax[node.result] < best.value)
				break;
			Next next = new Next(enemyOperation(node.now, depth), node.o);
			for (Operation o : node.o)
				if (o.equals(NONE))
					next.value -= 0xffffffffL;
			if (Parameter.DEBUG) {
				for (Operation o : node.o)
					Parameter.print(o.toString() + " ");
				Parameter.println(next.value + " ");
			}
			if ((best.value < next.value || (best.value == next.value && random.nextBoolean()))
					&& (next.value > Long.MAX_VALUE / 4 || !putNotGridBomb(node.now, node.o, Parameter.MY_ID))
					&& (MAX_DEPTH != depth || best.value == Long.MIN_VALUE || !used.contains(node.now.getHash()))) {
				Parameter.println("update");
				best = next;
			}
		}
		if (MAX_DEPTH == depth) {
			State tmp = new State(now);
			tmp.operations(best.operations, Parameter.MY_ID);
			used.add(tmp.getHash());
		}
		return best;
	}

	// static boolean debug = false;

	long enemyOperation(State now, int depth) {
		long value = Long.MAX_VALUE;
		boolean flag = true;
		ArrayList<State> hutuuList = new ArrayList<State>();
		ArrayList<State> tumiList = new ArrayList<State>();
		for (Operation[] enemyOperations : operationList) {
			State tmp = new State(now);

			/*if (now.map[78] == State.BOMB)
				debug = true;*/
			int res = tmp.operations(enemyOperations, Parameter.ENEMY_ID);
			/*if (now.map[78] == State.BOMB)
				debug = false;*/

			if (res == 0 || res == -2) {
				// 不正な行動
				// 魔法が有効じゃない
				continue;
			}
			tmp.step();
			if (res == 2) {
				// どっちも詰んでない
				if (depth == 0) {
					value = Math.min(value, tmp.calcValue());
				} else {
					hutuuList.add(tmp);
				}
			} else if (res == 1) {
				// 相打ち
				value = Math.min(value, tmp.calcFleeValue() + (Long.MIN_VALUE / 8));
				flag = false;
			} else if (res == 3) {
				// 自分が詰んだ
				value = Math.min(value, tmp.calcFleeValue() + (Long.MIN_VALUE / 4));
				flag = false;
			} else if (res == -1) {
				// 相手が詰んだ
				if (depth == 0) {
					value = Math.min(value, tmp.calcFleeValue() + (Long.MAX_VALUE / 2));
				} else {
					tumiList.add(tmp);
				}
			}
		}
		if (flag) {
			if (hutuuList.size() > 0) {
				for (State state : hutuuList) {
					value = Math.min(value, dfs(state, depth - 1).value);
				}
			} else if (tumiList.size() > 0) {
				for (State state : tumiList) {
					value = Math.min(value, dfs(state, depth - 1).value);
				}
			}
		}
		return value;
	}
}