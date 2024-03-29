/*
 * Copyright (C) 2003, 2004  Pascal Essiembre, Essiembre Consultant Inc.
 * 
 * This file is part of Essiembre ResourceBundle Editor.
 * 
 * Essiembre ResourceBundle Editor is free software; you can redistribute it 
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * Essiembre ResourceBundle Editor is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with Essiembre ResourceBundle Editor; if not, write to the 
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330, 
 * Boston, MA  02111-1307  USA
 */
package com.essiembre.eclipse.rbe.model.bundle;

import java.util.Iterator;

import com.essiembre.eclipse.rbe.model.workbench.RBEPreferences;

/**
 * Bundle-related utility methods. 
 * @author Pascal Essiembre (essiembre@users.sourceforge.net)
 * @version $Author: essiembre $ $Revision: 1.10 $ $Date: 2007/02/02 04:13:43 $
 */
public final class PropertiesGenerator {

    /** Generator header comment. */
    public static final String GENERATED_BY = 
            "#Generated by ResourceBundle Editor " //$NON-NLS-1$
          + "(http://eclipse-rbe.sourceforge.net)"; //$NON-NLS-1$

    /** A table of hex digits */
    private static final char[] HEX_DIGITS = {
        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };

    /** Special resouce bundle characters when persisting any text. */
    private static final String SPECIAL_VALUE_SAVE_CHARS = 
            "\t\f"; //$NON-NLS-1$
    /** Special resouce bundle characters when persisting keys. */
    private static final String SPECIAL_KEY_SAVE_CHARS = 
            "=\t\f#!: "; //$NON-NLS-1$
    
    /** System line separator. */
    private static final String SYSTEM_LINE_SEP = 
            System.getProperty("line.separator"); //$NON-NLS-1$
    /** Forced line separators. */
    private static final String[] FORCED_LINE_SEP = new String[3];
    static {
        FORCED_LINE_SEP[RBEPreferences.NEW_LINE_UNIX] = "\\\\n"; //$NON-NLS-1$
        FORCED_LINE_SEP[RBEPreferences.NEW_LINE_WIN] = 
                "\\\\r\\\\n"; //$NON-NLS-1$
        FORCED_LINE_SEP[RBEPreferences.NEW_LINE_MAC] = "\\\\r"; //$NON-NLS-1$
    }

    /**
     * Constructor.
     */
    private PropertiesGenerator() {
        super();
    }

    /**
     * Generates a formatted string from a given <code>Bundle</code>.  The
     * returned string will conform to documented properties file structure.
     * @param bundle the bundle used to generate the string
     * @return the generated string
     */
    public static String generate(Bundle bundle) {
        String lineBreak = SYSTEM_LINE_SEP;
        int numOfLineBreaks = RBEPreferences.getGroupLineBreaks();
        StringBuffer text = new StringBuffer();

        // Header comment
        String headComment = bundle.getComment();
        if (headComment != null && headComment.length() > 0) {
            if (RBEPreferences.getShowGenerator() 
                    && !headComment.startsWith(GENERATED_BY)) {
                text.append(GENERATED_BY);
                text.append(SYSTEM_LINE_SEP);
            }
            text.append(headComment);
        }
        
        // Format
        String group = null;
        int equalIndex = -1;
        for (Iterator iter = bundle.getKeys().iterator(); iter.hasNext();) {
            BundleEntry bundleEntry = bundle.getEntry((String) iter.next());
            String key = bundleEntry.getKey();
            String value = bundleEntry.getValue(); 
            String comment = bundleEntry.getComment();    
            
            if (value != null){
                // escape backslashes
                if (RBEPreferences.getConvertUnicodeToEncoded()) {
                    value = value.replaceAll(
                            "\\\\", "\\\\\\\\");//$NON-NLS-1$ //$NON-NLS-2$
                }
                
                // handle new lines in value
                if (RBEPreferences.getForceNewLineType()) {
                    value = value.replaceAll(
                            "\r\n|\r|\n", FORCED_LINE_SEP[ //$NON-NLS-1$
                                    RBEPreferences.getNewLineType()]);
                } else {
                    value = value.replaceAll(
                            "\r", "\\\\r"); //$NON-NLS-1$ //$NON-NLS-2$
                    value = value.replaceAll(
                            "\n", "\\\\n"); //$NON-NLS-1$ //$NON-NLS-2$
                }
            } else {
                value = ""; //$NON-NLS-1$
            }
            
            if (RBEPreferences.getKeepEmptyFields() || value.length() > 0) {
                // handle group equal align and line break options
                if (RBEPreferences.getGroupKeys()) {
                    String newGroup = getKeyGroup(key);
                    if (newGroup == null || !newGroup.equals(group)) {
                        group = newGroup;
                        equalIndex = getEqualIndex(key, group, bundle);
                        for (int i = 0; i < numOfLineBreaks; i++) {
                            text.append(lineBreak);
                        }
                    }
                } else {
                    equalIndex = getEqualIndex(key, null, bundle);
                }
                
                // Build line
                if (RBEPreferences.getConvertUnicodeToEncoded()) {
                    key = PropertiesGenerator.convertUnicodeToEncoded(key);
                    value = PropertiesGenerator.convertUnicodeToEncoded(value);
                }
                if (comment != null && comment.length() > 0) {
                    text.append(comment);
                }
                appendKey(text, key, equalIndex, bundleEntry.isCommented());
                appendValue(text, value, equalIndex, bundleEntry.isCommented());
                text.append(lineBreak);
            }
        }
        return text.toString();
    }
        
