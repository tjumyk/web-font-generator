package org.tjumyk.webfontgen;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.xml.sax.SAXException;

public class Main {
	public static final String TITLE = "Web Font Generator";
	public static final String VERSION = "1.2.0";
	public static final String AUTHOR = "[https://github.com/tjumyk]";

	private static final String DEFAULT_SRC_DIR = ".";
	private static final String DEFAULT_OUTPUT_DIR = "output";
	private static final String DEFAULT_CSS_NAME = "webfonts.css";
	private static final String DEFAULT_INCLUDE_FILES = "*.*";
	private static final String DEFAULT_JAR_NAME = "webfontgen.jar";

	private static Options options = null;

	public static void main(String[] args) throws IOException, SAXException {
		options = buildOptions();
		CommandLineParser parser = new DefaultParser();
		CommandLine cl = null;
		try {
			cl = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println(e.getLocalizedMessage() + "\nUse \"java -jar "
					+ DEFAULT_JAR_NAME + " -?\" for help info.");
			System.exit(1);
		}

		Worker worker = new Worker();
		String[] files = new String[] { DEFAULT_INCLUDE_FILES }, excludeDirs = new String[] { "" };
		String encode = "utf-8";
		File[] srcFontFile = null;
		File srcDir = new File(DEFAULT_SRC_DIR), distFontDir = new File(
				DEFAULT_OUTPUT_DIR), cssFile = new File(DEFAULT_OUTPUT_DIR
				+ File.separator + DEFAULT_CSS_NAME);
		boolean needHints = false, needMicrotypeExpress = false, needDeepMatch = false;

		if (cl.hasOption("?")) {
			printHelp();
			System.exit(0);
		}
		if (cl.hasOption("v")) {
			System.out.println(VERSION);
			System.exit(0);
		}
		if (cl.hasOption("s")) {
			srcDir = new File(cl.getOptionValue("s"));
		}
		if (cl.hasOption("o")) {
			distFontDir = new File(cl.getOptionValue("o"));
		}
		if (cl.hasOption("i")) {
			files = cl.getOptionValues("i");
		}
		if (cl.hasOption("x")) {
			excludeDirs = cl.getOptionValues("x");
		}
		if (cl.hasOption("e")) {
			encode = cl.getOptionValue("e");
		}
		if (cl.hasOption("c")) {
			cssFile = new File(cl.getOptionValue("c"));
		}
		if (cl.hasOption("h")) {
			needHints = true;
		}
		if (cl.hasOption("m")) {
			needMicrotypeExpress = true;
		}
		if (cl.hasOption("d")) {
			needDeepMatch = true;
		}
		List<File> fontFileList = new LinkedList<File>();
		for (String arg : cl.getArgs()) {
			File f = new File(arg);
			if (f.exists())
				fontFileList.add(f);
		}

		srcFontFile = fontFileList.toArray(new File[] {});

		if (files != null && files.length > 0 && srcFontFile != null
				&& srcFontFile.length > 0 && distFontDir != null) {
			worker.work(srcDir, srcFontFile, distFontDir, files, excludeDirs,
					encode, cssFile, needHints, needMicrotypeExpress,
					needDeepMatch);
			System.out.println("All done.");
		} else {
			printHelp();
		}
	}

	private static void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		String header = TITLE
				+ " by "
				+ AUTHOR
				+ " "
				+ ("(Version: " + VERSION + ").\n")
				+ "It use the given TTF font(s) to generate minimalized "
				+ "EOT/WOFF/TTF/SVG font-packs and cross-browser compatible @font-face CSS "
				+ "depending on the contents scanned from the files you specify.\n"
				+ "Its goal is to make embedding CJK(Chinese/Japanese/Korean) fonts in webpages "
				+ "easier dedicatedly for small-scale and static websites "
				+ "(just like github pages).";
		String footer = "Any bugs or suggestions, please put them "
				+ "on the github issue page.";
		formatter.printHelp("java -jar " + DEFAULT_JAR_NAME + " TTF1 TTF2...",
				header, options, footer, true);
	}

	private static Options buildOptions() {
		Options options = new Options();

		//@formatter:off
		options.addOption(Option.builder("?")
				                .desc("Show this help info.")
				                .longOpt("help")
				                .build());
		options.addOption(Option.builder("v")
                				.desc("Show version info.")
                				.longOpt("version")
                				.build());
		options.addOption(Option.builder("s")
				                .argName("DIR")
							    .hasArg()
							    .desc("Root dir of your website dir or workspace dir. Defaults to \""+DEFAULT_SRC_DIR+"\".")
							    .longOpt("src")
							    .build());
		options.addOption(Option.builder("o")
							 	.argName("DIR")
								.hasArg()
								.desc("Dir to store the generated fonts. Defaults to \""+DEFAULT_OUTPUT_DIR+"\".")
								.longOpt("out")
								.build());
		options.addOption(Option.builder("i")
								.argName("FILE1,FILE2,...")
								.hasArgs()
								.valueSeparator(',')
								.desc("Specify files to include for scanning (wildcard available). Defaults to \""+DEFAULT_INCLUDE_FILES+"\".")
								.longOpt("include")
								.build());
		options.addOption(Option.builder("x")
							    .argName("DIR1,DIR2,...")
							    .hasArgs()
							    .valueSeparator(',')
							    .desc("Specify which dirs will be skipped (wildcard available).")
							    .longOpt("exclude")
							    .build());
		options.addOption(Option.builder("e")
								.argName("CHARSET")
								.hasArg()
								.desc("Specify the encoding charset for parsing files. Defaults to \"UTF-8\".")
								.longOpt("encode")
								.build());
		options.addOption(Option.builder("c")
				 				.argName("FILE")
				 				.hasArg()
				 				.desc("Specify the filename(and location) of the generated CSS file. Defaults to \""+DEFAULT_OUTPUT_DIR
				 						+ File.separator + DEFAULT_CSS_NAME+"\".")
				 				.longOpt("css")
				 				.build());
		options.addOption(Option.builder("h")
								.desc("Keep hint info of the generated WOFF/EOT/TTF font.")
								.longOpt("hint")
								.build());
		options.addOption(Option.builder("m")
								.desc("Enable Microtype Express Compression for the generated EOT font.")
								.longOpt("mec")
								.build());
		options.addOption(Option.builder("d")
								.desc("(Experimental)Enable deep scanning to get precise font-text match so as to make fonts much smaller.\n"
									 +"[Notice] If enabled, ensure all related css files could be accessible(e.g. css from servers linked by absolute path), and you'd better add \"@charset\" declaration in your css.")
								.longOpt("deep")
								.build());
		//@formatter:on
		return options;
	}
}
