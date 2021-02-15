package com.report;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class TestPDFPreview extends LayoutPage {
	private static final long serialVersionUID = -1L;

	private final TestLGLEntity testLGLEntity;

	@SpringBean
	private TestPDFPreviewService testPDFPreviewService;

	public TestPDFPreview(PageParameters id) {
		super(id);
		this.testLGLEntity = TestLGLEntity.FMSW;

		setOutputMarkupId(true);

		ResourceLink<Object> excelLink = getExcelLink();
		addOrReplace(excelLink);

		TestResourcePanel resourcePanel = createResourePanel(null, null, null, true, ReportNames.REPORTS.getReportTemplateFormat());
		addOrReplace(resourcePanel);
		
		//ResourceLink<Object> pdfLink = getPDFLink();
		//addOrReplace(pdfLink);
	}

	/**
	 * Gets the excel link.
	 *
	 * @return the excel link
	 */
	private ResourceLink<Object> getExcelLink() {
		// FileFormatEnum fileFormat = ReportNames.REPORT.getReportTemplateFormat();
		/*ResourceLink<Object> excelLink = new ResourceLink<Object>("excel", new ExcelResource(getExcelName(), false, FileFormatEnum.XLSX2) {
			private static final long serialVersionUID = -1L;

			@Override
			public InputStream generateInputStream() {
				return generateExcelSheet();
			}
		});*/

		FileFormatEnum fileFormat = FileFormatEnum.XLSX2;
		ResourceLink<Object> excelLink =  new ResourceLink<>("excel", new ResourceReference(getExcelName()) {
			private static final long serialVersionUID = 1L;

			@Override
			public IResource getResource() {
				byte[] excelBytes = null;
				try {
					excelBytes = testPDFPreviewService.generateExcelReport(getSession.get().getAsOfDate(), testLGLEntity).toByteArray();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if(excelBytes == null) {
					excelBytes = new byte[0];
				}
				return new ByteArrayResource(fileFormat.getContextType(), excelBytes, getExcelName()+fileFormat.getFileExtension());
			}
		});
		
		excelLink.setOutputMarkupId(true);
		excelLink.add(new Label("excelLabel", "Download report as Excel"));
		return excelLink;
	}

	/**
	 * Generate excel sheet.
	 *
	 * @return the byte array output stream
	 */
	/*public InputStream generateExcelSheet() {
		try {
			return new ByteArrayInputStream(testPDFPreviewService.generateExcelReport(getSession.get().getAsOfDate(), testLGLEntity).toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

	/**
	 * Gets the excel name.
	 *
	 * @return the excel name
	 */
	public String getExcelName() {
		return ReportNames.REPORT.getReportName()
				+ "_"
				+ DateUtilities.getFormattedDate(getSession.get().getAsOfDate(), DateUtilities.getDatePattern(getSession.get().getLocale()))
				+ "_"
				+ DateUtilities.getFormattedDate(new Date(), DateUtilities.getDatePattern(getSession.get().getLocale()))
				+ "_" + DateUtilities.getFormattedDate(new Date(), DateUtilities.SDF_HOUR_SEC);
	}

	/**
	 * Adds the resoure panel.
	 *
	 * @param scenario the scenario
	 *
	 * @return the component
	 */
	private TestResourcePanel createResourePanel(ScenarioEnum scenario, Currency ccy,
			Map<TestLGLEntity, TestBucketSet> testEntityMap, boolean enabled, FileFormatEnum fileFormat) {
		TestResourcePanel rp = null;
		if (enabled) {
			rp = new TestResourcePanel("resourcePanel", reportService, TestLGLEntity.FMSW, null, null, null, fileFormat);
			rp.setOutputMarkupId(true);
			//rp.setRedirect(false);
			rp.setRenderBodyOnly(true);
			rp.setVisible(true);
			
			//For Testing
			/*PopupSettings popupSettings = new PopupSettings(PopupSettings.RESIZABLE |    PopupSettings.SCROLLBARS).setHeight(500).setWidth(700);
			ResourceLink pdfResourceLink = getPDFLink(reportService, testLGLEntity, scenario, ccy, testEntityMap, fileFormat);
			pdfResourceLink.setPopupSettings(popupSettings);
			addOrReplace(pdfResourceLink);*/
		} else {
			rp = new TestResourcePanel("resourcePanel");
			rp.setVisible(false);
		}
		rp.setEnabled(enabled);
		return rp;
	}
	
	
	/**
	 * Gets the pdf link.
	 *
	 * @return the pdf link
	 */
	@SuppressWarnings("unused")
	private ResourceLink<Object> getPDFLink(ReportService reportService, TestLGLEntity testLGLEntity, ScenarioEnum scenario, Currency ccy,
			Map<TestLGLEntity, TestBucketSet> testEntityMap, FileFormatEnum fileFormat) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		final String sheetName = "data_" + "_" + sdf.format(new Date());
		//final OOConfig ooConfig = ((WicketApplication) WebApplication.get()).getOpenOfficeConfig();
		final FileFormatEnum pdfFileFormat = FileFormatEnum.PDF;

		ResourceLink<Object> pdfLink =  new ResourceLink<>("pdf", new ResourceReference(sheetName) {
			private static final long serialVersionUID = 1L;

			@Override
			public IResource getResource() {
				return TestResourcePanel.getByteArrayResources(reportService, testLGLEntity, scenario, ccy, testEntityMap, pdfFileFormat);
			}
		});
		pdfLink.setOutputMarkupId(true);
		pdfLink.add(new Label("pdfLabel", "Download report as PDF"));
		
		return pdfLink;
	}
	
}
