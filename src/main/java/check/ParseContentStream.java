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

public class ParseContentStream extends PDFStreamEngine {
	static Map<Float, PriorityQueue> areas = new TreeMap<Float, PriorityQueue>();
	static float max = 0.0f;
	static PDPage curr_page;
	
	public class TextPositionCompartor implements Comparator {
		@Override
		public int compare(Object t1, Object t2) {
			return (int) (((TextPosition) t2).getXDirAdj() - ((TextPosition) t1).getXDirAdj());
		}		
	}
	
	public ParseContentStream( PDPage page ) throws IOException {
		super( ResourceLoader.loadProperties(
                "org/apache/pdfbox/resources/PDFTextStripper.properties", true ) );
		curr_page = page;
	}
	
	/**
	 * parses page for the "Do" operator which signifies that an image is being painted
	 * @author Summer Kitahara
	 * @return a set of all the marked content IDs of the marked images in the content 
	 * stream
	 */
	public Set<Integer> getImageMCIDs() {
		try {
			List<Object> tokens = curr_page.getContents().getStream().getStreamTokens();
			Set<Integer> mcids = new HashSet<Integer>();
			List<Integer> all_indices = new ArrayList<Integer>();
			int i = 0; ListIterator<Integer> op_iterator;;
			for (Iterator<Object> iter = tokens.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				if (obj instanceof PDFOperator) {
					String operation = ((PDFOperator) obj).getOperation();
					switch( operation ) {
						case "Do"://An image is being painted, refers to indirect reference to an image
						case "BI"://Refers to an inline image
						case "BDC"://Beginning of marked content with a properties list as an operand
						case "EMC"://Ending of marked content
							all_indices.add(i);
							if (operation.equals("Do") | operation.equals("BI")) {
								//pointer is next to the operator before this Do
								op_iterator = all_indices.listIterator(all_indices.size()-1);
								if (op_iterator.hasPrevious()) {
									int prev_i = op_iterator.previous();
									String prev_op = ((PDFOperator) tokens.get(prev_i)).getOperation();
									
									if (prev_op.equals("BDC")) {
										//the operand of BDC is a COSDictionary
										int mcid = ((COSDictionary) tokens.get(prev_i-1)).getInt(COSName.MCID);
										if (mcid >= 0) mcids.add(mcid);
									}
								}
							}
							break;
						default: break;
					}
				}
				i++;
			}
			return mcids;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new HashSet<Integer>();
	}
	
	@Override
	protected void processTextPosition( TextPosition text ) {
		float tx = text.getX();
		float ty = text.getY();
		PriorityQueue<TextPosition> positions = areas.get(ty);

		if (positions == null) {
			positions = new PriorityQueue<TextPosition>(10, new TextPositionComparator());
			positions.add(text);

			areas.put(ty, positions);
		} else {
			positions.add(text);
			areas.put(ty, positions);
		}
		if (text.getFontSizeInPt() > max) {
			max = text.getFontSizeInPt();
		}
	}
	
    /**
     * Process a sub stream of the current stream.
     *
     * @param aPage The page used for drawing.
     * @param resources The resources used when processing the stream.
     * @param cosStream The stream to process.
     *
     * @throws IOException If there is an exception while processing the stream.
     */
	private void processSubStream(COSStream cosStream) throws IOException 
    {
        List<COSBase> arguments = new ArrayList<COSBase>();
        PDFStreamParser parser = new PDFStreamParser(cosStream, false);
        try 
        {
            Iterator<Object> iter = parser.getTokenIterator();
            while (iter.hasNext()) 
            {
                Object next = iter.next();
                if (next instanceof COSObject) 
                {
                    arguments.add(((COSObject) next).getObject());
                }
                else if (next instanceof PDFOperator) 
                {
                	System.out.println(next);
                    super.processOperator((PDFOperator) next, arguments);
                    arguments = new ArrayList<COSBase>();
                }
                else
                {
                    arguments.add((COSBase) next);
                }
            }
        }
        finally
        {
            parser.close();
        }
    }
	
	public static void main(String[] args) throws IOException { 
		//String filename = "VISM_ASSETS_Camera_Ready.pdf";
		String filename = "socialmicrovolunteering.pdf";
        PDDocument document = PDDocument.load(filename);
        List doc_pages = document.getDocumentCatalog().getAllPages();
        PDPage pg;
		for (int i = 0; i < doc_pages.size(); ++i) {
			pg = (PDPage) doc_pages.get(i);
			ParseContentStream parser = new ParseContentStream(pg);
			System.out.println("Page "+ i);
			System.out.println(parser.getImageMCIDs());
			//PDFTextStripper stripper = new PDFTextStripper();
			/**parser.processStream(pg, pg.getResources(), pg.getContents().getStream());
			System.out.println("Page: " + i);
			//System.out.println(areas);
			//System.out.println();
			Set<Float> keys = areas.keySet();
			for (Float key: keys) {
				System.out.print(key);
				System.out.println(areas.get(key));
				//PriorityQueue<TextPosition> positions = areas.get(key);
				//for (TextPosition pos : positions) {
					//System.out.println(String.format("%s", pos.toString()));
				//}
				break;
			}
			System.out.println(max);
			areas = new TreeMap<Float, PriorityQueue>();
			//areas = new TreeMap<Float, StringBuilder>();
			break;*/
			
		}
		/**try {
			String filename = "CHI2016ProceedingsFormat.pdf";
	        PDDocument document = PDDocument.load(filename);
	        List allPages = document.getDocumentCatalog().getAllPages();
	        // just parsing page 2 here, as it's only a sample
	        PDPage page = (PDPage) allPages.get(0);
	        PDStream contents = page.getContents();
	        PDFStreamParser parser = new PDFStreamParser(contents.getStream());
	        parser.parse();  
	        List tokens = parser.getTokens();  
	        boolean parsingTextObject = false; //boolean to check whether the token being parsed is part of a TextObject
	        PDFTextObject textobj = new PDFTextObject();
	        for (int i = 0; i < tokens.size(); i++)  
	        {  
	            Object next = tokens.get(i); 
	            if (next instanceof PDFOperator)  {
	                PDFOperator op = (PDFOperator) next;  
	                switch(op.getOperation()){
	                    case "BT":
	                        //BT: Begin Text. 
	                        parsingTextObject = true;
	                        textobj = new PDFTextObject();
	                        break;
	                    case "ET":
	                        parsingTextObject = false;
	                        System.out.println("Text: " + textobj.getText() + "@" + textobj.getX() + "," + textobj.getY());
	                        break;
	                    case "Tj":
	                    
	                        textobj.setText();
	                        break;
	                    case "Tm":
	                        textobj.setMatrix();
	                        break;
	                    default:
	                        //System.out.println("unsupported operation " + op.getOperation());
	                }
	                textobj.clearAllAttributes();
	            }
	            else if (parsingTextObject)  {
	                textobj.addAttribute(next);
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }*/
	}
}
