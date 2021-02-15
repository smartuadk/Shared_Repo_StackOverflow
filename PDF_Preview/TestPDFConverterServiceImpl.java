package com.test.common.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.ExternalOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeConnectionProtocol;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.springframework.stereotype.Component;

import com.test.TestPDFConverterService;


@Component
public class TestPDFConverterServiceImpl implements TestPDFConverterService {


	@Override
	public File convertToPDFFile(ByteArrayOutputStream fromExcelFile, String sheetName, OOConfig ooConfig,
			FileFormatEnum fileFormat) throws Exception {
		File tempFile = null;
		File resultFile = null;
		try {
			tempFile = File.createTempFile(sheetName, fileFormat.getFileExtension());
			tempFile.setWritable(true);

			FileOutputStream fout = new FileOutputStream(tempFile);
			fromExcelFile.writeTo(fout);

			// convert document
			ExternalOfficeManagerConfiguration eomc = new ExternalOfficeManagerConfiguration();
			eomc.setConnectOnStart(true);

			// DefaultOfficeManagerConfiguration domc = new DefaultOfficeManagerConfiguration();
			eomc.setConnectionProtocol(ooConfig.getProtocol());
			// eomc.setOfficeHome(ooConfig.getOpenofficeHome());

			// domc.setHostname(ooConfig.getHostname());
			eomc.setPortNumber(ooConfig.getPort());

			OfficeManager officeManager = eomc.buildOfficeManager();
			officeManager.start();
			OfficeDocumentConverter converter = new OfficeDocumentConverter(officeManager);
			resultFile = File.createTempFile(sheetName, FileFormatEnum.PDF.getFileExtension());
			converter.convert(tempFile, resultFile);
			fout.close();
			officeManager.stop();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (tempFile != null) {
				tempFile.delete();
				tempFile = null;
			}
		}

		return resultFile;
	}

	@Override
	public ByteArrayOutputStream convertToPDFStream(ByteArrayOutputStream pFromExcelFile, String pSheetName, OOConfig pOoConfig, FileFormatEnum pFileFormat) throws Exception {
		return fileToByteArrayOutputStream(pSheetName, convertToPDFFile(pFromExcelFile, pSheetName, pOoConfig, pFileFormat));
	}

	private ByteArrayOutputStream fileToByteArrayOutputStream(String sheetName, File file) throws Exception {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(file);
			byte[] buffer = new byte[16384];
			for (int len = fin.read(buffer); len > 0; len = fin.read(buffer)) {
				result.write(buffer, 0, len);
			}
			result.close();
			fin.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(fin);
			file.delete();
		}
		return result;
	}
}
