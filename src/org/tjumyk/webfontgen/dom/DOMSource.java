package org.tjumyk.webfontgen.dom;

import java.io.IOException;

import org.apache.xerces.parsers.DOMParser;
import org.cyberneko.html.HTMLConfiguration;
import org.fit.cssbox.io.DocumentSource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class DOMSource {
	protected DocumentSource src;
	protected String charset;

	public DOMSource(DocumentSource src) {
		this.src = src;
		setContentType(src.getContentType());
	}

	public DOMSource(DocumentSource src, String charset) {
		this.src = src;
		this.charset = charset;
		setContentType(src.getContentType());
	}

	public DocumentSource getDocumentSource() {
		return src;
	}

	public String getCharset() {
		return charset;
	}

	public void setContentType(String type) {
		if (type != null) {
			String t = type.toLowerCase();

			// extract the charset if specified
			int strt = t.indexOf("charset=");
			if (strt >= 0) {
				strt += "charset=".length();
				int stop = t.indexOf(';', strt);
				if (stop == -1)
					stop = t.length();
				charset = t.substring(strt, stop).trim();
				charset = charset.replaceAll("^\"|\"$|^\'|\'$", "").trim();
			}
		}
	}

	public Document parse() throws SAXException, IOException {
		DOMParser parser = new DOMParser(new HTMLConfiguration());
		parser.setProperty("http://cyberneko.org/html/properties/names/elems",
				"lower");
		if (charset != null)
			parser.setProperty(
					"http://cyberneko.org/html/properties/default-encoding",
					charset);
		parser.parse(new org.xml.sax.InputSource(getDocumentSource()
				.getInputStream()));
		return parser.getDocument();
	}

}
