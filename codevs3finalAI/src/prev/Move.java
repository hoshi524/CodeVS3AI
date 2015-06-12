enum Move {
	NONE(0), LEFT(-1), RIGHT(1), UP(-Parameter.X), DOWN(Parameter.X);

	public final int dir;

	private Move(int dir) {
		this.dir = dir;
	}
}