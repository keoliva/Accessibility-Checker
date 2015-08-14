package check;

import check.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.simple.JSONObject;
import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.jempbox.xmp.XMPSchemaDublinCore;
import org.apache.jempbox.xmp.XMPSchemaPDF;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;

/**
 * Prints out a simple accessibility report for a document
 * @author Kayla
 *
 */
public class Checker {
	public PDDocument document;
	private String target;
	public PDDocumentCatalog root;
	public static StructTree stree;
	public List<PDPage> pages;
	private boolean changes_made = false;
	private boolean changes_made_to_docinfo;
	
	private static boolean tagged;
	private static String main_lang;
	
	/**
	 * Constructor 
	 * @param filename of the document whose accessibility report will be 
	 * given
	 * @throws IOException
	 */
	public Checker( String filename ) throws IOException {
		target = filename;
		document = PDDocument.load(filename);
		root = document.getDocumentCatalog();
		pages = root.getAllPages();
		stree = new StructTree(root.getStructureTreeRoot(), pages);
	}
		
	/**
	 * resolves the metadata in the information dictionary
	 * in the root of the pdf, and its metadata in the
	 * XMP format
	 * @return important fields in the metadata
	 */
	public Map<String, Object> displayDocInfo() {
		Map<String, Object> prop = new ResolveMetadata(this).getProperties();
		if (prop.containsKey("changes_made"))
			changes_made = true;
		return prop;
	}
	
	/**
	 * @return the primary language of the document
	 */
	private String displayMainLanguage() {
		try {
			COSDictionary root_dict = root.getCOSDictionary();
			String lang = root_dict.getString(COSName.LANG, "None");
			main_lang = lang;
			return lang;
		} catch ( Exception e ) { //root is null
			main_lang = "None";
			return "None";
		}
	}
	
	/**
	 * makes sure that the Marked entry in the document's
	 * MarkedInfo dictionary coincides with the existence 
	 * of the structure tree root, since that's the definite 
	 * way of knowing if a document has a logical structure
	 * @return whether or not the document is tagged
	 */
	private boolean displayTaggedBool() {
		try {
			PDMarkInfo mark_info = root.getMarkInfo();
			boolean marked_bool = (mark_info != null) ? (root.getMarkInfo().isMarked()) : false;
			if (stree.isValid()) {//officially marked
				if (!marked_bool) {
					mark_info.setMarked(true);
					root.setMarkInfo(mark_info);
					
					changes_made = true;
				}
				tagged = true;
				return true;
			} else {//officially unmarked
				if (marked_bool) {
					mark_info.setMarked(false);
					root.setMarkInfo(mark_info);
					
					changes_made = true;
				}
				tagged = false;
				return false;
			}
		} catch ( Exception e ) { //root is null
			tagged = false;
			return false;
		}		
	}
	
	/**
	 * @return a general message about how the check went, for the accessibility 
	 * of the document
	 */
	private Map<String, String> getGeneralMessage() {
		Map<String, String> msg = new HashMap<String, String>();
		if (!tagged)  
			msg.put("danger", "Your document is not tagged.");
		else if (main_lang == "None" | stree.figures_warning_on | 
				stree.headings_warning_on)
			msg.put("warning", "There are some problems you'll want to fix.");
		else //at least tagged
			msg.put("success", "Things look good.");

		return msg;
	}
	
	/**
	 * saves the document if changes were made, then finally closes it
	 * @throws IOException
	 */
	public void closeDocument() throws IOException {
		try {
			if (changes_made) {
				document.save(target);
			}
		} catch (Exception e) {
			
		} finally {
			document.close();
		}
	}

	public static void main(String[] args) throws IOException, COSVisitorException {
		//String filename = "VISM_ASSETS_Camera_Ready.pdf";
		//String filename = "socialmicrovolunteering.pdf";
		new Operators();
		int size = args.length;
		String filename;
		Checker report;
		for (int i=0; i < size; i++) {
			filename = args[i];
			JSONObject report_obj = new JSONObject();
			report = new Checker(filename);
			report_obj.put("properties", report.displayDocInfo());
			report_obj.put("language", report.displayMainLanguage());
			report_obj.put("tagged_bool", report.displayTaggedBool());

			report_obj.put("tags_info", stree.traverseParentTree());
			
			report_obj.put("general_message", report.getGeneralMessage());
			
			report.closeDocument();
			System.out.println(report_obj);
		}
	}
}
