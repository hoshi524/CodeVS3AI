
/**
 * プレイヤーが行う操作コマンド
 */
class Operation {

	Move move = Move.NONE;
	boolean magic = false;
	int burstTime;

	public Operation(Move move, boolean magic, int burstTime) {
		this.move = move;
		this.magic = magic;
		this.burstTime = burstTime;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(move.name());
		if (magic)
			sb.append(" MAGIC ").append(burstTime);
		return sb.toString();
	}
}