package net.fybertech.cursecli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class InputQueue
{
	private static List<String> queue = new ArrayList<>();
	private static Scanner scanner = new Scanner(System.in);		
	
	public static String getNext(String prompt) 
	{			
		if (queue.size() < 1) {
			if (prompt != null) System.out.print(prompt);
			String input = scanner.nextLine();
			String[] split = input.split(" ");
			queue.addAll(Arrays.asList(split));				
		}			
		
		return queue.remove(0);					
	}
	
	public static String getNextFull(String prompt) 
	{		
		if (queue.size() < 1) {
			if (prompt != null) System.out.print(prompt);
			return scanner.nextLine();				
		}			
		
		StringBuilder sb = new StringBuilder();

		for(String token: queue) {
		   sb.append(token).append(' ');
		}
		sb.deleteCharAt(sb.length()-1);

		queue.clear();			
		return sb.toString();		
	}
	
	public static String getNext() {
		return getNext(null);
	}
	
	public static void clear() {
		queue.clear();
	}
	
	public static void close() {
		scanner.close();
	}		
}

