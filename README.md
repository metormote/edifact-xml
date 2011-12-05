Edifact XML Converter for Java
==============================

The Applied Models Edifact to XML converter for Java parses any Edifact input and outputs XML according to the ISO/TS 20625 standard. 
The converter implements the org.xml.sax.XMLParser interface, which means that it is easily integrated with standard xml processing tools. 
This implementation covers all Edifact directories from 88.1 up to D10A. 

Speed and simplicity
--------------------

The converter is extremely fast and very simple to use. No configuration is needed. 
It can easily handle many thousands of messages per second on standard hardware. 
See below for some examples of use.

License
-------
The Edifact-XML converter is licensed under GPL v3.0


XML Writer example
------------------

This example uses the Apache Commons XMLWriter class to output the XML the console.


	import java.io.Writer;
	import java.io.FileReader;
	import java.io.OutputStreamWriter;
	import org.xml.sax.InputSource;
	import org.xml.sax.XMLReader;
	import org.apache.ws.commons.serialize.XMLWriter;
	import org.apache.ws.commons.serialize.XMLWriterImpl;
	import com.appliedmodels.edifact.parser.EdifactParser;
	
	public class EdifactTest {
	
	    public static void main(String args[]) throws Exception {
	        Writer out=new OutputStreamWriter(System.out);
	        try {
	            XMLReader edifactParser=new EdifactParser();
	            XMLWriter handler=new XMLWriterImpl();
	            handler.setWriter(out);
	            edifactParser.setContentHandler(handler);
	            edifactParser.parse(new InputSource(new FileReader("/tmp/test.edi")));
	        }
	        finally {
	            out.flush();
	            out.close();
	        }
	    }
	}


XPath example
-------------
This example shows how to use XPath to process the XML Edifact.


	import com.appliedmodels.edifact.parser.EdifactParser;
	import java.io.FileReader;
	import javax.xml.transform.TransformerFactory;
	import javax.xml.transform.dom.DOMResult;
	import javax.xml.transform.sax.SAXSource;
	import javax.xml.xpath.XPath;
	import javax.xml.xpath.XPathConstants;
	import javax.xml.xpath.XPathExpression;
	import javax.xml.xpath.XPathFactory;
	import org.w3c.dom.NodeList;
	import org.xml.sax.InputSource;
	import org.xml.sax.XMLReader;
	
	/*
	 * 2010 Applied Models Ltd
	 * All rights reserved.
	 */
	public class EdifactXPathTest {
	
	    public static void main(String args[]) throws Exception {
	
	        XMLReader edifactParser=new EdifactParser();
	
	        //create a SAX source
	        InputSource inputSource=new InputSource(new FileReader("/tmp/test.edi"));
	        SAXSource saxSource=new SAXSource(edifactParser, inputSource);
	    
	        //obtain a DOM tree
	        DOMResult domResult=new DOMResult();
	        TransformerFactory transformerFactory=TransformerFactory.newInstance();
	        transformerFactory.newTransformer().transform(saxSource, domResult);
	
	        //evaluate an XPath that extracts all free text elements
	        XPath xpath = XPathFactory.newInstance().newXPath();
	        XPathExpression xpathExpr = xpath.compile("//S_FTX/C_C108/D_4440/text()");
	
	        Object result = xpathExpr.evaluate(domResult.getNode(), XPathConstants.NODESET);
	        NodeList nodes = (NodeList) result;
	        for (int i = 0; i < nodes.getLength(); i++) {
	            System.out.println(nodes.item(i).getNodeValue());
	        }
	    }
	}






