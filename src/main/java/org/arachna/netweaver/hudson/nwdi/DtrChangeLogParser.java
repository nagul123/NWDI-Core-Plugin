/**
 *
 */
package org.arachna.netweaver.hudson.nwdi;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A parser for a DTR change log persisted to XML.
 * 
 * @author G526521
 */
public final class DtrChangeLogParser extends ChangeLogParser {
    @Override
    public ChangeLogSet<? extends Entry> parse(final AbstractBuild build, final File changelogFile) throws IOException,
        SAXException {
        // Do the actual parsing
        final DtrChangeLogSet changeSet = new DtrChangeLogSet(build);
        final InternalDtrChangeLogParser handler = new InternalDtrChangeLogParser(changeSet);
        final FileReader reader = new FileReader(changelogFile);
        final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setContentHandler(handler);
        xmlReader.parse(new InputSource(reader));
        reader.close();

        return changeSet;
    }

    private static final class InternalDtrChangeLogParser extends DefaultHandler {
        private final StringBuilder text = new StringBuilder();
        private final DtrChangeLogSet changeSet;
        private DtrChangeLogEntry currentChangeLogEntry;

        InternalDtrChangeLogParser(final DtrChangeLogSet changeSet) {
            this.changeSet = changeSet;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
         */
        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            text.append(ch, start, length);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
         * java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            if ("changeset".equals(localName)) {
                this.changeSet.add(this.currentChangeLogEntry);
                this.currentChangeLogEntry = null;
            }
            else if ("comment".equals(localName)) {
                this.currentChangeLogEntry.setMsg(this.getText());
            }
            else if ("user".equals(localName)) {
                this.currentChangeLogEntry.setUser(getText());
            }
            else if ("date".equals(localName)) {
                this.currentChangeLogEntry.setCheckInTime(getText());
            }
            else if ("item".equals(localName)) {
                this.currentChangeLogEntry.addAffectedPath(getText());
            }

            this.getText();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
         * java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(final String uri, final String localName, final String qName,
            final Attributes attributes) throws SAXException {
            if ("changeset".equals(localName)) {
                currentChangeLogEntry = new DtrChangeLogEntry();
                currentChangeLogEntry.setVersion(attributes.getValue("version"));
            }
        }

        private String getText() {
            final String t = this.text.toString().trim();
            this.text.setLength(0);

            return t;
        }
    }
}
