package org.tjumyk.webfontgen;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.xml.sax.SAXException;

import com.google.typography.font.tools.sfnttool.SfntTool;

public class Worker {

	Set<Character> charUsed = new HashSet<Character>();
	Map<String, HashSet<Character>> charsUsedMap = new HashMap<String, HashSet<Character>>();
	CSSFontMatcher matcher = null;

	public void work(File cd, File[] srcFontFiles, File distFontDir,
			String[] files, String[] excludeDirs, String encode, File cssFile,
			boolean needHints, boolean needMicrotypeExpress,
			boolean enableCSSMatch) throws IOException, SAXException {
		URL cdURL = cd.toURI().toURL();
		String[] fontNames = new String[srcFontFiles.length];
		for (int i = 0; i < srcFontFiles.length; i++) {
			fontNames[i] = FilenameUtils.getBaseName(srcFontFiles[i].getName());
		}
		System.out.println("Target fonts: " + Arrays.toString(fontNames));

		charUsed.clear();
		charsUsedMap.clear();
		for (File file : FileUtils.listFiles(cd, new WildcardFileFilter(files,
				IOCase.INSENSITIVE), FileFilterUtils
				.notFileFilter(new WildcardFileFilter(excludeDirs,
						IOCase.INSENSITIVE)))) {
			System.out.println("Scanning File: " + file);
			if (!enableCSSMatch) {
				String str = FileUtils.readFileToString(file, encode);
				for (int i = 0, j = str.length(); i < j; i++) {
					charUsed.add(str.charAt(i));
				}
			} else {
				if (matcher == null)
					matcher = new CSSFontMatcher();
				URL fileUrl = file.toURI().toURL();
				String ext = FilenameUtils.getExtension(file.getName());
				if (CSSFontMatcher.isExtSupported(ext)) {
					matcher.analyzeFonts(cdURL, fileUrl, encode, charsUsedMap,
							fontNames);
				} else {
					System.out
							.println("[Warning] Unsupported file extension for \"-d\" mode: \""
									+ file + "\", scan skipped.");
				}
			}
		}

		if (!distFontDir.exists())
			distFontDir.mkdirs();
		String distPath = distFontDir.getAbsolutePath();

		String combined = null;
		Map<String, String> combinedMap = null;
		if (!enableCSSMatch) {
			int charUsedCount = charUsed.size();
			if (charUsedCount <= 0) {
				System.err.println("No characters found!");
				System.exit(1);
			}
			System.out.println("Characters Count: " + charUsedCount);
			StringBuffer buf = new StringBuffer(charUsedCount);
			Iterator<Character> iterator = charUsed.iterator();
			while (iterator.hasNext()) {
				buf.append(iterator.next());
			}
			combined = buf.toString();
		} else {
			combinedMap = new HashMap<String, String>();
			Set<String> keySet = charsUsedMap.keySet();
			if (keySet.size() <= 0) {
				System.err.println("No font-char match found!");
				System.exit(1);
			}
			System.out.println("Characters Count:");
			for (String font : keySet) {
				HashSet<Character> charUsed = charsUsedMap.get(font);
				int charUsedCount = charUsed.size();
				if (charUsedCount <= 0)
					continue;
				System.out.println(font + ":" + charUsedCount);
				StringBuffer buf = new StringBuffer(charUsedCount);
				Iterator<Character> iterator = charUsed.iterator();
				while (iterator.hasNext()) {
					buf.append(iterator.next());
				}
				combinedMap.put(font, buf.toString());
			}
		}

		File cssDir = cssFile.getParentFile();
		if (!cssDir.exists())
			cssDir.mkdirs();
		Path cssDirPath = Paths.get(cssDir.getAbsolutePath());
		Path fontDirPath = Paths.get(distPath);
		String relativePath = cssDirPath.relativize(fontDirPath).toString();
		if (relativePath.length() > 0)
			relativePath = relativePath + File.separator;
		relativePath = relativePath.replaceAll("\\\\", "/");

		StringBuffer css = new StringBuffer();
		css.append("@charset \'" + encode + "\';\n\n");
		css.append("/*!\n"
				+ "// ===========================================================\n"
				+ ("// " + Main.TITLE + " " + Main.VERSION + "\n")
				+ ("// By " + Main.AUTHOR + "\n")
				+ "// ===========================================================\n"
				+ "*/\n\n");

		for (int i = 0; i < srcFontFiles.length; i++) {
			File srcFontFile = srcFontFiles[i];
			String fontName = fontNames[i];
			String urlFriendlyFontName = getURLFriendlyFontName(fontName);

			String srcFontPath = srcFontFile.getAbsolutePath();
			String text = null;
			if (!enableCSSMatch)
				text = combined;
			else
				text = combinedMap.get(fontName);
			if (text == null || text.length() <= 0) {
				System.out.println("[Warning] No characters related to font["
						+ fontName + "], output for this font is canceled.");
				continue;
			}
			createFont(srcFontPath, urlFriendlyFontName, distPath, text,
					needHints, needMicrotypeExpress);
			//@formatter:off
			css.append("@font-face {\n" + //
			"    font-family: \'" + fontName + "\';\n" + 
			"    src: url(\'" + relativePath + urlFriendlyFontName + ".eot\'); /* IE9 Compat Modes */\n" + 
			"    src: url(\'" + relativePath + urlFriendlyFontName + ".eot?#iefix\') format(\'embedded-opentype\'),  /* IE6-IE8 */\n" + 
			"         url(\'" + relativePath + urlFriendlyFontName + ".woff\') format(\'woff\'), /* Modern Browsers */\n" + 
			"         url(\'" + relativePath + urlFriendlyFontName + ".ttf\') format(\'truetype\'), /* Safari, Android, iOS */\n" + 
			"         url(\'" + relativePath + urlFriendlyFontName + ".svg#"+urlFriendlyFontName+"\') format(\'svg\'); /* Legacy iOS */\n" +
			"    font-weight: normal;\n"+
			"    font-style: normal;\n"+
			"}\n\n");
			//@formatter:on	
		}

		System.out.print("Generating: " + cssFile.getAbsolutePath() + "...");
		FileUtils.write(cssFile, css, encode);
		System.out.printf("%1$,.1fKB\n", FileUtils.sizeOf(cssFile) / 1024.0);
	}

