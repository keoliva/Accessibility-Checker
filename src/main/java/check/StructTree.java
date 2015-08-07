package check;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.Number;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.*;
import org.apache.pdfbox.pdmodel.markedcontent.PDPropertyList;
import org.apache.pdfbox.util.PDFOperator;



public class StructTree {
	PDStructureTreeRoot st_root;
	static Map<String, Object> role_map;

	static List<PDPage> pages;
	
	/**
	 * Constructor
	 * @param root the structure tree root of a document
	 */
	public StructTree( PDStructureTreeRoot root, List<PDPage> doc_pages ) {
		st_root = root;
		pages = doc_pages;
	}
	
	/**
	 * Checks if the structure tree root exists
	 * @return whether or not structure tree root holds logical structure
	 */
	boolean isValid() {
		return (st_root != null) ? (!st_root.getKids().isEmpty()) : false; 
	}
	
	/**
	 * traverses the parent tree of the structure tree root
	 * @return a dictionary with two keys:
	 * figures, whose value is an array of dictionaries describing 
	 * a figure, mcid, page, and alternative text.
	 * headings, maps heading types to how many times they appear
	 * in the document
	 */
	public Map traverseParentTree() {
		if (!isValid()) {
			return new HashMap<String, Object>();
		}
		role_map = st_root.getRoleMap();
		PDNumberTreeNode parent_tree = st_root.getParentTree();
		COSDictionary dict = parent_tree.getCOSDictionary();
		String key = (dict.containsKey("Nums")) ? "Nums" : "Kids";
		COSArray kids = (COSArray) dict.getItem(key);
		
		int pages_seen = 0;
		List<Object> figures = new ArrayList<Object>();
		Map<String, Integer> headings = new HashMap<String, Integer>();
		for (int i=0; i < kids.size(); i++) {
			COSBase kid = resolve(kids.get(i));
			if (kid instanceof COSArray) {//represents a page
				COSArray elem = (COSArray) kid;
				for (int j=0; j < elem.size(); j++) {
					processElement(elem.get(j), pages_seen, figures, headings);
				}
				pages_seen++;
			}
		}
		Map<String, Object> objs = new HashMap<String, Object>();
		objs.put("headings", headings);
		objs.put("figures", figures);
		return objs;
	}
	
	/**
	 * resolves an indirect reference to an object
	 * @param elem
	 * @return direct reference to elem if it's not already
	 */
	static COSBase resolve( COSBase elem ) {
		if (elem instanceof COSObject) 
			return ((COSObject) elem).getObject();
		return elem;
	}
	
	/**
	 * @param e : COSBase
	 * @return whether the object is an of type COSInteger
	 */
	public static boolean elemIsInt( COSBase e ) {
		return (e instanceof COSInteger);
	}
	
	/**
	 * filters a list of objects
	 * @param list : a list of integers
	 * @param p : the predicate used in the filter
	 * @return the first instance of a COSInteger object
	 */
	public static Integer filterInts( List<?> list) {
	    for (Object item : list) {
	      if (elemIsInt((COSBase) item)) {
	    	  return  ((COSInteger)item).intValue();
	      }
	    }
	    return null;
	}
	
	/**
	 * 
	 * @param tag of the struct elem
	 * @return if tag is Figure, or if it maps to Figure within the rolemap
	 */
	public static boolean isFigure(String tag) {
		Object value = role_map.get(tag);
		boolean in_rolemap = (value == null) ? false : value.equals("Figure");
		return tag.equals("Figure") | in_rolemap;
	}
	
	/**
	 * 
	 * @param tag
	 * @return if tag is a heading, or if it maps to a heading within the rolemap
	 */
	public static boolean isHeading(String tag) {
		Pattern ptrn = Pattern.compile("^H\\d*$"); //Anything of the form H or H1, H2, ...
		
		Object value = role_map.get(tag);
		boolean in_rolemap = (value == null) ? false : ptrn.matcher((String) value).find();
		return ptrn.matcher(tag).find() | in_rolemap;
	}
	
	/**
	 * updates figures array and headings dictionary if any 
	 * elem has type either Figure or is a type of heading
	 * @param elem
	 * @param pages_seen
	 * @param figures
	 * @param headings
	 */
	public static void processElement( COSBase elem, Integer pages_seen, List<Object> figures, Map<String, Integer> headings ) {
		elem = resolve(elem);
		if (elem instanceof COSDictionary) { //PDStructureElement
			COSDictionary dict = ((COSDictionary) elem);
			String tag = dict.getNameAsString(COSName.S);
			if (tag != null) {
				if (isFigure(tag)) {
					Map<String, Object> figure_dict = new HashMap<String, Object>();
					figure_dict.put("Alt", dict.getString(COSName.ALT));
					figure_dict.put("Page", pages_seen);
					
					Integer mcid;
					COSBase k_item = resolve(dict.getItem(COSName.K));
					if (k_item instanceof COSInteger) {
						mcid = ((COSInteger)k_item).intValue();
					} else { //An array
						mcid = filterInts(((COSArray) k_item).toList());
					}
					figure_dict.put("MCID", mcid);
					figures.add(figure_dict);
				} else if (isHeading(tag)) {
					Integer count = headings.get(tag);
					headings.put(tag, (count == null)? 1 : count + 1);
				}
			}
		}
	}
}
