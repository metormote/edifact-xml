/*
 * 2010 Applied Models Ltd
 * All rights reserved.
 */

package com.appliedmodels.edifact.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author erik
 */
public class EdifactParser extends DefaultHandler implements XMLReader {

    public static final String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";
    public static final String FEATURE_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";

    public static final String PARSER_PACKAGE="com.appliedmodels.edifact.parser";

    protected boolean namespaces;

    protected boolean namespacePrefixes;

    protected ContentHandler contentHandler;
    
    protected ErrorHandler errorHandler;

    protected DTDHandler dtdHandler;

    protected EntityResolver entityResolver;

    protected SimpleCharStream stream;

    protected EdifactListener edifactListener;

    protected Map<String, EdifactDirectoryParser> parserCache=new HashMap<String, EdifactDirectoryParser>();

    private boolean parseUNH=false;
    private UNHInfo unhInfo=new UNHInfo();

    private boolean parseUIH=false;
    private UIHInfo uihInfo=new UIHInfo();

    String currentElement;


    public EdifactParser() {
        super();
    }

    
    public ContentHandler getContentHandler() {
        return contentHandler;
    }

    
    public DTDHandler getDTDHandler() {
        return dtdHandler;
    }

    
    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    
    public void setContentHandler(ContentHandler contentHandler) {
        this.contentHandler=contentHandler;
    }

    
    public void setDTDHandler(DTDHandler dtdHandler) {
        this.dtdHandler=dtdHandler;
    }

    
    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver=entityResolver;
    }

    
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler=errorHandler;
    }

    /*
     * Register an EdifactListener to get notified when a new 
     * Edifact message is parsed.
     *
    */
    public void setEdifactListener(EdifactListener edifactListener) {
        this.edifactListener = edifactListener;
    }

    /*
     * Return the EdifactListener or null if no listener is set.
     *
    */
    public EdifactListener getEdifactListener() {
        return edifactListener;
    }

    
    
    
    public boolean getFeature(String feature) throws SAXNotRecognizedException, SAXNotSupportedException {
        if(FEATURE_NAMESPACES.equals(feature)) {
            return namespaces;
        }
        else if(FEATURE_NAMESPACE_PREFIXES.equals(feature)) {
            return namespacePrefixes;
        }
        else {
            throw new SAXNotRecognizedException(feature);
        }
    }

    
    public Object getProperty(String property) throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException(property);
    }

    
    public void setFeature(String feature, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if(FEATURE_NAMESPACES.equals(feature)) {
            namespaces=value;
        }
        else if(FEATURE_NAMESPACE_PREFIXES.equals(feature)) {
            namespacePrefixes=value;
        }
        else {
            throw new SAXNotRecognizedException(feature);
        }

    }

    
    public void setProperty(String property, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException(property);
    }
    

    
    public void parse(String source) throws IOException, SAXException {
        parse(new InputSource(source));
    }

    
    public void parse(final InputSource source) throws IOException, SAXException {
        InputStream input=null;
        Reader reader=null;
        
        try {
            reader=source.getCharacterStream();
            if(reader==null)
            {
                if(source.getByteStream()!=null) {
                    if(source.getEncoding()!=null) {
                        reader=new InputStreamReader(source.getByteStream(), source.getEncoding());
                    }
                    else {
                        reader=new InputStreamReader(source.getByteStream());
                    }
                }
                else {
                    URLConnection urlCon=new URL(source.getSystemId()).openConnection();
                    input=urlCon.getInputStream();
                    reader=new BufferedReader(new InputStreamReader(urlCon.getInputStream(), urlCon.getContentEncoding()));
                }
                throw new IllegalArgumentException("no source found");
            }

            final EdifactReader ediReader=new EdifactReader(reader);
            stream=new SimpleCharStream(ediReader);

            contentHandler.setDocumentLocator(new Locator() {


                public String getPublicId() {
                    return source.getPublicId();
                }


                public String getSystemId() {
                    return source.getSystemId();
                }


                public int getLineNumber() {
                    return stream.getBeginLine()+ediReader.getDelta();
                }


                public int getColumnNumber() {
                    return stream.getBeginColumn();
                }
            });


            S401 s401=new S401();
            s401.setCharStream(stream);
            s401.setContentHandler(this);

            try {
                contentHandler.startDocument();
                contentHandler.startElement("", "INTERCHANGE", "INTERCHANGE", new AttributesImpl());

                for(;;) {

                    boolean eof=s401.parseServiceSegment();

                    if(eof) {
                        contentHandler.endElement("", "INTERCHANGE", "INTERCHANGE");
                        contentHandler.endDocument();
                        break;
                    }

                }
            }
            catch(ParseException pe) {
                if(pe.getCause()!=null) {
                    if(pe.getCause() instanceof SAXParseException) {
                        getErrorHandler().fatalError((SAXParseException)pe.getCause());
                    }
                    else if(pe.getCause() instanceof SAXException) {
                        throw (SAXException)pe.getCause();
                    }
                    else if(pe.getCause() instanceof IOException) {
                        throw (SAXException)pe.getCause();
                    }
                    else {
                        throw new SAXException((Exception)pe.getCause());
                    }
                }
                else {
                    throw new SAXException(pe);
                }
            }
        } finally {
            if(input!=null) {
                input.close();
            }
            if(reader!=null) {
                reader.close();
            }
        }
    }

    
    @Override
    public void startElement(String namespace, String localName, String qName, Attributes attributes) throws SAXException {
        if(parseUNH || parseUIH) {
            currentElement=qName;
        }
        else if("S_UNH".equals(qName)) {
            parseUNH=true;
        }
        else if("S_UIH".equals(qName)) {
            parseUIH=true;
        }
        else {
            contentHandler.startElement(namespace, localName, qName, attributes);
        }
    }

    
    @Override
    public void endElement(String namespace, String localName, String qName) throws SAXException {
        if(parseUNH)  {
            if("D_0051".equals(qName)) {
                if(edifactListener!=null) {
                    edifactListener.startMessage(unhInfo.type);
                }
                unhInfo.reportContent();
                parseUNH=false;
            }
            currentElement=null;
        }
        else if(parseUIH) {
            if("D_0054".equals(qName)) {
                if(edifactListener!=null) {
                    edifactListener.startMessage(unhInfo.type);
                }
                uihInfo.reportContent();
                parseUIH=false;
            }
            currentElement=null;
        }
        else {
            contentHandler.endElement(namespace, localName, qName);
        }

        try {
            if("S_UNH".equals(qName)) {

                String dir=unhInfo.version+unhInfo.release;

                EdifactDirectoryParser edp=getEdifactDirectoryParser(dir, stream);
                edp.setContentHandler(contentHandler);
                edp.parseMessage(unhInfo.type);

                String t="M_"+unhInfo.type;
                contentHandler.endElement("", t, t);

                if(edifactListener!=null) {
                    edifactListener.endMessage(unhInfo.type);
                }

                unhInfo.reset();
            }
            else if("S_UIH".equals(qName)) {

                String dir=uihInfo.version+uihInfo.release;

                EdifactDirectoryParser edp=getEdifactDirectoryParser(dir, stream);
                edp.setContentHandler(contentHandler);
                edp.parseMessage(uihInfo.type);

                String t="M_"+uihInfo.type;
                contentHandler.endElement("", t, t);

                if(edifactListener!=null) {
                    edifactListener.endMessage(unhInfo.type);
                }

                uihInfo.reset();
            }
        }
        catch(ParseException pe) {
            if(pe.getCause()!=null) {
                if(pe.getCause() instanceof SAXParseException) {
                    getErrorHandler().fatalError((SAXParseException)pe.getCause());
                }
                else if(pe.getCause() instanceof SAXException) {
                    throw (SAXException)pe.getCause();
                }
                else if(pe.getCause() instanceof IOException) {
                    throw (SAXException)pe.getCause();
                }
                else {
                    throw new SAXException((Exception)pe.getCause());
                }
            }
            else {
                throw new SAXException(pe);
            }
        }
    }

    
    @Override
    public void characters(char cbuf[], int offset, int length) throws SAXException {
        if(parseUNH) {
            if("D_0062".equals(currentElement)) {
                unhInfo.reference=new String(cbuf);
            }
            else if("D_0065".equals(currentElement)) {
                unhInfo.type=new String(cbuf);
            }
            else if("D_0052".equals(currentElement)) {
                unhInfo.version=new String(cbuf);
            }
            else if("D_0054".equals(currentElement)) {
                unhInfo.release=new String(cbuf);
            }
            else if("D_0051".equals(currentElement)) {
                unhInfo.agency=new String(cbuf);
            }
        }
        else if(parseUIH) {
            if("D_0065".equals(currentElement)) {
                uihInfo.type=new String(cbuf);
            }
            else if("D_0052".equals(currentElement)) {
                uihInfo.version=new String(cbuf);
            }
            else if("D_0054".equals(currentElement)) {
                uihInfo.release=new String(cbuf);
            }
        }
        else {
            contentHandler.characters(cbuf, offset, length);
        }
    }

    
    private EdifactDirectoryParser getEdifactDirectoryParser(String dir, SimpleCharStream stream) throws SAXException {
        dir=dir.toUpperCase();
        if(!parserCache.containsKey(dir)) {
            try {
                EdifactDirectoryParser edp=(EdifactDirectoryParser)Class.forName(PARSER_PACKAGE+"."+nomalizeName(dir)).newInstance();
                edp.setCharStream(stream);
                parserCache.put(dir, edp);
            }
            catch(InstantiationException ie) {
                ie.printStackTrace();
                throw new RuntimeException(ie);
            }
            catch(IllegalAccessException iae) {
                iae.printStackTrace();
                throw new RuntimeException(iae);
            }
            catch(ClassNotFoundException cnfe) {
                throw new SAXException(cnfe);
            }
        }
        return parserCache.get(dir);
    }

    private String nomalizeName(String name) {
        StringBuffer sb=new StringBuffer();
        if(Character.isDigit(name.charAt(0))) {
            sb.append('_');
        }
        for(int i=0;i<name.length();i++) {
            if(name.charAt(i)!='-') {
                sb.append(name.charAt(i));
            }
        }
        return sb.toString();
    }


    private final class UNHInfo {
        private final Attributes a=new AttributesImpl();

        String reference;
        String type;
        String version;
        String release;
        String agency;

        final void reportContent() throws SAXException {
            String t="M_"+type;
            contentHandler.startElement("", t, t, a);

            contentHandler.startElement("", "S_UNH", "S_UNH", a);
            contentHandler.startElement("", "D_0062", "D_0062", a);
            contentHandler.characters(reference.toCharArray(), 0, reference.length());
            contentHandler.endElement("", "D_0062", "D_0062");
            
            contentHandler.startElement("", "C_S009", "C_S009", a);
            contentHandler.startElement("", "D_0065", "D_0065", a);
            contentHandler.characters(type.toCharArray(), 0, type.length());
            contentHandler.endElement("", "D_0065", "D_0065");

            contentHandler.startElement("", "D_0052", "D_0052", a);
            contentHandler.characters(version.toCharArray(), 0, version.length());
            contentHandler.endElement("", "D_0052", "D_0052");

            contentHandler.startElement("", "D_0054", "D_0054", a);
            contentHandler.characters(release.toCharArray(), 0, release.length());
            contentHandler.endElement("", "D_0054", "D_0054");

            contentHandler.startElement("", "D_0051", "D_0051", a);
            contentHandler.characters(agency.toCharArray(), 0, agency.length());
            contentHandler.endElement("", "D_0051", "D_0051");

        }

        final void reset() {
            reference="";
            type="";
            version="";
            release="";
            agency="";
        }
    }

    private final class UIHInfo {
        private final Attributes a=new AttributesImpl();

        String type;
        String version;
        String release;

        final void reportContent() throws SAXException {
            String t="M_"+type;
            contentHandler.startElement("", t, t, a);
            
            contentHandler.startElement("", "S_UIH", "S_UIH", a);
            contentHandler.startElement("", "C_S306", "C_S306", a);
            contentHandler.startElement("", "D_0065", "D_0065", a);
            contentHandler.characters(type.toCharArray(), 0, type.length());
            contentHandler.endElement("", "D_0065", "D_0065");

            contentHandler.startElement("", "D_0052", "D_0052", a);
            contentHandler.characters(version.toCharArray(), 0, version.length());
            contentHandler.endElement("", "D_0052", "D_0052");

            contentHandler.startElement("", "D_0054", "D_0054", a);
            contentHandler.characters(release.toCharArray(), 0, release.length());
            contentHandler.endElement("", "D_0054", "D_0054");

        }

        final void reset() {
            type="";
            version="";
            release="";
        }
    }
}