	private String getURLFriendlyFontName(String name) {
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (ch >= 128) { // non-ascii
				String str = Integer.toString(name.hashCode());
				System.out.println("[Warning] Font name \"" + name
						+ "\" is not url-friendly, generated font files"
						+ " will have a different name \"" + str + "\".");
				return str;
			}
		}
		return name;
	}

	private void createFont(String srcFontPath, String fontName,
			String distPath, String text, boolean needHints,
			boolean needMicrotypeExpress) throws IOException {
		//@formatter:off
		String distWOFFPath = distPath + File.separator + fontName + ".woff";
		System.out.print("Generating: " + distWOFFPath + "...");
		if(needHints)
			SfntTool.main(new String[] { "-s", text, "-w", srcFontPath,distWOFFPath});
		else
			SfntTool.main(new String[] { "-s", text, "-w", "-h", srcFontPath,distWOFFPath});
		System.out.printf("%1$,.1fKB\n", FileUtils.sizeOf(new File(distWOFFPath))/1024.0);
		
		String distEOTPath = distPath + File.separator + fontName + ".eot";
		System.out.print("Generating: " + distEOTPath + "...");
		if(needHints)
			if(needMicrotypeExpress)
				SfntTool.main(new String[] { "-s", text, "-e", "-x", srcFontPath, distEOTPath});
			else
				SfntTool.main(new String[] { "-s", text, "-e", srcFontPath, distEOTPath});
		else
			if(needMicrotypeExpress)
				SfntTool.main(new String[] { "-s", text, "-e", "-x", "-h", srcFontPath, distEOTPath});
			else
				SfntTool.main(new String[] { "-s", text, "-e", "-h", srcFontPath, distEOTPath});
		System.out.printf("%1$,.1fKB\n", FileUtils.sizeOf(new File(distEOTPath))/1024.0);
		
		String distTTFPath = distPath + File.separator + fontName + ".ttf";
		System.out.print("Generating: " + distTTFPath + "...");
		if(needHints)
			SfntTool.main(new String[] { "-s", text, srcFontPath, distTTFPath});
		else
			SfntTool.main(new String[] { "-s", text, "-h", srcFontPath, distTTFPath});
		System.out.printf("%1$,.1fKB\n", FileUtils.sizeOf(new File(distTTFPath))/1024.0);
		
		String distSVGPath = distPath + File.separator + fontName + ".svg";
		System.out.print("Generating: " + distSVGPath + "...");
		SVGFont.main(new String[]{srcFontPath, "-s", text, "-id", fontName, "-o", distSVGPath});
		System.out.printf("%1$,.1fKB\n", FileUtils.sizeOf(new File(distSVGPath))/1024.0);
		//@formatter:on
	}
}
