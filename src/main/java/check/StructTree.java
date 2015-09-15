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
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.pdmodel.markedcontent.PDPropertyList;
import org.apache.pdfbox.util.PDFOperator;

public class StructTree {
	PDStructureTreeRoot st_root;
	Map role_map;

	List<PDPage> pages;
	Map<Integer, Integer> structparents_to_pages;
	Integer curr_page_index;
	Set<Integer> curr_page_img_mcids;
	
	boolean figures_warning_on; //if there's something off with the figures
	static int reasonable_amount_of_headings = 5;
	boolean headings_warning_on;
	int headings_count;
	
	/**
	 * Constructor
	 * @param root the structure tree root of a document
	 */
	public StructTree( PDStructureTreeRoot root, List<PDPage> doc_pages ) {
		st_root = root;
		role_map = root.getRoleMap();
		pages = doc_pages;	
		structparents_to_pages = new HashMap<Integer, Integer>();
		for (int p = 0; p < pages.size(); p++) {
			structparents_to_pages.put(pages.get(p).getStructParents(), p);
		}
		//System.out.println(structparents_to_pages);
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
		//dictionary of the parent tree in the structure tree root
		COSDictionary dict = st_root.getParentTree().getCOSDictionary();
		//the key is either Nums or Kids XOR
		String key = (dict.containsKey("Nums")) ? "Nums" : "Kids";
		COSArray kids = (COSArray) dict.getItem(key);
		Map<Integer, Set> figures = new HashMap<Integer, Set>();
		Map<String, Integer> headings = new HashMap<String, Integer>();
		
		ParseContentStream parser;
		Map<String, Set> mcids;
		Set<Integer> all_mcids;
		for (int i = 0; i < kids.size(); i++) {
			COSBase kid = resolve(kids.get(i));
			if (kid instanceof COSArray) {//represents a page
				COSArray elem = (COSArray) kid;
				int index = 0;
				while (elem.get(index) == null)
					index++;
				// Find a marked content item, and look at its "Pg" entry which represents 
				// the page that the content item is being held, to be certain of which page 
				// to parse
				COSDictionary item = (COSDictionary) ((COSObject) elem.get(index)).getObject();
				COSDictionary pg = (COSDictionary) item.getDictionaryObject(COSName.PG);
				
				curr_page_index = structparents_to_pages.get(pg.getInt(COSName.STRUCT_PARENTS));
				//System.out.println("Page " + (curr_page_index + 1));
				
				parser = new ParseContentStream(pages.get(curr_page_index), elem.size());
				mcids = parser.getMCIDs();
				
				curr_page_img_mcids = mcids.get("img_mcids");
				
				all_mcids = mcids.get("all_mcids");

				if (all_mcids != null) {
					for (int j : all_mcids) {
						//The marked content ids gathered, are the 
						// keys to structure elements within the page
						processElement(elem.get(j), figures, headings);
					}	
				}
			}
		}
		Map<String, Object> objs = new HashMap<String, Object>();
		objs.put("headings", headings);
		objs.put("figures", figures);
		
		if (headings_count < 5) {
			headings_warning_on = true;
		}
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
	public static Integer filterInts( List<?> list ) {
	    for (Object item : list) {
	      if (elemIsInt((COSBase) item)) {
	    	  return  ((COSInteger)item).intValue();
	      }
	    }
	    return null;
	}
	
	/**
	 * @param tag of the struct elem
	 * @return if tag is Figure, or if it maps to Figure within the rolemap
	 */
	public boolean isFigure( String tag ) {
		Object value = role_map.get(tag);
		boolean in_rolemap = (value == null) ? false : value.equals("Figure");
		return tag.equals("Figure") | in_rolemap;
	}
	
	/**
	 * 
	 * @param tag
	 * @return if tag is a heading, or if it maps to a heading within the rolemap
	 */
	public boolean isHeading( String tag ) {
		Pattern ptrn = Pattern.compile("^H\\d*$"); //Anything of the form H or H1, H2, ...
		
		Object value = role_map.get(tag);
		boolean in_rolemap = (value == null) ? false : ptrn.matcher((String) value).find();
		return ptrn.matcher(tag).find() | in_rolemap;
	}
	
	/**private boolean isImageXObject() {
		//operand is a COSName/key to an XObject in the 
		// page's resources. Have to check the xobject 
		// has a subtype of Image rather than Form
		String key = ((COSName) tokens.get(i - 1)).getName();
		if (!(curr_page.getResources().getXObjects().get( key ) 
				instanceof PDXObjectImage)) {
			continue;
		}
	}*/
	private boolean hasAltText( COSDictionary dict ) {
		return dict.getString(COSName.ALT, null) != null;
	}
	
	/**
	 * updates figures array and headings dictionary if any 
	 * elem has type either Figure or is a type of heading
	 * @param elem
	 * @param pages_seen
	 * @param figures
	 * @param headings
	 */
	public void processElement( COSBase elem, Map<Integer, Set> figures, Map<String, Integer> headings ) {
		elem = resolve(elem);
		if (elem instanceof COSDictionary) { //PDStructureElement
			COSDictionary dict = ((COSDictionary) elem);
			String tag = dict.getNameAsString(COSName.S);
			
			//Get the marked content ID of the structure element
			Integer mcid;
			COSBase k_item = resolve(dict.getItem(COSName.K));
			if (k_item instanceof COSInteger) {
				mcid = ((COSInteger)k_item).intValue();
			} else { //An array
				mcid = filterInts(((COSArray) k_item).toList());
			}
			
			if (tag != null) {
				//System.out.println(tag);
				if (isFigure( tag ) || hasAltText( dict ) || curr_page_img_mcids.contains( mcid )) {
					Map<String, Object> figure_dict = new HashMap<String, Object>();
					figure_dict.put("Alt", dict.getString(COSName.ALT, "None"));
					figure_dict.put("MCID", mcid);
					if (!isFigure(tag)) { //yet the image is marked in the content stream
						figure_dict.put("Warning",
							String.format("This image is marked as a %s "
									+ "rather than a Figure.", tag));
						figures_warning_on = true;
					}
					//since pages_seen is zero indexed
					Set figure_objs = figures.get(curr_page_index + 1);
					if (figure_objs == null) {
						figure_objs = new HashSet();
					}
					figure_objs.add(figure_dict);
					figures.put(curr_page_index + 1, figure_objs);
				} else if (isHeading( tag )) {
					Integer count = headings.get(tag);
					headings.put(tag, (count == null)? 1 : count + 1);
					headings_count++;
				}
			}
		}
	}
}
