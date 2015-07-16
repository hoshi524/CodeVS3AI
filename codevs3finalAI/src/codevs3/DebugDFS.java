package codevs3;

import java.util.ArrayList;

public class DebugDFS {

	private static final int MAX_DEPTH = 50;
	Node root = new Node("root");
	@SuppressWarnings("unchecked")
	ArrayList<Node>[] nodeList = new ArrayList[MAX_DEPTH];
	{
		for (int i = 0; i < MAX_DEPTH; ++i)
			nodeList[i] = new ArrayList<>();
		nodeList[0].add(root);
	}

	void addNode(String s, int depth) {
		if (depth < 0) throw new RuntimeException();
		Node n = new Node(s);
		if (depth == 0) root.child.add(n);
		else nodeList[depth - 1].get(nodeList[depth - 1].size() - 1).child.add(n);
		nodeList[depth].add(n);
	}

	class Node {
		final String s;
		final ArrayList<Node> child = new ArrayList<>();

		Node(String s) {
			this.s = s;
		}

		public String toString() {
			return s;
		}
	}
}
