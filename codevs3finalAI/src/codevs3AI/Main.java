package codevs3AI;

import java.util.Scanner;

public class Main {
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
}
