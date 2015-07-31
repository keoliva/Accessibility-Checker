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
package org.apache.fontbox.ttf;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

/**
 * A table in a true type font.
 * 
 * @author Ben Litchfield (ben@benlitchfield.com)
 * 
 */
public class NamingTable extends TTFTable
{
    /**
     * A tag that identifies this table type.
     */
    public static final String TAG = "name";
    
    private List<NameRecord> nameRecords = new ArrayList<NameRecord>();
    
    private String fontFamily = null;
    private String fontSubFamily = null;
    private String psName = null;
    
    
    /**
     * This will read the required data from the stream.
     * 
     * @param ttf The font that is being read.
     * @param data The stream to read the data from.
     * @throws IOException If there is an error reading the data.
     */
    public void initData( TrueTypeFont ttf, TTFDataStream data ) throws IOException
    {
        int formatSelector = data.readUnsignedShort();
        int numberOfNameRecords = data.readUnsignedShort();
        int offsetToStartOfStringStorage = data.readUnsignedShort();
        for( int i=0; i< numberOfNameRecords; i++ )
        {
            NameRecord nr = new NameRecord();
            nr.initData( ttf, data );
            nameRecords.add( nr );
        }
        for( int i=0; i<numberOfNameRecords; i++ )
        {
            NameRecord nr = nameRecords.get( i );
            data.seek( getOffset() + (2*3)+numberOfNameRecords*2*6+nr.getStringOffset() );
            int platform = nr.getPlatformId();
            int encoding = nr.getPlatformEncodingId();
            String charset = "ISO-8859-1";
            boolean isPlatform310 = false;
            boolean isPlatform10 = false;
            if( platform == 3 && (encoding == 1 || encoding == 0) )
            {
                charset = "UTF-16";
                isPlatform310 = true;
            }
            else if( platform == 2 )
            {
                if( encoding == 0 )
                {
                    charset = "US-ASCII";
                }
                else if( encoding == 1 )
                {
                    //not sure is this is correct??
                    charset = "ISO-10646-1";
                }
                else if( encoding == 2 )
                {
                    charset = "ISO-8859-1";
                }
            }
            else if ( platform == 1 && encoding == 0)
            {
                isPlatform10 = true;
            }
            String string = data.readString( nr.getStringLength(), charset );
            nr.setString( string );
            int nameID = nr.getNameId();
            if (nameID == NameRecord.NAME_FONT_FAMILY_NAME)
            {
                // prefer 3,1 or 3,0 platform/encoding use 1,0 as fallback
                if (isPlatform310 || (isPlatform10 && fontFamily == null))
                {
                    fontFamily = string;
                }
            }            
            else if (nameID == NameRecord.NAME_FONT_SUB_FAMILY_NAME)
            {
                // prefer 3,1 or 3,0 platform/encoding use 1,0 as fallback
                if (isPlatform310 || (isPlatform10 && fontSubFamily == null))
                {
                    fontSubFamily = string;
                }
            }            
            else if (nameID == NameRecord.NAME_POSTSCRIPT_NAME)
            {
                // prefer 3,1 or 3,0 platform/encoding use 1,0 as fallback
                if (isPlatform310 || (isPlatform10 && psName == null))
                {
                    psName = string;
                }
            }            
        }
        initialized = true;
    }
    
    /**
     * This will get the name records for this naming table.
     * 
     * @return A list of NameRecord objects.
     */
    public List<NameRecord> getNameRecords()
    {
        return nameRecords;
    }
    
    /**
     * Returns the font family name.
     * 
     * @return the font family name
     */
    public String getFontFamily()
    {
        return fontFamily;
    }

    /**
     * Returns the font sub family name.
     * 
     * @return the font sub family name
     */
    public String getFontSubFamily()
    {
        return fontSubFamily;
    }

    /**
     * Returns the postscript name.
     * 
     * @return the postscript name
     */
    public String getPSName()
    {
        return psName;
    }

}
