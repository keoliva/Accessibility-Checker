package check;

import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSString;

class PDFTextObject{
    private List attributes = new ArrayList<Object>();
    private String text = "";
    private float x = -1;
    private float y = -1;

    public void clearAllAttributes(){
        attributes = new ArrayList<Object>();
    }

    public void addAttribute(Object anAttribute){
        attributes.add(anAttribute);
    }

    public void setText(){
        //Move the contents of the attributes to the text attribute.
        for (int i = 0; i < attributes.size(); i++){
            if (attributes.get(i) instanceof COSString){
                COSString aString = (COSString) attributes.get(i);
                text = text + aString.getString();
            } else if (attributes.get(i) instanceof COSArray) {
            	COSArray tj = (COSArray) attributes.get(i);
            	String txt = "";
            	for (int j = 0; j < tj.size(); j++) {
            		if (tj.get(j) instanceof COSString) {
            			txt += ((COSString) tj.get(j)).getString();
            		}
            	}
            	text = text + txt;
            }
            else {
                System.out.println("Whoops! Wrong type of property...");
            }
        }
    }

    public String getText(){
        return text;
    }

    public void setMatrix(){
        //Move the contents of the attributes to the x and y attributes.
        //A Matrix has 6 attributes, the last two of which are x and y
        for (int i = 4; i < attributes.size(); i++){
            float curval = -1;
            if (attributes.get(i) instanceof COSInteger){
                COSInteger aCOSInteger = (COSInteger) attributes.get(i); 
                curval = aCOSInteger.floatValue();

            }
            if (attributes.get(i) instanceof COSFloat){
                COSFloat aCOSFloat = (COSFloat) attributes.get(i);
                curval = aCOSFloat.floatValue();
            }
            switch(i) {
                case 4:
                    x = curval;
                    break;
                case 5:
                    y = curval;
                    break;
            }
        }
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }
}