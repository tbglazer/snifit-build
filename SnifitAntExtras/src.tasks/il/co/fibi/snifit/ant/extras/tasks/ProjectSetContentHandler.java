package il.co.fibi.snifit.ant.extras.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ProjectSetContentHandler extends DefaultHandler {
	boolean inPsf = false;

	boolean inProvider = false;

	boolean inProject = false;

	Map<String, List<String>> map;

	String id;

	List<String> references;

	boolean isVersionOne = false;

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		String elementName = getElementName(localName, qName);
		if (elementName.equals("psf")) {
			this.map = new HashMap<>();
			this.inPsf = true;
			String version = atts.getValue("version");
			this.isVersionOne = version.equals("1.0");
			return;
		}
		if (this.isVersionOne)
			return;
		if (elementName.equals("provider")) {
			if (!this.inPsf)
				throw new SAXException("Element provider must be contained in element psf.");
			this.inProvider = true;
			this.id = atts.getValue("id");
			this.references = new ArrayList<>();
			return;
		}
		if (elementName.equals("project")) {
			if (!this.inProvider)
				throw new SAXException("Element project must be contained in element provider.");
			this.inProject = true;
			String reference = atts.getValue("reference");
			this.references.add(reference);
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		String elementName = getElementName(localName, qName);
		if (elementName.equals("psf")) {
			this.inPsf = false;
			return;
		}
		if (this.isVersionOne)
			return;
		if (elementName.equals("provider")) {
			this.map.put(this.id, this.references);
			this.references = null;
			this.inProvider = false;
			return;
		}
		if (elementName.equals("project")) {
			this.inProject = false;
		}
	}

	public Map<String, List<String>> getReferences() {
		return this.map;
	}

	public boolean isVersionOne() {
		return this.isVersionOne;
	}

	private String getElementName(String localName, String qName) {
		if (localName != null && !localName.isEmpty())
			return localName;
		return qName;
	}
}
