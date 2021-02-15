package com.test.report;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.IRequestListener;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.Strings;

public class TestResourcePanel extends Panel {

	/** The oo pdf converter service. */
	@SpringBean
	private static TestPDFConverterService testPDFConverterService;

	private static final long serialVersionUID = -1L;

	private ResourceReference pdfResource = null;
	
	private static File pdfFile = null;

	public TestResourcePanel(String id) {
		super(id);
		add(new TestInlineFrame(id, null));
	}


	public TestResourcePanel(String id, final ReportService reportService, final TestLGLEntity testLGLEntity , final ScenarioEnum scenario, final Currency ccy, final Map<TestEntity, TestBucketSet> testEntityMap, final FileFormatEnum fileFormat) {
		super(id);
		add(getPDFResource(id, reportService, testLGLEntity , scenario, ccy, testEntityMap, fileFormat));
	}
	
	private TestInlineFrame getPDFResource(String id, final ReportService reportService, final TestLGLEntity testLGLEntity , final ScenarioEnum scenario, final Currency ccy, final Map<TestEntity, TestBucketSet> testEntityMap, final FileFormatEnum fileFormat) {
					
		/*pdfResource = new ResourceReference("") {
			private static final long serialVersionUID = -1L;
			@Override
			public IResource getResource() {
				return getByteArrayResources(reportService, testLGLEntity, scenario, ccy, testEntityMap, fileFormat);
			}
		};*/
		
		/*TestInlineFrame iFrame = new TestInlineFrame(id, new ResourceReference("") {
			private static final long serialVersionUID = -1L;

			@Override
			public IResource getResource() {
				return getByteArrayResources(reportService, testLGLEntity, scenario, ccy, testEntityMap, fileFormat);
			}
		});*/
		
		pdfResource = getResourceReference(reportService, testLGLEntity, scenario, ccy, testEntityMap, fileFormat);
		TestInlineFrame iFrame = new TestInlineFrame(id, pdfResource);
		return iFrame;
	}
	
	
	private static ResourceReference getResourceReference(ReportService reportService, TestLGLEntity testLGLEntity , ScenarioEnum scenario, Currency ccy,
			Map<TestEntity, TestBucketSet> testEntityMap, FileFormatEnum fileFormat) {
		ResourceReference newPdfResource = new ResourceReference("") {
			private static final long serialVersionUID = -1L;
			@Override
			public IResource getResource() {
				return getByteArrayResources(reportService, testLGLEntity, scenario, ccy, testEntityMap, fileFormat);
			}
		};
		return newPdfResource;
	}

	public static ByteArrayResource getByteArrayResources(ReportService reportService, TestLGLEntity testLGLEntity, ScenarioEnum scenario, Currency ccy, Map<TestEntity, TestBucketSet> testEntityMap, FileFormatEnum fileFormat) {

		String sheetName = "";
		byte [] pdfByte = null;
		ByteArrayOutputStream excelBaos = null;
		ByteArrayOutputStream pdfBaos = null;
		FileFormatEnum pdfFileFormat = FileFormatEnum.PDF;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			Date asOfDate = getSession.get().getAsOfDate();
			OOConfig ooConfig = ((WicketApplication) WebApplication.get()).getOpenOfficeConfig();
			if (null == ooConfig) {
				pdfByte = TestWebResource.getDummyData();
			}
			try {
				excelBaos = reportService.getExcelSheetAsStream(asOfDate, scenario, ccy, testEntityMap, fileFormat, testLGLEntity);
				
				// for reports, which are not scenario dependent
				if(scenario == null) {
					sheetName = "UNKOWN";
				} else {
					sheetName = scenario.name();
				}
				sheetName += "_" + sdf.format(new Date());

				pdfBaos = fileToByteArrayOutputStream(testPDFConverterService.convertToPDFFile(excelBaos, sheetName, ooConfig, pdfFileFormat));
			} catch (LiquiException e) {
				e.printStackTrace();
			}
			
			if(pdfByte == null) {
				pdfByte = new byte[0];
			}
			pdfFile = new File(sheetName+pdfFileFormat.getFileExtension());
			pdfFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(pdfFile);
			fos.write(pdfBaos.toByteArray());
			System.out.println("File Path: " + pdfFile.getAbsolutePath());
			fos.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		ByteArrayResource bar = new ByteArrayResource(pdfFileFormat.getContextType(), pdfBaos.toByteArray(), sheetName+pdfFileFormat.getFileExtension());
		return bar;
	}
	
	private static ByteArrayOutputStream fileToByteArrayOutputStream(File file) throws Exception {
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

	/**
	 * The Class TestInlineFrame.
	 */
	public class TestInlineFrame extends WebMarkupContainer implements IRequestListener {

		private static final long serialVersionUID = -1L;
		
		//private final IRequestListener resourceListener;
		
		private final ResourceReference resourceReference;

		/*public TestInlineFrame(final String id, IRequestListener resourceListener) {
			super(id);
			this.resourceListener = resourceListener;
		}*/

		public TestInlineFrame(final String id, ResourceReference resourceReference) {
			super(id);
			//this.resourceListener = resourceListener;
			this.resourceReference = resourceReference;
		}
	
		protected CharSequence getURL() {
			CharSequence charSequence = urlFor(pdfResource, null);		
			return charSequence;
		}


		@Override
		protected final void onComponentTag(final ComponentTag tag) {
			checkComponentTag(tag, "iframe");

			// Set href to link to this frame's frameRequested method
			CharSequence url = getURL();
			
			System.out.println("getURL(): " + url);

			// generate the src attribute
			tag.put("src", Strings.replaceAll(url, "&", "&amp;"));
			
			/*if(pdfFile.isFile()) {
				tag.put("src", "file:///" +Strings.replaceAll(pdfFile.getAbsolutePath(), "&", "&amp;"));
			}*/

			//tag.put("type", "application/pdf");
			//tag.put("src", "C://Workspace_Related//apache-tomcat-8.5.53_Eclipse//backup//test.pdf");
			
			tag.put("target", "_self");
			
			System.out.println("iframe tag : " + tag.toString());

			super.onComponentTag(tag);
		}

		@Override
		protected boolean getStatelessHint() {
			return false;
		}

		@Override
		public void onRequest() {
			//this.resourceListener.onRequest();
		}
		
		@Override
		public boolean rendersPage() {
			return true;
		}
	}

}
