package check;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.jempbox.xmp.XMPSchemaDublinCore;
import org.apache.jempbox.xmp.XMPSchemaPDF;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;

public class ResolveMetadata {
	private static PDDocumentCatalog root;
	private static PDDocumentInformation document_info;
	private static PDMetadata metadata;
	private COSDictionary docinfo;
	private boolean changes_made_to_metadata = false;
	private boolean changes_made_to_docinfo = false;
	
	private enum Status {
		DocInfoIsNullAndXMPIsNull,
		DocInfoIsNullAndXMPIsNotNull,
		DocInfoIsNotNullAndXMPIsNull,
		DocInfoIsNotNullAndXMPIsNotNull
	}
	
	private Status status;
	
	/**
	 * Constructor
	 * @param doc_info
	 * @param meta
	 */
	public ResolveMetadata( Checker checker ) {
		document_info = checker.document.getDocumentInformation();
		docinfo = document_info.getDictionary();
		root = checker.root;
		PDMetadata meta = root.getMetadata();
		metadata = meta;

		boolean metadata_is_null = (metadata == null);
		boolean docInfo_is_null = document_info.getMetadataKeys().isEmpty();
		if (docInfo_is_null) {
			if (metadata_is_null) {
				status = Status.DocInfoIsNullAndXMPIsNull;
				metadata = new PDMetadata(checker.document);
			}
			else status = Status.DocInfoIsNullAndXMPIsNotNull;
		} else {
			if (metadata_is_null) { 
				status = Status.DocInfoIsNotNullAndXMPIsNull;
				metadata = new PDMetadata(checker.document);
			}
			else status = Status.DocInfoIsNotNullAndXMPIsNotNull;
		}
	}
	
	public Map<String, Object> getProperties() {
		this.resolve();
		
		Map<String, String> orig_properties = new HashMap<String, String>();
		
		try {
			orig_properties.put("title", docinfo.getString(COSName.TITLE, ""));
			
			orig_properties.put("author", docinfo.getString(COSName.AUTHOR, ""));
			orig_properties.put("subject", docinfo.getString(COSName.SUBJECT, ""));
			orig_properties.put("keywords", docinfo.getString(COSName.KEYWORDS, ""));
		} catch ( Exception e ) { //root is null
		}
		
		Map<String, Object> properties = new HashMap<String, Object>();
		try {
			properties.put("title", docinfo.getString(COSName.TITLE, ""));
			
			properties.put("author", docinfo.getString(COSName.AUTHOR, ""));
			properties.put("subject", docinfo.getString(COSName.SUBJECT, ""));
			properties.put("keywords", docinfo.getString(COSName.KEYWORDS, ""));
			if (changes_made_to_docinfo)
				properties.put("orig_prop", orig_properties);
			properties.put("changes_made", changes_made_to_docinfo | changes_made_to_metadata);
		} catch ( Exception e ) { //root is null
		}
		return properties;
	}
	
	private String join(List list, String delim) {
		StringBuilder result = new StringBuilder();
		int size = list.size();
		if (size == 0)
			return "";
		result.append(list.get(0));
		for (int i = 1; i < size; i++) {
			result.append(delim);
			result.append(list.get(i));
			
		}
		return result.toString();
	}
	
	private boolean entriesAreNotDifferent(XMPSchemaDublinCore dc, XMPSchemaPDF pdf) {
		boolean result = false;
		//check titles
		if (!document_info.getTitle().toLowerCase().equals(dc.getTitle().toLowerCase()))
			return false;
		//check authors
		String doc_author = document_info.getAuthor().toLowerCase();
		for (String author : dc.getCreators()) {
			if (!doc_author.contains(author.toLowerCase()))
					return false;
		}
		//check subject
		if (!document_info.getSubject().toLowerCase().equals(dc.getDescription().toLowerCase()))
			return false;
		//check keywords
		if (!document_info.getKeywords().toLowerCase().equals(pdf.getKeywords().toLowerCase()))
			return false;
		return true;
	}
	
	public void resolve() {	
		try {
			XMPMetadata xmp = XMPMetadata.load(metadata.createInputStream());
			//xmp.asByteArray()
			//root.setMetadata(PDMetadata)
			XMPSchemaDublinCore dc = xmp.getDublinCoreSchema();
			XMPSchemaPDF pdf_schema = xmp.getPDFSchema();
			switch (status) {
				case DocInfoIsNullAndXMPIsNull:
					changes_made_to_metadata = true;
					break;
				case DocInfoIsNotNullAndXMPIsNull:
					dc.setTitle(docinfo.getString(COSName.TITLE, ""));
					for (String author : docinfo.getString(COSName.AUTHOR, "").split(",")) {
						dc.addCreator(author.trim());
					}
					dc.setDescription(docinfo.getString(COSName.AUTHOR, ""));
					pdf_schema.setKeywords(docinfo.getString(COSName.KEYWORDS, ""));
					try {
						metadata.importXMPMetadata(xmp.asByteArray());
						root.setMetadata(metadata);
						changes_made_to_metadata = true;
					} catch (Exception e) {
					}	
					break;
				case DocInfoIsNotNullAndXMPIsNotNull:
					if (entriesAreNotDifferent(dc, pdf_schema))
						break;
				case DocInfoIsNullAndXMPIsNotNull:			
					document_info.setTitle(dc.getTitle());
					document_info.setAuthor(join(dc.getCreators(), ", "));
					document_info.setSubject(dc.getDescription());
					document_info.setKeywords(pdf_schema.getKeywords());
					changes_made_to_docinfo = true;
					break;
				default:
					break;
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}	
	}
	
	public static void main(String[] args) throws IOException {
		String filename = "socialmicrovolunteering.pdf";
        Checker report = new Checker(filename);
        ResolveMetadata _this = new ResolveMetadata(report);
        System.out.println(_this.getProperties());

	}
	
}
