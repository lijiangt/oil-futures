package com.blogspot.lijiangt.oil92;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Hello world!
 *
 */
public class TidyExcel {

	// private final static String MONTH_PATTERN_CHINESE = "MM月-yy";

	private static Map.Entry<Integer, Map<Integer, Double>> getDataRow(
			File source) throws ParseException {
		String filename = source.getName()
				.substring(0, source.getName().lastIndexOf('.')).trim();
		Date date = new Date(new SimpleDateFormat("dd-MM-yyyy").parse(filename)
				.getTime());
		int rowIndex = getRowIndex(date);
		InputStream is = null;
		try {
			is = new FileInputStream(source);
			Workbook workBook = new HSSFWorkbook(is);
			Sheet sheet = getSheet(workBook, filename);
			Map<Integer, Double> rowData = new HashMap<Integer, Double>(40);
			for (int i = 4;; i++) {
				Row row = sheet.getRow(i);
				Cell cr = row.getCell(17);
				double value = cr.getNumericCellValue();// R列
				if (value == 0) {
					break;
				}
				// System.out.println(value);
				Cell cb = row.getCell(1);
				Calendar c = Calendar.getInstance();
				c.setTime(cb.getDateCellValue());
				int month = c.get(Calendar.MONTH) + 1;
				int year = c.get(Calendar.YEAR);
				int columnIndex = getColumnIndex(year, month);
				rowData.put(columnIndex, value);
				// System.out.print(rowIndex);
				// System.out.print(' ');
				// System.out.println(columnIndex);
			}
			return new SimpleEntry<Integer, Map<Integer, Double>>(rowIndex,
					rowData);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private static Sheet getSheet(Workbook workBook, String name) {
		for (int i = 0; i < workBook.getNumberOfSheets(); i++) {
			if (name.equals(workBook.getSheetName(i).trim())) {
				return workBook.getSheetAt(i);
			}
		}
		return null;
	}

	private static final long FIRST_MILLI;
	static {
		try {
			FIRST_MILLI = new Date(new SimpleDateFormat("dd-MM-yyyy").parse(
					"01-01-2013").getTime()).getTime();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	private static final long DAY_MILLISECONDS = 24 * 3600000;

	private static int getRowIndex(Date date) {
		return (int) ((date.getTime() - FIRST_MILLI) / DAY_MILLISECONDS) + 1;
	}

	private static int getColumnIndex(int year, int month) {
		return (year - 2013) * 12 + month;
	}

	private static final Pattern FILE_NAME_PATTERN = Pattern
			.compile("\\d\\d-\\d\\d-20\\d\\d\\.xls");

	private static void writeData(File file,
			Map<Integer, Map<Integer, Double>> data) {
		InputStream is = null;
		Workbook workBook = null;
		FileOutputStream fos = null;
		try {
			is = new FileInputStream(file);
			workBook = new HSSFWorkbook(is);
			Sheet sheet = getSheet(workBook, "sum");
//			sheet.getPhysicalNumberOfRows();
			for (Map.Entry<Integer, Map<Integer, Double>> entry : data
					.entrySet()) {
				Row row = sheet.getRow(entry.getKey());
				for (Map.Entry<Integer, Double> en : entry.getValue()
						.entrySet()) {
					Cell c = row.getCell(en.getKey());
					if (c == null) {
						c = row.createCell((short) en.getKey().intValue());
					}
					c.setCellValue(en.getValue());
				}
			}
			fos = new FileOutputStream(new File(file.getParentFile(),
					String.valueOf(System.currentTimeMillis()) + '-'
							+ file.getName()));
			workBook.write(fos);
			fos.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	public static void main(String[] args) throws ParseException {
		// System.out.println(new SimpleDateFormat(MONTH_PATTERN_CHINESE).parse(
		// "1月-16"));
		// System.out.println(new SimpleDateFormat(MONTH_PATTERN_CHINESE).parse(
		// "11月-16"));
		// System.out.println(getRowIndex(new Date(new
		// SimpleDateFormat("dd-MM-yyyy").parse("25-04-2014")
		// .getTime())));
		// System.out.println(getRowIndex(new Date(new
		// SimpleDateFormat("dd-MM-yyyy").parse("01-01-2013")
		// .getTime())));
		// System.out.println(getRowIndex(new Date(new
		// SimpleDateFormat("dd-MM-yyyy").parse("03-01-2013")
		// .getTime())));
		// System.out.println(getColumnIndex(2013,2));
		// System.out.println(getColumnIndex(2013,6));
		if (args.length != 2) {
			System.err.println("参数格式： 目标文件 待分析excel文件夹");
			System.exit(-1);
		}
		File file = new File(args[0]);
		if (!file.exists() || !file.isFile()) {
			System.err.println("目标文件错误");
			System.exit(-1);
		}
		File dir = new File(args[1]);
		if (!dir.exists()) {
			System.err.println("待分析excel文件夹不存在!");
			System.exit(-1);
		}
		if (!dir.isDirectory()) {
			System.err.println("待分析excel路径不是文件夹!");
			System.exit(-1);
		}
		File[] listFiles = dir.listFiles();
		Map<Integer, Map<Integer, Double>> data = new HashMap<Integer, Map<Integer, Double>>(
				1024);
		Map.Entry<Integer, Map<Integer, Double>> entry;
		for (File f : listFiles) {
			if (f.isFile() && FILE_NAME_PATTERN.matcher(f.getName()).matches()) {
//				System.out.println("读取Excel开始: " + f.getName());
				try {
					entry = getDataRow(f);
					if (data.containsKey(entry.getKey())) {
						System.err.println("数据重复，文件名: " + f.getName());
						System.exit(-1);
					} else {
						data.put(entry.getKey(), entry.getValue());
					}
				} catch (Throwable e) {
					System.err.println("文件数据错误: " + f.getName());
				}
//				System.out.println("读取Excel结束: " + f.getName());
			}
		}
//		System.out.println("写入数据到Excel文件开始");
		writeData(file, data);
//		System.out.println("写入数据到Excel文件结束" );
	}

}
