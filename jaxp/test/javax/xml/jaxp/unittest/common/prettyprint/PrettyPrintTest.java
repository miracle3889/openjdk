/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package common.prettyprint;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/*
 * @test
 * @bug 6439439 8087303
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true common.prettyprint.PrettyPrintTest
 * @run testng/othervm common.prettyprint.PrettyPrintTest
 * @summary Test serializing xml and html with indentation.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class PrettyPrintTest {
    /*
     * test CDATA, elements only, text and element, whitespace and element,
     * xml:space property and nested xml:space property, mixed node types.
     */
    @DataProvider(name = "xml-data")
    public Object[][] xmlData() throws Exception {
        return new Object[][] {
                { read("xmltest1.xml"), read("xmltest1.out") },
                { read("xmltest2.xml"), read("xmltest2.out") },
                { read("xmltest3.xml"), read("xmltest3.out") },
                { read("xmltest4.xml"), read("xmltest4.out") },
                { read("xmltest5.xml"), read("xmltest5.out") },
                { read("xmltest6.xml"), read("xmltest6.out") },
                { read("xmltest7.xml"), read("xmltest7.out") },
                { read("xmltest8.xml"), read("xmltest8.out") } };
    }

    /*
     * @bug 8087303
     * Test the whitespace text nodes are serialized with pretty-print by LSSerializer and transformer correctly
     *
     */
    @Test(dataProvider = "xml-data")
    public void testXMLPrettyPrint(String source, String expected) throws Exception {
        // test it's no change if no pretty-print
        String result = serializerWrite(toXmlDocument(source), false);
        assertTrue(toXmlDocument(source).isEqualNode(toXmlDocument(result)), "The actual is: " + result);
        // test pretty-print
        assertEquals(serializerWrite(toXmlDocument(source), true), expected);
        // test it's no change if no pretty-print
        result = transform(toXmlDocument(source), false);
        assertTrue(toXmlDocument(source).isEqualNode(toXmlDocument(result)), "The actual is: " + result);
        // test pretty-print
        assertEquals(transform(toXmlDocument(source), true).replaceAll("\r\n", "\n"), expected);
    }

    /*
     * test pure text content, and sequent Text nodes.
     */
    @DataProvider(name = "xml-node-data")
    public Object[][] xmlNodeData() throws Exception {
        return new Object[][] {
                { newTextNode(read("nodetest1.txt")), read("nodetest1.out") },
                { createDocWithSequentTextNodes(), read("nodetest2.out") } };
    }

    /*
     * @bug 8087303
     * Test the whitespace text nodes are serialized with pretty-print by LSSerializer and transformer correctly,
     * doesn't compare with the source because the test data is Node object
     *
     */
    @Test(dataProvider = "xml-node-data")
    public void testXMLNodePrettyPrint(Node xml, String expected) throws Exception {
        assertEquals(serializerWrite(xml, true), expected);
        assertEquals(transform(xml, true).replaceAll("\r\n", "\n"), expected);
    }

    /*
     * test block element, inline element, text, and mixed elements.
     */
    @DataProvider(name = "html-data")
    public Object[][] htmlData() throws Exception {
        return new Object[][] {
            { read("htmltest1.xml"), read("htmltest1.out") },
            { read("htmltest2.xml"), read("htmltest2.out") },
            { read("htmltest3.xml"), read("htmltest3.out") },
            { read("htmltest4.xml"), read("htmltest4.out") },
            { read("htmltest5.xml"), read("htmltest5.out") },
            { read("htmltest6.xml"), read("htmltest6.out") } };
    }

    /*
     * @bug 8087303
     * Transform to HTML, test Pretty Print for HTML.
     *
     */
    @Test(dataProvider = "html-data")
    public void testTransformToHTML(String source, String expected) throws Exception {
        // test it's no change if no pretty-print
        StringWriter writer = new StringWriter();
        getTransformer(true, false).transform(new StreamSource(new StringReader(source)), new StreamResult(writer));
        assertTrue(toXmlDocument(source).isEqualNode(toXmlDocument(writer.toString())), "The actual is: " + writer.toString());

        // test pretty-print
        writer = new StringWriter();
        getTransformer(true, true).transform(new StreamSource(new StringReader(source)), new StreamResult(writer));
        assertEquals(writer.toString().replaceAll("\r\n", "\n"), expected);
    }

    @Test
    public void testLSSerializerFormatPrettyPrint() {

        final String XML_DOCUMENT = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n"
                + "<hello>before child element<child><children/><children/></child>after child element</hello>";
        /**JDK-8035467
         * no newline in default output
         */
        final String XML_DOCUMENT_DEFAULT_PRINT =
                "<?xml version=\"1.0\" encoding=\"UTF-16\"?>"
                + "<hello>"
                + "before child element"
                + "<child><children/><children/></child>"
                + "after child element</hello>";

        final String XML_DOCUMENT_PRETTY_PRINT = "<?xml version=\"1.0\" encoding=\"UTF-16\"?><hello>\n" +
                "    before child element\n" +
                "    <child>\n" +
                "        <children/>\n" +
                "        <children/>\n" +
                "    </child>\n" +
                "    after child element\n" +
                "</hello>\n";

        // it all begins with a Document
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException parserConfigurationException) {
            parserConfigurationException.printStackTrace();
            Assert.fail(parserConfigurationException.toString());
        }
        Document document = null;

        StringReader stringReader = new StringReader(XML_DOCUMENT);
        InputSource inputSource = new InputSource(stringReader);
        try {
            document = documentBuilder.parse(inputSource);
        } catch (SAXException saxException) {
            saxException.printStackTrace();
            Assert.fail(saxException.toString());
        } catch (IOException ioException) {
            ioException.printStackTrace();
            Assert.fail(ioException.toString());
        }

        // query DOM Interfaces to get to a LSSerializer
        DOMImplementation domImplementation = documentBuilder.getDOMImplementation();
        DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementation;
        LSSerializer lsSerializer = domImplementationLS.createLSSerializer();

        System.out.println("Serializer is: " + lsSerializer.getClass().getName() + " " + lsSerializer);

        // get configuration
        DOMConfiguration domConfiguration = lsSerializer.getDomConfig();

        // query current configuration
        Boolean defaultFormatPrettyPrint = (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT);
        Boolean canSetFormatPrettyPrintFalse = (Boolean) domConfiguration.canSetParameter(DOM_FORMAT_PRETTY_PRINT, Boolean.FALSE);
        Boolean canSetFormatPrettyPrintTrue = (Boolean) domConfiguration.canSetParameter(DOM_FORMAT_PRETTY_PRINT, Boolean.TRUE);

        System.out.println(DOM_FORMAT_PRETTY_PRINT + " default/can set false/can set true = " + defaultFormatPrettyPrint + "/"
                + canSetFormatPrettyPrintFalse + "/" + canSetFormatPrettyPrintTrue);

        // test values
        assertEquals(defaultFormatPrettyPrint, Boolean.FALSE, "Default value of " + DOM_FORMAT_PRETTY_PRINT + " should be " + Boolean.FALSE);

        assertEquals(canSetFormatPrettyPrintFalse, Boolean.TRUE, "Can set " + DOM_FORMAT_PRETTY_PRINT + " to " + Boolean.FALSE + " should be "
                + Boolean.TRUE);

        assertEquals(canSetFormatPrettyPrintTrue, Boolean.TRUE, "Can set " + DOM_FORMAT_PRETTY_PRINT + " to " + Boolean.TRUE + " should be "
                + Boolean.TRUE);

        // get default serialization
        String prettyPrintDefault = lsSerializer.writeToString(document);
        System.out.println("(default) " + DOM_FORMAT_PRETTY_PRINT + "==" + (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT)
                + ": \n\"" + prettyPrintDefault + "\"");

        assertEquals(prettyPrintDefault, XML_DOCUMENT_DEFAULT_PRINT, "Invalid serialization with default value, " + DOM_FORMAT_PRETTY_PRINT + "=="
                + (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT));

        // configure LSSerializer to not format-pretty-print
        domConfiguration.setParameter(DOM_FORMAT_PRETTY_PRINT, Boolean.FALSE);
        String prettyPrintFalse = lsSerializer.writeToString(document);
        System.out.println("(FALSE) " + DOM_FORMAT_PRETTY_PRINT + "==" + (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT)
                + ": \n\"" + prettyPrintFalse + "\"");

        assertEquals(prettyPrintFalse, XML_DOCUMENT_DEFAULT_PRINT, "Invalid serialization with FALSE value, " + DOM_FORMAT_PRETTY_PRINT + "=="
                + (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT));

        // configure LSSerializer to format-pretty-print
        domConfiguration.setParameter(DOM_FORMAT_PRETTY_PRINT, Boolean.TRUE);
        String prettyPrintTrue = lsSerializer.writeToString(document);
        System.out.println("(TRUE) " + DOM_FORMAT_PRETTY_PRINT + "==" + (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT)
                + ": \n\"" + prettyPrintTrue + "\"");

        assertEquals(prettyPrintTrue, XML_DOCUMENT_PRETTY_PRINT, "Invalid serialization with TRUE value, " + DOM_FORMAT_PRETTY_PRINT + "=="
                + (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT));
    }

    private String serializerWrite(Node xml, boolean pretty) throws Exception {
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        DOMImplementationLS domImplementation = (DOMImplementationLS) registry.getDOMImplementation("LS");
        StringWriter writer = new StringWriter();
        LSOutput formattedOutput = domImplementation.createLSOutput();
        formattedOutput.setCharacterStream(writer);
        LSSerializer domSerializer = domImplementation.createLSSerializer();
        domSerializer.getDomConfig().setParameter(DOM_FORMAT_PRETTY_PRINT, pretty);
        domSerializer.getDomConfig().setParameter("xml-declaration", false);
        domSerializer.write(xml, formattedOutput);
        return writer.toString();
    }

    private String transform(Node xml, boolean pretty) throws Exception {
        Transformer transformer = getTransformer(false, pretty);
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(xml), new StreamResult(writer));
        return writer.toString();
    }

    private Document toXmlDocument(String xmlString) throws Exception {
        InputSource xmlInputSource = new InputSource(new StringReader(xmlString));
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(true);
        DocumentBuilder xmlDocumentBuilder = dbf.newDocumentBuilder();
        Document node = xmlDocumentBuilder.parse(xmlInputSource);
        return node;
    }

    private Text newTextNode(String text) throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return db.newDocument().createTextNode(text);
    }

    private Document createDocWithSequentTextNodes() throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = db.newDocument();
        Node root = doc.createElement("root");
        doc.appendChild(root);
        root.appendChild(doc.createTextNode(" "));
        root.appendChild(doc.createTextNode("t"));
        root.appendChild(doc.createTextNode("\n"));
        root.appendChild(doc.createTextNode("t"));
        root.appendChild(doc.createTextNode("   "));
        Node child1 = doc.createElement("child1");
        root.appendChild(child1);
        child1.appendChild(doc.createTextNode(" "));
        child1.appendChild(doc.createTextNode("\n"));
        root.appendChild(doc.createTextNode("t"));
        Node child2 = doc.createElement("child2");
        root.appendChild(child2);
        child2.appendChild(doc.createTextNode(" "));
        root.appendChild(doc.createTextNode(" "));
        Node child3 = doc.createElement("child3");
        root.appendChild(child3);
        child3.appendChild(doc.createTextNode(" "));
        root.appendChild(doc.createTextNode(" "));
        Node child4 = doc.createElement("child4");
        root.appendChild(child4);
        child4.appendChild(doc.createTextNode(" "));

        root.appendChild(doc.createTextNode(" "));
        Node child5 = doc.createElement("child5");
        root.appendChild(child5);
        child5.appendChild(doc.createTextNode("t"));

        Node child51 = doc.createElement("child51");
        child5.appendChild(child51);
        child51.appendChild(doc.createTextNode(" "));
        Node child511 = doc.createElement("child511");
        child51.appendChild(child511);
        child511.appendChild(doc.createTextNode("t"));
        child51.appendChild(doc.createTextNode(" "));
        child5.appendChild(doc.createTextNode("t"));

        root.appendChild(doc.createTextNode(" "));
        root.appendChild(doc.createComment(" test comment "));
        root.appendChild(doc.createTextNode(" \n"));
        root.appendChild(doc.createComment(" "));
        root.appendChild(doc.createTextNode("\n"));
        root.appendChild(doc.createProcessingInstruction("target1", "test"));
        root.appendChild(doc.createTextNode(" "));
        root.appendChild(doc.createTextNode(" "));
        return doc;
    }

    private Transformer getTransformer(boolean html, boolean pretty) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        if (html)
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
        transformer.setOutputProperty(OutputKeys.INDENT, pretty ? "yes" : "no");
        return transformer;
    }


    private String read(String filename) throws Exception {
        try (InputStream in = PrettyPrintTest.class.getResourceAsStream(filename)) {
            return new String(in.readAllBytes());
        }
    }

    private static final String DOM_FORMAT_PRETTY_PRINT = "format-pretty-print";
}
