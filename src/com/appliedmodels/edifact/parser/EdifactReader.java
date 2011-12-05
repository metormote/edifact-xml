/*
 * 2009 Applied Models Ltd
 * All rights reserved.
 */

package com.appliedmodels.edifact.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * This reader filters an edifact stream. It replaces non standard escape characters
 * with the standard set and removes unwanted whitespace.
 * 
 * @author erik
 */
public class EdifactReader extends BufferedReader {

    public static final String SERVICESTRING=":+.?*'";
    public static final String LINEBREAKS="\n\r\f";
    public static final int NEWLINE='\n';
    
    public static final int ESCAPE='?';
    public static final int DATASEP='+';
    public static final int COMPDATASEP=':';
    public static final int REPEATSEP='*';
    public static final int SEGTERM='\'';
    public static final int DECIMAL='.';

    public int escape=ESCAPE;
    public int datasep=DATASEP;
    public int compdatasep=COMPDATASEP;
    public int repeatsep=REPEATSEP;
    public int segterm=SEGTERM;
    public int decimal=DECIMAL;
    
    private boolean standard=true;
    private boolean escaped=false;
    private boolean newline=false;

    private int escapedChar;

    private int delta=0;

    public EdifactReader(Reader parent) throws IOException {
        super(parent);
        init();
    }

    public EdifactReader(Reader parent, int bufSize) throws IOException {
        super(parent, bufSize);
        init();
    }

    private void init() throws IOException {
        mark(256);
        int c,i=0;
        for(c=super.read();Character.isWhitespace(c);c=super.read()) {
            if(c==-1 || i>255) {
                throw new IOException("Not a valid Edifact stream: expected UNA, UNB, UIB, UNH or UIH");
            }
            i++;
        }

        char cbuf[]={(char)c, (char)super.read(), (char)super.read()};
        String tag=new String(cbuf);
        
        if("UNA".equals(tag)) {

            compdatasep=super.read();
            if(compdatasep!=COMPDATASEP) {
                standard=false;
            }

            datasep=super.read();
            if(datasep!=DATASEP) {
                standard=false;
            }

            decimal=super.read();
            if(decimal!=DECIMAL) {
                standard=false;
            }

            escape=super.read();
            if(escape!=ESCAPE) {
                standard=false;
            }

            repeatsep=super.read();
            if(repeatsep!=REPEATSEP && repeatsep!=' ') {
                standard=false;
            }

            segterm=super.read();
            if(segterm!=SEGTERM) {
                standard=false;
            }

            delta=9;
        }
        else if("UNB".equals(tag)) {
            reset();
        }
        else if("UIB".equals(tag)) {
            reset();
        }
        else if("UNH".equals(tag)) {
            reset();
        }
        else if("UIH".equals(tag)) {
            reset();
        }
        else {
            throw new IOException("Not a valid Edifact stream: got "+tag+", expected UNA, UNB, UIB, UNH or UIH");
        }

        
    }

    @Override
    public final int read() throws IOException {
        if(escaped) {
            escaped=false;
            return escapedChar;
        }
        else if(newline) {
            newline=false;
            return NEWLINE;
        }

        int c=super.read();
        //System.out.print(c);
        if(LINEBREAKS.indexOf(c)!=-1) {
            return read();
        }
        else if(standard) {
            return c;
        }
        else {
            if(c==compdatasep) {
                return COMPDATASEP;
            }
            else if(c==datasep) {
                return DATASEP;
            }
            else if(c==segterm) {
                newline=true;
                return SEGTERM;
            }
            else if(c==repeatsep && c!=' ') {
                return REPEATSEP;
            }
            else if(c==escape) {
                int e=super.read();
                if(escape(e)) {
                    escapedChar=c;
                    escaped=true;
                    return ESCAPE;
                }
                else {
                    return e;
                }
            }
            else if(escape(c)) {
                escapedChar=c;
                escaped=true;
                return ESCAPE;
            }
            else {
                return c;
            }
        }
    }

    @Override
    public final int read(char[] cbuf, int off, int len) throws IOException {
        if(standard) {
            return super.read(cbuf, off, len);
        }
        else {
            int n=read();
            int i;
            for(i=0;i<len && n!=-1;i++)
            {
                cbuf[off+i]=(char)n;
                n=read();
            }
            return i;
        }
    }

    @Override
    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    @Override
    public String readLine() throws IOException {
        throw new RuntimeException("readLine() not implemented");
    }


    public int getDelta() {
        return delta;
    }


    private static final boolean escape(int c) {
        switch(c) {
            case COMPDATASEP:
            case DATASEP:
            case SEGTERM:
            case REPEATSEP:
            case ESCAPE:
                return true;
            default:
                return false;

        }
    }
}
