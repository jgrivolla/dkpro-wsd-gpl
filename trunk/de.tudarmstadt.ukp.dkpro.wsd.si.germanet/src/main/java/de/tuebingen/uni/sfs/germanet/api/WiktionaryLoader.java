/*
 * Copyright (C) 2012 Department of General and Computational Linguistics,
 * University of Tuebingen
 *
 * This file is part of the Java API to GermaNet.
 *
 * The Java API to GermaNet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Java API to GermaNet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this API; if not, see <http://www.gnu.org/licenses/>.
 */
package de.tuebingen.uni.sfs.germanet.api;

import java.io.*;
import java.io.InputStream;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Load <code>WiktionaryParaphrases</code> into a specified <code>GermaNet</code> object.
 *
 * @author University of Tuebingen, Department of Linguistics (germanetinfo at uni-tuebingen.de)
 * @version 8.0
 */
class WiktionaryLoader {

    private GermaNet germaNet;
    private String namespace;
    private File wikiDir;

    /**
     * Constructs a <code>WiktionaryLoader</code> for the specified
     * <code>GermaNet</code> object.
     * @param germaNet the <code>GermaNet</code> object to load the
     * <code>WiktionaryParaphrases</code> into
     */
    protected WiktionaryLoader(GermaNet germaNet) {
        this.germaNet = germaNet;
    }

    /**
     * Loads <code>WiktionaryParaphrases</code> from the specified file into this
     * <code>WiktionaryLoader</code>'s <code>GermaNet</code> object.
     * @param wiktionaryFile the file containing <code>WiktionaryParaphrases</code> data
     * @throws java.io.FileNotFoundException
     * @throws javax.xml.stream.XMLStreamException
     */
    protected void loadWiktionary(File wiktionaryFile) throws FileNotFoundException, XMLStreamException {
        wikiDir = wiktionaryFile;
        FilenameFilter filter = new WikiFilter(); //get only wiktionary files
        File[] wikiFiles = wikiDir.listFiles(filter);


        if (wikiFiles == null || wikiFiles.length == 0) {
            throw new FileNotFoundException("Unable to load Wiktionary Paraphrases from \""
                    + this.wikiDir.getPath() + "\"");
        }

        for (int i = 0; i < wikiFiles.length; i++) {
            System.out.println("Loading "
                    + wikiFiles[i].getName() + "...");
            InputStream in = new FileInputStream(wikiFiles[i]);
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader parser = factory.createXMLStreamReader(in);
            int event;
            String nodeName;


            //Parse entire file, looking for Wiktionary paraphrase start elements
            while (parser.hasNext()) {
                event = parser.next();
                switch (event) {
                    case XMLStreamConstants.START_DOCUMENT:
                        namespace = parser.getNamespaceURI();
                        break;
                    case XMLStreamConstants.START_ELEMENT:
                        nodeName = parser.getLocalName();
                        if (nodeName.equals(GermaNet.XML_WIKTIONARY_PARAPHRASE)) {
                            WiktionaryParaphrase wiki = processWiktionaryParaphrase(parser);
                            germaNet.addWiktionaryParaphrase(wiki);
                        }
                        break;
                }
            }
            parser.close();
        }

        System.out.println("Done.");


    }

    /**
     * Loads <code>WiktionaryParaphrases</code> from the given streams into this
     * <code>WiktionaryLoader</code>'s <code>GermaNet</code> object.
     * @param inputStreams the list of streams containing <code>WiktionaryParaphrases</code> data
     * @param xmlNames the names of the streams
     * @throws javax.xml.stream.XMLStreamException
     */
    protected void loadWiktionary(List<InputStream> inputStreams,
            List<String> xmlNames) throws XMLStreamException {


        for (int i = 0; i < inputStreams.size(); i++) {
            if (xmlNames.get(i).startsWith("wiktionary")) {
                System.out.println("Loading input stream "
                        + xmlNames.get(i) + "...");
                XMLInputFactory factory = XMLInputFactory.newInstance();
                XMLStreamReader parser = factory.createXMLStreamReader(inputStreams.get(i));
                int event;
                String nodeName;


                //Parse entire file, looking for Wiktionary paraphrase start elements
                while (parser.hasNext()) {
                    event = parser.next();
                    switch (event) {
                        case XMLStreamConstants.START_DOCUMENT:
                            namespace = parser.getNamespaceURI();
                            break;
                        case XMLStreamConstants.START_ELEMENT:
                            nodeName = parser.getLocalName();
                            if (nodeName.equals(GermaNet.XML_WIKTIONARY_PARAPHRASE)) {
                                WiktionaryParaphrase wiki = processWiktionaryParaphrase(parser);
                                germaNet.addWiktionaryParaphrase(wiki);
                            }
                            break;
                    }
                }
                parser.close();
            }
        }

        System.out.println("Done.");


    }

    /**
     * Returns the <code>WiktionaryParaphrase</code> for which the start tag was just encountered.
     * @param parser the <code>parser</code> being used on the current file
     * @return a <code>WiktionaryParaphrase</code> representing the data parsed
     * @throws javax.xml.stream.XMLStreamException
     */
    private WiktionaryParaphrase processWiktionaryParaphrase(XMLStreamReader parser) throws XMLStreamException {
        int lexUnitId;
        int wiktionaryId;
        int wiktionarySenseId;
        String wiktionarySense;
        boolean edited = false;
        WiktionaryParaphrase curWiki;

        lexUnitId = Integer.valueOf(parser.getAttributeValue(namespace, GermaNet.XML_LEX_UNIT_ID).substring(1));
        wiktionaryId = Integer.valueOf(parser.getAttributeValue(namespace, GermaNet.XML_WIKTIONARY_ID).substring(1));
        wiktionarySenseId = Integer.valueOf(parser.getAttributeValue(namespace, GermaNet.XML_WIKTIONARY_SENSE_ID));
        wiktionarySense = parser.getAttributeValue(namespace, GermaNet.XML_WIKTIONARY_SENSE);

        String edit = parser.getAttributeValue(namespace, GermaNet.XML_WIKTIONARY_EDITED);
        if (edit.equals(GermaNet.YES)) {
            edited = true;
        }

        curWiki = new WiktionaryParaphrase(lexUnitId, wiktionaryId, wiktionarySenseId,
                wiktionarySense, edited);

        return curWiki;
    }

    /**
     * Filters out all the files which do not contain <code>WiktionaryParaphrases</code>
     */
    private class WikiFilter implements FilenameFilter {

        @Override
        public boolean accept(File directory, String name) {
            return (name.endsWith("xml")
                    && (name.startsWith("wiktionaryParaphrases")));
        }
    }
}
