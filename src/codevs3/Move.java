package codevs3;

public enum Move {
	NONE(0, new Check() {
		@Override
		public boolean before(int p) {
			return true;
		}

		@Override
		public boolean after(int p) {
			return true;
		}
	}), LEFT(-1, new Check() {
		@Override
		public boolean before(int p) {
			return p % Parameter.X != 0;
		}

		@Override
		public boolean after(int p) {
			return p >= 0 && p % Parameter.X + 1 != Parameter.X;
		}
	}), RIGHT(1, new Check() {
		@Override
		public boolean before(int p) {
			return p % Parameter.X + 1 != Parameter.X;
		}

		@Override
		public boolean after(int p) {
			return p % Parameter.X != 0;
		}
	}), UP(-Parameter.X, new Check() {
		@Override
		public boolean before(int p) {
			return p >= Parameter.X;
		}

		@Override
		public boolean after(int p) {
			return p >= 0;
		}
	}), DOWN(Parameter.X, new Check() {
		@Override
		public boolean before(int p) {
			return p + Parameter.X < Parameter.XY;
		}

		@Override
		public boolean after(int p) {
			return p < Parameter.XY;
		}
	});

	public final int dir;
	public final Check check;

	public interface Check {
		boolean before(int p);
		boolean after(int p);
	}

	private Move(int dir, Check check) {
		this.dir = dir;
		this.check = check;
	}
}