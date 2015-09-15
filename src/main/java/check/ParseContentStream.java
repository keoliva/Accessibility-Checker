package check;

import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.pdmodel.markedcontent.PDPropertyList;
import org.apache.pdfbox.util.*;

public class ParseContentStream {
	private PDPage curr_page;
	private Map<String, Operator> map_ops;
	private int max_mcid;

	public ParseContentStream( PDPage page, int max_mcid ) {
		curr_page = page;
		map_ops = Operators.map_ops;
		this.max_mcid = max_mcid;
	}
	
	/**
	 * @return a map with two values. One of them being a set of all the marked content IDs of 
	 * the marked images in the content as well as a set with all marked content ids for any 
	 * type of content
	 */
	public Map<String, Set> getMCIDs() {
		Map<String, Set> result = new HashMap<String, Set>();
		try {
			List<Object> tokens = curr_page.getContents().getStream().getStreamTokens();
			//all marked content ids
			Set<Integer> all_mcids = new HashSet<Integer>();
			//an index for the marked content ids of images
			Set<Integer> img_mcids = new HashSet<Integer>();
			//indices of certain operators, all within 
			//Operators.map_ops field
			List<Integer> indices = new ArrayList<Integer>();
			ListIterator<Integer> op_iterator; 
			Operator op;
			String operation;
			boolean find_lines = true;
			for (int i = 0; i < tokens.size(); i++) {
				Object obj = tokens.get(i);
				if (obj instanceof PDFOperator) {
					operation = ((PDFOperator) obj).getOperation();
					op = map_ops.get(operation);
					op = (op == null) ? Operator.OTHER : op;
					switch( op ) {
						case l://Append straight line segment to path
						case Do://An image is being painted, refers to indirect reference to an image
						case BI://Refers to an inline image
						case BDC://Beginning of marked content with a properties list as an operand
						case DP://also marks content and has a properties list as an operand
						case EMC://Ending of marked content
							indices.add(i);
							//refers to an image, or line being drawn
							if (operation.equals("Do") | operation.equals("BI") | operation.equals("l")) {
								//must 
								if (operation.equals("l")) {
									if (!find_lines) continue;
									else find_lines = false;
								} else if (operation.equals("Do")) {
									//operand is a COSName/key to an XObject in the 
									// page's resources. Have to check the xobject 
									// has a subtype of Image rather than Form
									String key = ((COSName) tokens.get(i - 1)).getName();
									if (!(curr_page.getResources().getXObjects().get( key ) 
											instanceof PDXObjectImage)) {
										continue;
									}
								}
							
								//pointer is next to the operator before the drawing operators 
								// (Do, BI, or l)
								op_iterator = indices.listIterator(indices.size()-1);
								if (op_iterator.hasPrevious()) {
									int prev_i = op_iterator.previous();
									String prev_op = ((PDFOperator) tokens.get(prev_i)).getOperation();
									if (prev_op.equals("BDC") | prev_op.equals("DP")) {
										//their operand is a COSDictionary
										int mcid = ((COSDictionary) tokens.get(prev_i-1)).getInt(COSName.MCID);
										if (mcid >= 0 && mcid < max_mcid) img_mcids.add(mcid);
									}
								}
							} else if (operation.equals("BDC") | operation.equals("DP")) {
								//their operand is a COSDictionary
								int mcid = ((COSDictionary) tokens.get(i-1)).getInt(COSName.MCID);
								if (mcid >= 0 && mcid < max_mcid) all_mcids.add(mcid);
							}
							break;
						default: break;
					}
				}
			}
			result.put("all_mcids", all_mcids);
			result.put("img_mcids", img_mcids);
			return result;
		} catch (IOException e) {
			return result;
		}
	}
}
