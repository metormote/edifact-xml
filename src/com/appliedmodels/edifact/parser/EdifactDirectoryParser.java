/*
 * 2009 Applied Models Ltd
 * All rights reserved.
 */

package com.appliedmodels.edifact.parser;

import org.xml.sax.ContentHandler;

/**
 *
 * @author erik
 */
public interface EdifactDirectoryParser {

    public void setContentHandler(ContentHandler handler);

    public ContentHandler getContentHandler();
    
    public void setCharStream(SimpleCharStream input);

    public void parseMessage(String messageType) throws ParseException;

    public boolean parseServiceSegment() throws ParseException;
}
