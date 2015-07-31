package check;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.pdmodel.markedcontent.PDPropertyList;
import org.apache.pdfbox.util.PDFOperator;
import org.apache.pdfbox.util.PDFText2HTML;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;

public class ParseContentStream {
	static List<PDPage> pages;
	static PDDocument document;
	static TokenStack fonts = new TokenStack();
	
	
	public ParseContentStream( PDDocument doc, List<PDPage> pgs ) throws IOException {
		document = doc;
		pages = pgs;
		//fonts.push(new TextPosition());
	}
	
	public static void displayPageContent(int index) throws IOException {
		PDPage page = (PDPage) pages.get(index);		
		
		PDRectangle rect = page.getMediaBox();
		System.out.println(rect);
		PDGraphicsState state = new PDGraphicsState(rect);
		System.out.println(state.getCurrentTransformationMatrix());
		System.out.println(state.getTextState().getFontSize());
		
		List<Object> page_content_tokens = page.getContents().getStream().getStreamTokens();
		// Start a new content stream which will "hold" the to be created content
    	PDPageContentStream contentStream = new PDPageContentStream(document, page);
    	//contentStream.close();
    	TokenQueue new_tokens = new TokenQueue();
		StringWriter tagged_pdf = new StringWriter();
		tagged_pdf.write("<page>\n");
		
		TokenStack stack = new TokenStack();
		TokenStack tags_stack = new TokenStack();
		Map<Integer, String> mcids = new TreeMap<Integer, String>();
		//String page_contents = contents.getInputStreamAsString();
		int tokens_size = page_content_tokens.size();
		//System.out.println(size);
		Object item;
		String cmd;
		int id = 0;
		for (int i = 0; i < tokens_size; i++) {
			item = page_content_tokens.get(i);
			if (item instanceof PDFOperator) {			
				switch(((PDFOperator) item).getOperation()) {
				case ("Tf"):
					COSInteger size = (COSInteger) stack.pop();
					COSName font = (COSName) stack.pop();
					break;
				case ("Do"):
					COSName xobject_name = (COSName) stack.pop();
					PDXObject xobj = page.getResources().getXObjects().get(xobject_name);
					if (xobj instanceof PDXObjectImage) {
						String name = (String) new_tokens.dequeue(); //dequeue'
						cmd = String.format("/Figure <</MCID %d>> BDC %s Do ", id);
						new_tokens.enque(cmd);
					}
					break;
				case ("BI"):
					cmd = String.format("/Figure <</MCID %d>> BDC \n", id);
					new_tokens.enque(cmd);
					break;
				case ("EI"):
					cmd = "EMC EI \n";
					new_tokens.enque(cmd);
					break;
				case ("Tj"):
					Object str = stack.pop();
					tagged_pdf.write(((COSString) str).getString());
					break;
				case ("TJ"): {
					Object arr = stack.pop();
					StringWriter str_writer = new StringWriter();
					for (int j = 0; j < ((COSArray) arr).size(); j++) {
						COSBase obj = ((COSArray) arr).getObject(j);
						if (obj instanceof COSString) {
							str_writer.write(((COSString) obj).getString());
						} 
					}
					tagged_pdf.write(str_writer.toString());
					break;
				}
				case ("BDC"): //[tag properties]
				case ("DP"): { 
					Object prop_list = stack.pop();
					COSName tag = (COSName)stack.pop();
					tags_stack.push(tag);
					StringWriter prop = new StringWriter();
					String tag_with_prop = "";
					if (prop_list instanceof COSDictionary) {
						tag_with_prop = String.format("<%s %s>", tag.getName(),
										((COSDictionary) prop_list).entrySet());
						mcids.put(((COSDictionary)prop_list).getInt("MCID"), tag.getName());
					} else { //one of the values in the property list is an indirect object
						COSName ref = (COSName)prop_list;
						PDPropertyList properties = page.getResources().getProperties();
						COSDictionary properties_dict = (COSDictionary)properties.getCOSObject();
						COSDictionary prop_list_direct = (COSDictionary) properties_dict.getDictionaryObject(ref);
						tag_with_prop = String.format("<%s %s>", tag.getName(),
								((COSDictionary) prop_list_direct).entrySet());
						mcids.put(((COSDictionary)prop_list_direct).getInt("MCID"), tag.getName());
					}
					tagged_pdf.write(tag_with_prop);
					break;
				}
				case ("BMC"): //[tag]
				case ("MP"):  
					COSName tag = (COSName)stack.pop();
					tags_stack.push(tag);
					tagged_pdf.write(String.format("<%s>", tag.getName()));
					break;
				case ("EMC"):
					COSName last_tag = (COSName) tags_stack.pop();
					tagged_pdf.write(String.format("</%s>\n", last_tag.getName()));
					break;
				default:
					stack.push(item);
					new_tokens.enque(item.toString());
					break;
				}
			} else {
				stack.push(item);
				new_tokens.enque(item.toString());
			}
		}
		tagged_pdf.write("\n</page>");
		
		System.out.println(tagged_pdf.toString());	
	}
	
	public static void main(String[] args) throws IOException {
		String filename = "socialmicrovolunteering.pdf";
		PDDocument document = PDDocument.load(filename);
		List<PDPage> doc_pages = document.getDocumentCatalog().getAllPages();
		
		
		
		pages = doc_pages;
		
		//p.displayPageContent(1);
		/**PDFText2HTML stripper = new PDFText2HTML(null);
		stripper.setStartPage(0);
		StringWriter tagged_pdf = new StringWriter();
		//FileOutputStream fos = new FileOutputStream(new File("rand.html"));
	    stripper.writeText(document, tagged_pdf);
	    System.out.println(stripper.toString());*/
		
		
	}
}
