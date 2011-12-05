/*
 * 2009 Applied Models Ltd
 * All rights reserved.
 */

package com.appliedmodels.edifact.parser;

/**
 * Implement this interface to register a listener to be notified when a new
 * message is parsed.
 * 
 * @author erik
 */
public interface EdifactListener {


    public void startMessage(String type);

    public void endMessage(String type);
    
}
