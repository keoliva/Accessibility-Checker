package check;

import java.util.HashMap;
import java.util.Map;

public class Operators {
	public static final Map<String, Operator> map_ops = new HashMap<String, Operator>();
	
	/**
	 * Constructor
	 * puts certain operators in a PDFs content stream 
	 * that refer to images, marked content, and when lines are 
	 * made (lines made in tables)
	 */
	public Operators() {
		map_ops.put("l", Operator.l);
		map_ops.put("Do", Operator.Do);
		map_ops.put("BI", Operator.BI);
		map_ops.put("BDC", Operator.BDC);
		map_ops.put("DP", Operator.DP);
		map_ops.put("EMC", Operator.EMC);
	}
}
