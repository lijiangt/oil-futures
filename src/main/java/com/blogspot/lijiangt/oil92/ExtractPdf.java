package com.blogspot.lijiangt.oil92;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

public class ExtractPdf {

	public ExtractPdf() {
	}

	private static String getPdfContent(File file) {
		InputStream input = null;
		PDDocument document = null;
		try {
			input = new FileInputStream(file);
			// 加载 pdf 文档
			PDFParser parser = new PDFParser(input);
			parser.parse();
			document = parser.getPDDocument();
			// 获取内容信息
			PDFTextStripper pts = new PDFTextStripper();
			return pts.getText(document);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (null != input)
				try {
					input.close();
				} catch (IOException e) {
				}
			if (null != document)
				try {
					document.close();
				} catch (IOException e) {
				}
		}
	}

	private final static Pattern IMR_PATTERN = Pattern
			.compile("INITIAL MARGIN REQUIREMENT\\s+([0-9,\\.]+)\\s+");
	private final static Pattern AVM_PATTERN = Pattern
			.compile("ACCOUNT VALUE AT MARKET\\s+([0-9,\\.]+)\\s+");

	private static Map.Entry<String, String> getValue(File file) {
		String content = getPdfContent(file);
		Matcher matcher = IMR_PATTERN.matcher(content);
		String left = null, right = null;
		while (matcher.find()) {
			left = StringUtils.replace(matcher.group(1), ",", "");
		}
		matcher = AVM_PATTERN.matcher(content);
		while (matcher.find()) {
			right = StringUtils.replace(matcher.group(1), ",", "");
		}
		return new ImmutablePair<String, String>(left, right);
	}

	private static final Pattern FILE_NAME_PATTERN = Pattern
			.compile("\\d+ statement\\.pdf");

	public static void main(String[] args) {
		File file = new File("/media/win_d/Downloads/Statement");
		File[] listFiles = file.listFiles();
		Map<String, Map.Entry<String, String>> map = new TreeMap<String, Map.Entry<String, String>>();
		for (File f : listFiles) {
			if (f.isFile() && FILE_NAME_PATTERN.matcher(f.getName()).matches()) {
				String filename = f.getName();
				String date = filename.substring(0, filename.length() - 14);
				map.put(date, getValue(f));
			}
		}
		System.out.println("DATE,INITIAL MARGIN REQUIREMENT,ACCOUNT VALUE AT MARKET");
		for (Map.Entry<String, Map.Entry<String, String>> entry : map
				.entrySet()) {
			System.out.println(entry.getKey() + ','
					+ (entry.getValue().getKey() == null ? "" : entry.getValue()
					.getKey()) + ',' + (entry.getValue().getValue() == null ? ""
					: entry.getValue().getValue()));
		}
	}

}