    /**
     * Converts unicodes to encoded &#92;uxxxx.
     * @param str string to convert
     * @return converted string
     * @see java.util.Properties
     */
    public static String convertUnicodeToEncoded(String str) {
        int len = str.length();
        StringBuffer outBuffer = new StringBuffer(len * 2);

        for (int x = 0; x < len; x++) {
            char aChar = str.charAt(x);
            if ((aChar < 0x0020) || (aChar > 0x007e)) {
                outBuffer.append('\\');
                outBuffer.append('u');
                outBuffer.append(toHex((aChar >> 12) & 0xF));
                outBuffer.append(toHex((aChar >> 8) & 0xF));
                outBuffer.append(toHex((aChar >> 4) & 0xF));
                outBuffer.append(toHex(aChar & 0xF));
            } else {
                outBuffer.append(aChar);
            }
        }
        return outBuffer.toString();
    }
    
    /**
     * Converts a nibble to a hex character
     * @param nibble  the nibble to convert.
     * @return a converted character
     */
    private static char toHex(int nibble) {
        char hexChar = HEX_DIGITS[(nibble & 0xF)];
        if (!RBEPreferences.getConvertUnicodeToEncodedUpper()) {
            return Character.toLowerCase(hexChar);
        }
        return hexChar;
    }

    /**
     * Appends a value to resource bundle content.
     * @param text the resource bundle content so far
     * @param value the value to add
     * @param equalIndex the equal sign position
     * @param commented is the value commented
     */
    private static void appendValue(
            StringBuffer text, String value, 
            int equalIndex, boolean commented) {
        if (value != null) {
            // Escape potential leading spaces.
            if (value.startsWith(" ")) { //$NON-NLS-1$
                value = "\\" + value; //$NON-NLS-1$
            }
            int lineLength = RBEPreferences.getWrapCharLimit() - 1;
            int valueStartPos = equalIndex;
            if (RBEPreferences.getSpacesAroundEqualSigns()) {
                valueStartPos += 3;
            } else {
                valueStartPos += 1;
            }
            
            // Break line after escaped new line
            if (RBEPreferences.getNewLineNice()) {
                value = value.replaceAll(
                        "(\\\\r\\\\n|\\\\r|\\\\n)", //$NON-NLS-1$
                        "$1\\\\" + SYSTEM_LINE_SEP); //$NON-NLS-1$
            }
            // Wrap lines
            if (RBEPreferences.getWrapLines() && valueStartPos < lineLength) {
                StringBuffer valueBuf = new StringBuffer(value);
                while (valueBuf.length() + valueStartPos > lineLength
                        || valueBuf.indexOf("\n") != -1) { //$NON-NLS-1$
                    int endPos = Math.min(
                            valueBuf.length(), lineLength - valueStartPos);
                    String line = valueBuf.substring(0, endPos);
                    int breakPos = line.indexOf(SYSTEM_LINE_SEP);
                    if (breakPos != -1) {
                        endPos = breakPos + SYSTEM_LINE_SEP.length();
                        saveValue(text, valueBuf.substring(0, endPos));
                        //text.append(valueBuf.substring(0, endPos));
                    } else {
                        breakPos = line.lastIndexOf(' ');
                        if (breakPos != -1) {
                            endPos = breakPos + 1;
                            saveValue(text, valueBuf.substring(0, endPos));
                            //text.append(valueBuf.substring(0, endPos));
                            text.append("\\"); //$NON-NLS-1$
                            text.append(SYSTEM_LINE_SEP);
                        }
                    }
                    valueBuf.delete(0, endPos);
                    // Figure out starting position for next line
                    if (!RBEPreferences.getWrapAlignEqualSigns()) {
                        valueStartPos = RBEPreferences.getWrapIndentSpaces();
                    }

                    if (commented && valueStartPos > 0) {
                        text.append("##"); //$NON-NLS-1$
                    }

                    for (int i = 0; i < valueStartPos; i++) {
                        text.append(' ');
                    }
                }
                text.append(valueBuf);
            } else {
                saveValue(text, value);
                //text.append(value);
            }
        }
    }

