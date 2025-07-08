package mataf.snifit.tasks;

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

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		String elementName = getElementName(localName, qName);
		if (elementName.equals("psf")) {
			this.map = new HashMap<String, List<String>>();
			this.inPsf = true;
		} else if (elementName.equals("provider")) {
			if (!this.inPsf)
				throw new SAXException("Element provider must be contained in element psf.");
			this.inProvider = true;
			this.id = atts.getValue("id");
			this.references = new ArrayList<String>();
		} else if (elementName.equals("project")) {
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
		} else if (elementName.equals("provider")) {
			this.map.put(this.id, this.references);
			this.references = null;
			this.inProvider = false;
		} else if (elementName.equals("project")) {
			this.inProject = false;
		}
	}

	public Map<String, List<String>> getReferences() {
		return this.map;
	}

	private String getElementName(String localName, String qName) {
		if (localName != null && localName.length() > 0)
			return localName;
		return qName;
	}
}
