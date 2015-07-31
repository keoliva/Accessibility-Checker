package check;

import check.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.simple.JSONObject;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;

/**
 * Prints out a simple accessibility report for a document
 * @author Kayla
 *
 */
public class Checker {
	public static PDDocument document;
	private static PDDocumentCatalog root;
	private static StructTree stree;
	private static List<PDPage> pages;
	private static boolean changes_made;
	
	/**
	 * Constructor 
	 * @param filename of the document whose accessibility report will be 
	 * given
	 * @throws IOException
	 */
	public Checker( String filename ) throws IOException {
		document = PDDocument.load(filename);
		File f = new File(filename);
		PDFParser parser = new PDFParser(new FileInputStream(f));
		parser.parse();
		root = document.getDocumentCatalog();
		pages = root.getAllPages();
		stree = new StructTree(root.getStructureTreeRoot(), pages);
		changes_made = false;
	}
		
	/**
	 * resolves the metadata in the information dictionary
	 * in the root of the pdf, and its metadata in the
	 * XMP format
	 * @return important fields in the metadata
	 */
	private Map<String, String> displayDocInfo() {
		COSDictionary docInfo = document.getDocumentInformation().getDictionary();
		
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("title", docInfo.getString(COSName.TITLE, ""));
		properties.put("author", docInfo.getString(COSName.AUTHOR, ""));
		properties.put("subject", docInfo.getString(COSName.SUBJECT, ""));
		properties.put("keywords", docInfo.getString(COSName.KEYWORDS, ""));
		return properties;
	}
	
	/**
	 * @return the primary language of the document
	 */
	private String displayMainLanguage() {
		COSDictionary root_dict = root.getCOSDictionary();
		return root_dict.getString(COSName.LANG, "None");
	}
	
	/**
	 * makes sure that the Marked entry in the document's
	 * MarkedInfo dictionary coincides with the existence 
	 * of the structure tree root, since that's the definite 
	 * way of knowing if a document has a logical structure
	 * @return whether or not the document is tagged
	 */
	private boolean displayTaggedBool() {
		PDMarkInfo mark_info = root.getMarkInfo();
		boolean marked_bool = (mark_info != null) ? (root.getMarkInfo().isMarked()) : false;
		if (stree.isValid()) {//officially marked
			if (!marked_bool) {
				mark_info.setMarked(true);
				root.setMarkInfo(mark_info);
				
				changes_made = true;
			}
			return true;
		} else {//officially unmarked
			if (marked_bool) {
				mark_info.setMarked(false);
				root.setMarkInfo(mark_info);
				
				changes_made = true;
			}
			return false;
		}
	}

	public static void main(String[] args) throws IOException, COSVisitorException {
		/**String filename;//= args[0];
		for (int i=0; i < args.length; i++) {
			filename = args[i];
			//String filename = "socialmicrovolunteering.pdf";
			Checker report = new Checker(filename);
			JSONObject report_obj = new JSONObject();
			report_obj.put("properties", report.displayDocInfo());
			report_obj.put("language", report.displayMainLanguage());
			report_obj.put("tagged_bool", report.displayTaggedBool());
			report_obj.put("tags_info", stree.traverseParentTree());
			report_obj.put("changes_made_bool", changes_made);
			System.out.print(report_obj);
			if (changes_made) {
				document.save(filename);
			}
		}*/
		String filename = "CHI2016ProceedingsFormat.pdf";
		Checker report = new Checker(filename);
		JSONObject report_obj = new JSONObject();
		report_obj.put("properties", report.displayDocInfo());
		report_obj.put("language", report.displayMainLanguage());
		report_obj.put("tagged_bool", report.displayTaggedBool());
		report_obj.put("tags_info", stree.traverseParentTree());
		report_obj.put("changes_made_bool", changes_made);
		System.out.print(report_obj);
		if (changes_made) {
			document.save(filename);
		}
	}
}
