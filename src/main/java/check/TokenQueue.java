package check;

import java.util.LinkedList;

public class TokenQueue {
	
	private LinkedList queue = new LinkedList();
	
	public boolean isEmpty() {
		return queue.isEmpty();
	}
	
	public void enque( Object obj ) {
		queue.addLast(obj);
		System.out.println("push(" + obj + ")");
	    System.out.println("stack: " + queue);
	}
	
	public Object dequeue() {
		return queue.removeFirst();
	}
	
	public Object seek() {
		return queue.getFirst();
	}
	
	public int size() {
		return queue.size();
	}
}
