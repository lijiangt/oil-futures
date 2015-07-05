package com.blogspot.lijiangt.oil92;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import com.opencsv.CSVReader;

public class ExtractCsv {

	public ExtractCsv() {
	}

	private static final Pattern FILE_NAME_PATTERN = Pattern
			.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d_money\\.csv");

	public static void main(String[] args) {
		File file = new File("/media/win_d/Downloads/moneyfiles");
		File[] listFiles = file.listFiles();
		Map<String, Map.Entry<String, String>> map = new TreeMap<String, Map.Entry<String, String>>();
		for (File f : listFiles) {
			if (f.isFile() && FILE_NAME_PATTERN.matcher(f.getName()).matches()) {
				String filename = f.getName();
				String date = StringUtils.replace(
						filename.substring(0, filename.length() - 10), "-", "");
				map.put(date, getValue(f));
			}
		}
		System.out.println("Date,Futures Initial Margin,Liquidating Value");
		for (Map.Entry<String, Map.Entry<String, String>> entry : map
				.entrySet()) {
			System.out.println(entry.getKey()
					+ ','
					+ (entry.getValue().getKey() == null ? "" : entry
							.getValue().getKey())
					+ ','
					+ (entry.getValue().getValue() == null ? "" : entry
							.getValue().getValue()));
		}
	}

	private static Entry<String, String> getValue(File f) {
		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(f));
			String[] nextLine;
			int i = 1;
			String fv = null, lv = null;
			LOOP: while ((nextLine = reader.readNext()) != null) {
				switch (i) {
				case 1:
					if (!"Liquidating Value".equals(nextLine[14])) {
						throw new RuntimeException("wrong format");
					}
					if (!"Futures Initial Margin".equals(nextLine[18])) {
						throw new RuntimeException("wrong format");
					}
					break;
				case 6:
					fv = nextLine[18];
					lv = nextLine[14];
					if (fv == null || lv == null) {
						throw new RuntimeException("wrong format");
					}
					break;
				case 7:
				case 8:
					if (!fv.equals(nextLine[18])) {
						throw new RuntimeException("wrong format");
					}
					if (!lv.equals(nextLine[14])) {
						throw new RuntimeException("wrong format");
					}
					break;
				case 9:
					break LOOP;
				}
				i++;
			}
			return new ImmutablePair<String, String>(StringUtils.replace(fv, ",", ""), StringUtils.replace(lv, ",", "")); 
		} catch (IOException e) {
			throw new RuntimeException(e);
		}finally{
			if(reader!=null){
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}

	}

}
