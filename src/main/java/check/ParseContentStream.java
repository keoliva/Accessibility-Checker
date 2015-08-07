package check;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

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
	//static Map<Float, StringBuilder> areas = new TreeMap<Float, StringBuilder>();
	static float max = 0.0f;
	//static StringBuilder max_str = new StringBuilder();
	
	public class TextPositionCompartor implements Comparator {
		@Override
		public int compare(Object t1, Object t2) {
			return (int) (((TextPosition) t2).getXDirAdj() - ((TextPosition) t1).getXDirAdj());
		}		
	}
	
	public ParseContentStream( PDPage page ) throws IOException {
		super( ResourceLoader.loadProperties(
                "org/apache/pdfbox/resources/PDFTextStripper.properties", true ) );
	}
	
	@Override
	protected void processTextPosition( TextPosition text ) {
		float tx = text.getX();
		float ty = text.getY();
		//System.out.println(String.format("(%s, %s): %s", tx, ty, text.getCharacter()));
		//System.out.println(String.format("(%s, %s): %s", text.getXDirAdj(), text.getYDirAdj(), text.getCharacter()));
		PriorityQueue<TextPosition> positions = areas.get(ty);
		//StringBuilder positions = areas.get(ty);
		if (positions == null) {
			positions = new PriorityQueue<TextPosition>(10, new TextPositionComparator());
			positions.add(text);
			//positions = new StringBuilder();
			//positions.append(text.getCharacter());
			areas.put(ty, positions);
		} else {
			positions.add(text);
			//positions.append(text.getCharacter());
			areas.put(ty, positions);
		}
		//System.out.println(text.toString());
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
		String filename = "CHI2016ProceedingsFormat.pdf";
        PDDocument document = PDDocument.load(filename);
        List doc_pages = document.getDocumentCatalog().getAllPages();
        PDPage pg;
		for (int i = 0; i < doc_pages.size(); ++i) {
			pg = (PDPage) doc_pages.get(i);
			ParseContentStream parser = new ParseContentStream(pg);
			//PDFTextStripper stripper = new PDFTextStripper();
			parser.processStream(pg, pg.getResources(), pg.getContents().getStream());
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
			break;
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
