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
	private PDDocumentCatalog root;
	private PDDocumentInformation document_info;
	private PDMetadata metadata;
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
	
	/**
	 * gets a map including certain metadata (title, author, 
	 * subject, keywords), a boolean of whether or not any of the 
	 * two forms of metadata (xmp metadata, document information) 
	 * were updated, and if any values are updated, a dictionary 
	 * with the original entries are included
	 * @return a map where the values are about the metadata 
	 */
	public Map<String, Object> getProperties() {
		Map<String, String> orig_properties = new HashMap<String, String>();
		orig_properties.put("title", docinfo.getString(COSName.TITLE, ""));
		orig_properties.put("author", docinfo.getString(COSName.AUTHOR, ""));
		orig_properties.put("subject", docinfo.getString(COSName.SUBJECT, ""));
		orig_properties.put("keywords", docinfo.getString(COSName.KEYWORDS, ""));
		try {
			resolve();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Map<String, Object> properties = new HashMap<String, Object>(orig_properties);
		properties.put("changes_made", changes_made_to_docinfo | changes_made_to_metadata);
		if (changes_made_to_docinfo)
			properties.put("orig_prop", orig_properties);
			properties.put("title", docinfo.getString(COSName.TITLE, ""));
			properties.put("author", docinfo.getString(COSName.AUTHOR, ""));
			properties.put("subject", docinfo.getString(COSName.SUBJECT, ""));
			properties.put("keywords", docinfo.getString(COSName.KEYWORDS, ""));
		return properties;
	}
	
	/**
	 * joins the elements in a list with the delim parameter
	 * @param list
	 * @param delim the delimiter
	 * @return a string with the elements in the list delimited by 
	 * the delimiter
	 */
	private String join( List list, String delim ) {
		StringBuilder result = new StringBuilder();
		if (list == null) 
			return "";
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
	
	/**
	 * @param str
	 * @return str if it's not null and the empty string if it is
	 */
	private String resolveStr( String str ) {
		if (str == null)
			return "";
		return str.toLowerCase();
	}
	
	/**
	 * @param dc dublin core of the xmp metadata
	 * @param pdf pdf schema of the xmp metadata
	 * @return whether or not the document information important keys, 
	 * and the xmp metadata are different
	 */
	private boolean entriesAreNotDifferent( XMPSchemaDublinCore dc, XMPSchemaPDF pdf ) {
		boolean result = false;
		//check titles
		if (!resolveStr(document_info.getTitle()).equals(resolveStr(dc.getTitle())))
			return false;
		//check authors
		String doc_author = resolveStr(document_info.getAuthor());
		List<String> authors = dc.getCreators();
		if (authors == null) { 
			if (!doc_author.equals(""))
				return false;
		} else {
			for (String author : authors) {
				if (!doc_author.contains(author.toLowerCase()))
					return false;
			}
		}
		//check subject
		if (!resolveStr(document_info.getSubject()).equals(resolveStr(dc.getDescription())))
			return false;
		//check keywords
		if (!resolveStr(document_info.getKeywords()).equals(resolveStr(pdf.getKeywords())))
			return false;
		return true;
	}
	
	/**
	 * based on the status of the metadata, the necessary 
	 * changes are made
	 * @throws IOException
	 */
	public void resolve() throws IOException {	
		boolean loaded_xmp = false; 
		XMPMetadata xmp = new XMPMetadata(); 
		xmp.addDublinCoreSchema();
		xmp.addPDFSchema();
		XMPSchemaDublinCore dc = new XMPSchemaDublinCore(xmp);
		XMPSchemaPDF pdf_schema = new XMPSchemaPDF(xmp);
		try {
			xmp = XMPMetadata.load(metadata.createInputStream());
			loaded_xmp = true;
			//xmp.asByteArray()
			//root.setMetadata(PDMetadata)
			dc = xmp.getDublinCoreSchema();
			pdf_schema = xmp.getPDFSchema();
		} catch (IOException e1) {
			if (!loaded_xmp)
				xmp = new XMPMetadata(); 
				xmp.addDublinCoreSchema();
				xmp.addPDFSchema();
				dc = xmp.getDublinCoreSchema();
				pdf_schema = xmp.getPDFSchema();
		}	
		switch ( status ) {
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
	}
}
