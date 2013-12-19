package org.tjumyk.webfontgen;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fit.cssbox.io.DefaultDocumentSource;
import org.fit.cssbox.io.DocumentSource;
import org.tjumyk.webfontgen.dom.DOMFontAnalyzer;
import org.tjumyk.webfontgen.dom.DOMSource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class CSSFontMatcher {

	public static final String[] SUPPORTED_DOC_EXT = {"htm","html"};
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.err.println("Usage: CSSFontMatcher url charset fontname");
			System.exit(0);
		}
		CSSFontMatcher instance = new CSSFontMatcher();

		try {
			Map<String, HashSet<Character>> fontCharMap = new HashMap<String, HashSet<Character>>();
			instance.analyzeFonts(null, new URL(args[0]), args[1], fontCharMap,
					args[2]);
			for (String font : fontCharMap.keySet()) {
				System.out.println("[" + font + "]");
				Set<Character> chars = fontCharMap.get(font);
				Character[] charArr = chars.toArray(new Character[] {});
				Arrays.sort(charArr);
				for (Character ch : charArr) {
					System.out.print(ch);
				}
				System.out.println();
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static boolean isExtSupported(String ext){
		for(String e : SUPPORTED_DOC_EXT){
			if(e.equalsIgnoreCase(ext)){
				return true;
			}
		}
		return false;
	}

	public void analyzeFonts(URL base, URL url, String encoding,
			Map<String, HashSet<Character>> fontCharMap, String... fontNames)
			throws IOException, SAXException {
		DocumentSource docSource = new DefaultDocumentSource(base,
				url.toExternalForm());

		DOMSource parser = new DOMSource(docSource, encoding);
		Document doc = parser.parse();

		DOMFontAnalyzer da = new DOMFontAnalyzer(doc, base);
		da.setDefaultEncoding(encoding);
		da.attributesToStyles();
		da.getStyleSheets();

		da.setTargetFonts(fontNames);
		da.analyzeFontUsage(fontCharMap);
		
		docSource.close();
	}
}
