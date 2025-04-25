package com.openclassrooms.tourguide.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExcelWriter {

	public ExcelWriter() {
		super();
	}

	private static final String FILE_PATH = "TourGuide_Performance_Graphs.xlsx";
	private static final String LOCATION_SHEET = "Locations";
	private static final String REWARD_SHEET = "Rewards";

	public void writePerformanceResult(String testName, int nbUsers, long durationSeconds) {

		String sheetName = testName.equalsIgnoreCase("highVolumeTrackLocation") ? LOCATION_SHEET : REWARD_SHEET;

		writeToSheet(sheetName, nbUsers, durationSeconds);
	}

	private void writeToSheet(String sheetName, int nbUsers, long durationSeconds) {
		File file = new File(FILE_PATH);

		if (!checkFileExists(file))
			return;

		try (FileInputStream fileInputStream = new FileInputStream(file);
				Workbook workbook = WorkbookFactory.create(fileInputStream)) {
			Sheet sheet = getSheet(workbook, sheetName);
			if (sheet == null)
				return;

			if (nbUsers == 100) {
				cleanSheet(sheet);
			}

			appendPerformanceResult(sheet, sheetName, nbUsers, durationSeconds);

			try (FileOutputStream fileOut = new FileOutputStream(FILE_PATH)) {
				workbook.write(fileOut);
			}

			log.info("\n\n✅ Add result in workbook : " + FILE_PATH + " | Sheet : " + sheetName + "\nNumber of user : "
					+ nbUsers + " | duration : " + durationSeconds + "s\n");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean checkFileExists(File file) {
		if (!file.exists()) {
			log.error("❌ File no found : " + FILE_PATH);
			return false;
		}
		return true;
	}

	private Sheet getSheet(Workbook workbook, String sheetName) {
		Sheet sheet = workbook.getSheet(sheetName);
		if (sheet == null) {
			log.error("❌ Sheet : " + sheetName + " not exist");
			return null;
		}
		return sheet;
	}

	private void cleanSheet(Sheet sheet) {
		int lastRow = sheet.getLastRowNum();
		for (int i = 1; i <= lastRow; i++) {
			Row row = sheet.getRow(i);
			if (row != null)
				sheet.removeRow(row);
		}
	}

	private void appendPerformanceResult(Sheet sheet, String sheetName, int nbUsers, long durationSeconds) {
		int lastRowNum = sheet.getLastRowNum();
		Row row = sheet.createRow(lastRowNum + 1);
		row.createCell(1).setCellValue(nbUsers);
		row.createCell(2).setCellValue(durationSeconds);

		if (sheetName.equals(LOCATION_SHEET)) {
			row.createCell(3).setCellValue(900);
		} else if (sheetName.equals(REWARD_SHEET)) {
			row.createCell(3).setCellValue(1200);
		}
	}
}