    /**
     * Appends a key to resource bundle content.
     * @param text the resource bundle content so far
     * @param key the key to add
     * @param equalIndex the equal sign position
     * @param commented is the key commented out
     */
    private static void appendKey(
            StringBuffer text, String key, int equalIndex, boolean commented) {

        if (commented) {
            text.append("##"); //$NON-NLS-1$
        }
        
        // Escape and persist the rest
        saveKey(text, key);
//        text.append(key);
        for (int i = 0; i < equalIndex - key.length(); i++) {
            text.append(' ');
        }
        if (RBEPreferences.getSpacesAroundEqualSigns()) {
            text.append(" = "); //$NON-NLS-1$
        } else {
            text.append("="); //$NON-NLS-1$
        }
    }
    
    
    private static void saveKey(StringBuffer buf, String str) {
        saveText(buf, str, SPECIAL_KEY_SAVE_CHARS);
    }
    private static void saveValue(StringBuffer buf, String str) {
        saveText(buf, str, SPECIAL_VALUE_SAVE_CHARS);
    }
    
    /**
     * Saves some text in a given buffer after converting special characters.
     * @param buf the buffer to store the text into
     * @param str the value to save
     * @param escapeChars characters to escape
     */
    private static void saveText(
            StringBuffer buf, String str, String escapeChars) {
        int len = str.length();
        for(int x = 0; x < len; x++) {
            char aChar = str.charAt(x);
            if (escapeChars.indexOf(aChar) != -1) {
                buf.append('\\');
            }
            buf.append(aChar);
        }
    }
    
    /**
     * Gets the group from a resource bundle key.
     * @param key the key to get a group from
     * @return key group
     */
    private static String getKeyGroup(String key) {
        String sep = RBEPreferences.getKeyGroupSeparator();
        int deepness = RBEPreferences.getGroupLevelDeepness();
        int endIndex = 0;
        int levelFound = 0;
        
        for (int i = 0; i < deepness; i++) {
            int sepIndex = key.indexOf(sep, endIndex);
            if (sepIndex != -1) {
                endIndex = sepIndex + 1;
                levelFound++;
            }
        }
        if (levelFound != 0) {
            if (levelFound < deepness) {
                return key;
            }
            return key.substring(0, endIndex - 1);
        }
        return null;
    }
    
    /**
     * Gets the position where the equal sign should be located for
     * the given group.
     * @param key resource bundle key
     * @param group resource bundle key group
     * @param bundle resource bundle
     * @return position
     */
    private static int getEqualIndex(String key, String group, Bundle bundle) {
        int equalIndex = -1;
        boolean alignEquals = RBEPreferences.getAlignEqualSigns();
        boolean groupKeys = RBEPreferences.getGroupKeys();
        boolean groupAlignEquals = RBEPreferences.getGroupAlignEqualSigns();

        // Exit now if we are not aligning equals
        if (!alignEquals || groupKeys && !groupAlignEquals 
                         || groupKeys && group == null) {
            return key.length();
        }
        
        // Get equal index
        for (Iterator iter = bundle.getKeys().iterator(); iter.hasNext();) {
            String iterKey = (String) iter.next();
            if (!groupKeys || groupAlignEquals && iterKey.startsWith(group)) {
                int index = iterKey.length();
                if (index > equalIndex) {
                    equalIndex = index;
                }
            }
        }        
        return equalIndex;
    }
}
