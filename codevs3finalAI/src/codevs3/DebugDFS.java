package codevs3;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class DebugDFS {

	private static final int MAX_DEPTH = 50;
	private static final String tabs[] = new String[MAX_DEPTH];
	Node root = new Node("root");
	@SuppressWarnings("unchecked")
	ArrayList<Node>[] nodeList = new ArrayList[MAX_DEPTH];
	{
		for (int i = 0; i < MAX_DEPTH; ++i) {
			nodeList[i] = new ArrayList<>();
			String s = "";
			for (int j = 0; j < i; ++j)
				s += "	";
			tabs[i] = s;
		}
		nodeList[0].add(root);

	}

	void addNode(String s, int depth) {
		if (depth < 0) throw new RuntimeException();
		Node n = new Node(s);
		if (depth == 0) root.child.add(n);
		else nodeList[depth - 1].get(nodeList[depth - 1].size() - 1).child.add(n);
		nodeList[depth].add(n);
	}

	void print() {
		try {
			PrintWriter out = new PrintWriter(new File("debug.txt"));
			dfs(out, root, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void dfs(PrintWriter out, Node n, int depth) {
		out.print(tabs[depth]);
		out.println(n);
		for (Node c : n.child)
			dfs(out, c, depth + 1);
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
