package check;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.jempbox.xmp.XMPSchemaDublinCore;
import org.apache.jempbox.xmp.XMPSchemaPDF;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;

public class ResolveMetadata {
	private static PDDocumentInformation document_info;
	private static PDMetadata metadata;
	
	private enum Status {
		DocInfoIsNullAndXMPIsNull,
		DocInfoIsNullAndXMPIsNotNull,
		DocInfoIsNotNullAndXMPIsNull,
		DocInfoIsNotNullandXMPIsNotNull
	}
	
	private Status status;
	public Map<String, String> properties = new HashMap<String, String>();
	
	/**
	 * Constructor
	 * @param doc_info
	 * @param meta
	 */
	public ResolveMetadata( Checker checker ) {
		document_info = checker.document.getDocumentInformation();
		PDMetadata meta = checker.root.getMetadata();
		if (meta == null)
			metadata = new PDMetadata(checker.document);
		else
			metadata = meta;
		try {
			XMPMetadata xmp = XMPMetadata.load(metadata.createInputStream());
			XMPSchemaDublinCore dc = xmp.getDublinCoreSchema();
			List<String> authors = dc.getCreators();
			String subject = dc.getDescription();
			String title = dc.getTitle();
			XMPSchemaPDF pdf_schema = xmp.getPDFSchema();
			String keywords = pdf_schema.getKeywords();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
	}
	
}
