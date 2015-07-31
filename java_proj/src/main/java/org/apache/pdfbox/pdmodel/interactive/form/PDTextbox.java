/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.pdmodel.interactive.form;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;

/**
 * A class for handling the PDF field as a textbox.
 *
 * @author sug
 * 
 */
public class PDTextbox extends PDVariableText
{

    /**
     * @see PDField#PDField(PDAcroForm,COSDictionary)
     *
     * @param theAcroForm The acroform.
     */
    public PDTextbox( PDAcroForm theAcroForm )
    {
        super( theAcroForm );
        setFieldType("Tx");
    }

    /**
     * @see org.apache.pdfbox.pdmodel.interactive.form.PDField#PDField(PDAcroForm,COSDictionary)
     *
     * @param theAcroForm The acroForm for this field.
     * @param field The field's dictionary.
     */
    public PDTextbox( PDAcroForm theAcroForm, COSDictionary field)
    {
        super( theAcroForm, field);
    }
    
    /**
     * Returns the maximum number of characters of the text field.
     * 
     * @return the maximum number of characters, returns -1 if the value isn't present
     */
    public int getMaxLen()
    {
        return getDictionary().getInt(COSName.MAX_LEN);
    }

    /**
     * Sets the maximum number of characters of the text field.
     * 
     * @param maxLen the maximum number of characters
     */
    public void setMaxLen(int maxLen)
    {
        getDictionary().setInt(COSName.MAX_LEN, maxLen);
    }

}
