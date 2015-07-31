package check;
import java.util.Stack;
import java.util.LinkedList;

/**
 * Stack class implemented as a wrapper for java.util.LinkedList class.
 * The methods delegate to LinkedList methods.
 * @author Kayla
 *
 */
public class TokenStack {
	private Stack stack = new Stack();
	
	public boolean isEmpty() {
		return stack.empty();
	}
	
	public void push( Object obj ) {
		stack.push(obj);
		System.out.println("push(" + obj + ")");
	    System.out.println("stack: " + stack);
	}
	
	public Object pop() {
		if (isEmpty()) {
			//System.out.println("Ummm, you're shit's empty");
			//return null;
		}
		System.out.print("pop -> ");
	    Object top = stack.pop();
	    System.out.println("->" + top);
	    //System.out.println("stack: " + list);
		return top;
	}
	
	public Object peek() {
		return stack.peek();
	}
	
	public int size() {
		return stack.size();
	}

}
