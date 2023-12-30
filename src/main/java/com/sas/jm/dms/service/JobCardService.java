package com.sas.jm.dms.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.sas.jm.dms.email.EmailService;
import com.sas.jm.dms.entity.JobCard;
import com.sas.jm.dms.entity.JobCardCounters;
import com.sas.jm.dms.entity.JobCardInfo;
import com.sas.jm.dms.entity.JobSpares;
import com.sas.jm.dms.entity.JobSparesInfo;
import com.sas.jm.dms.entity.SparesInventory;
import com.sas.jm.dms.repository.JobCardRepository;
import com.sas.jm.dms.repository.JobSparesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobCardService {

	private final JobCardRepository jobCardRepository;
	private final JobSparesRepository jobSparesRepository;
	private final SparesService sparesService;
	private final EmailService emailService;

	private final MongoTemplate mongoTemplate;

	private String[] emailRecepients = { "krishnakumarc27@gmail.com" };

	public int getNextSequence(String sequenceName) {
		// Find the counter document and increment its sequence_value atomically
		Query query = new Query(Criteria.where("_id").is(sequenceName));
		Update update = new Update().inc("sequenceValue", 1);
		FindAndModifyOptions findAndModifyOptions = FindAndModifyOptions.options().returnNew(true);

		JobCardCounters counter = mongoTemplate.findAndModify(query, update, findAndModifyOptions,
				JobCardCounters.class);

		if (counter == null) {
			throw new RuntimeException("Error getting next sequence for " + sequenceName);
		}

		return counter.getSequenceValue();
	}

	public List<?> findAll() {
		return jobCardRepository.findAllByOrderByIdDesc();
	}

	public JobCard save(JobCard jobCard) {
		jobCard.setJobId(getNextSequence("jobCardId"));
		jobCard.setJobCreationDate(LocalDateTime.now());
		jobCard = jobCardRepository.save(jobCard);

		sendNotifications("JobCard opened - " + jobCard.getJobId(), jobCard.toString());
		return jobCard;
	}

	public List<?> findAllByJobStatus(String status) {
		return jobCardRepository.findAllByJobStatusOrderByIdDesc(status);
	}

	public JobCard update(JobCard jobCard) {
		return jobCardRepository.save(jobCard);
	}

	// @Transactional(rollbackFor = Exception.class)
	public synchronized JobSpares updateJobSpares(JobSpares jobSpares) throws Exception {

		JobSpares origJobSpares = jobSparesRepository.findById(jobSpares.getId()).orElse(null);
		// List<JobSparesInfo> origJobSparesInfoList = origJobSpares.getJobSparesInfo();

		List<JobSparesInfo> jobSparesInfoList = jobSpares.getJobSparesInfo();

		// Checking for quantity insufficiency
		for (JobSparesInfo jobSparesInfo : jobSparesInfoList) {
			SparesInventory spares = sparesService.findById(jobSparesInfo.getSparesId());
			if (spares != null) {
				if (origJobSpares != null) {
					List<JobSparesInfo> origJobSparesInfoList = origJobSpares.getJobSparesInfo();
					Optional<JobSparesInfo> origJobSparesInfoOpt = origJobSparesInfoList.stream()
							.filter(info -> info.getSparesId().equals(jobSparesInfo.getSparesId())).findFirst();

					if (origJobSparesInfoOpt.isPresent()) {
						BigDecimal originalQty = origJobSparesInfoOpt.get().getQty();
						spares.setQty(spares.getQty().add(originalQty));
					}
				}
				if (jobSparesInfo.getQty().compareTo(spares.getQty()) > 0) {
					throw new Exception("Quantity of " + spares.getDesc() + " in Spares inventory (" + spares.getQty()
							+ ") is lesser than quantity of spares used for job (" + jobSparesInfo.getQty() + ")");

				}
			}
		}

		String exceptionMess = "";

		if (origJobSpares != null) {
			List<JobSparesInfo> origJobSparesInfoList = origJobSpares.getJobSparesInfo();
			// some jobspares are deleted. hence add those back to jobSpares
			List<JobSparesInfo> deletedJobSparesInfoList = new ArrayList<>(origJobSparesInfoList);
			deletedJobSparesInfoList.removeAll(jobSparesInfoList);

			for (JobSparesInfo deletedJobSparesInfo : deletedJobSparesInfoList) {
				SparesInventory deletedSpares = sparesService.findById(deletedJobSparesInfo.getSparesId());
				if (deletedSpares != null) {
					BigDecimal result = deletedSpares.getQty().add(deletedJobSparesInfo.getQty());
					deletedSpares.setQty(result);
					deletedSpares.setAmount(deletedSpares.getSellRate().multiply(result));
					try {
						sparesService.saveFromJobSpares(deletedSpares);
					} catch (OptimisticLockingFailureException e) {
						exceptionMess += "    Please retry!! Concurrent modification detected while updating deletedJobSpares for "
								+ deletedSpares.getDesc();
						// throw new Exception("Please retry!! Concurrent modification detected for " +
						// spares.getDesc());
					}
				} else {
					exceptionMess += "   " + deletedJobSparesInfo.getSparesAndLabour()
							+ " is not found in Spares Inventory    ";
				}
			}
			// }
		}

		// test for empty scenario TODO

		for (JobSparesInfo jobSparesInfo : jobSparesInfoList) {
			SparesInventory spares = sparesService.findById(jobSparesInfo.getSparesId());
			if (spares != null) {

				// Since jobSpares object from FE contains entire object, check for values
				// present and add them.
				if (origJobSpares != null) {
					List<JobSparesInfo> origJobSparesInfoList = origJobSpares.getJobSparesInfo();
					Optional<JobSparesInfo> origJobSparesInfoOpt = origJobSparesInfoList.stream()
							.filter(info -> info.getSparesId().equals(jobSparesInfo.getSparesId())).findFirst();

					if (origJobSparesInfoOpt.isPresent()) {
						BigDecimal originalQty = origJobSparesInfoOpt.get().getQty();
						spares.setQty(spares.getQty().add(originalQty));
					}
				}
				if (jobSparesInfo.getQty().compareTo(spares.getQty()) > 0) {
					exceptionMess += "   Quantity of " + spares.getDesc() + " in Spares inventory (" + spares.getQty()
							+ ") is lesser than quantity of spares used for job (" + jobSparesInfo.getQty() + ")   ";

				} else {
					BigDecimal result = spares.getQty().subtract(jobSparesInfo.getQty());
					spares.setQty(result);
					spares.setAmount(spares.getSellRate().multiply(result));
					try {
						sparesService.saveFromJobSpares(spares);
					} catch (OptimisticLockingFailureException e) {
						exceptionMess += "    Please retry!! Concurrent modification detected for " + spares.getDesc();
						// throw new Exception("Please retry!! Concurrent modification detected for " +
						// spares.getDesc());
					}
				}

			} else {
				exceptionMess += "   " + jobSparesInfo.getSparesAndLabour() + " is not found in Spares Inventory    ";
				// throw new Exception(jobSparesInfo.getSparesAndLabour() + " is not found in
				// Spares Inventory ");
				// should never come here
			}
		}
		if (!exceptionMess.isEmpty()) {
			jobSparesRepository.save(jobSpares);
			// This will cause the transaction to roll back
			throw new Exception(exceptionMess);
		}
		return jobSparesRepository.save(jobSpares);
	}

	public JobSpares getJobSpares(String id) {
		return jobSparesRepository.findById(id)
				.orElse(JobSpares.builder().jobSparesInfo(new ArrayList<>()).jobLaborInfo(new ArrayList<>()).build());
	}

	public synchronized JobCard updateJobStatus(JobCard jobCard) throws Exception {
		JobCard origJobCard = jobCardRepository.findById(jobCard.getId()).orElse(null);
		if (origJobCard != null) {
			if (jobCard.getJobStatus().equals("CLOSED")) {
				List<JobCardInfo> jobInfoList = origJobCard.getJobInfo();
				for (JobCardInfo jobInfo : jobInfoList) {
					if (!jobInfo.getCompleted().equals("Completed")) { // "Completed" string in UI also.
						throw new Exception(jobInfo.getComplaints() + " is not yet completed for the JobId "
								+ origJobCard.getJobId());
					}
				}
				LocalDateTime jobCloseDate = LocalDateTime.now();
				origJobCard.setJobCloseDate(jobCloseDate);
				JobSpares origJobSpares = jobSparesRepository.findById(jobCard.getId()).orElse(null);
				if (origJobSpares != null) {
					origJobSpares.setJobCloseDate(jobCloseDate);
					jobSparesRepository.save(origJobSpares);
				}
			}
		} else {
			throw new Exception("Invalid jobCard " + jobCard.getJobId());
			// should never come here
		}
		origJobCard.setJobStatus(jobCard.getJobStatus());
		sendNotifications("JobCard - " + jobCard.getJobId() + " status " + jobCard.getJobStatus(), jobCard.toString());

		return jobCardRepository.save(origJobCard);
	}

	public ResponseEntity<?> generateJobCardPdf(String id) throws Exception {

		JobCard jobCard = jobCardRepository.findById(id).orElse(null);
		JobSpares jobSpares = jobSparesRepository.findById(id).orElse(null);

		if (jobCard == null) {
			throw new Exception("JobCard not found for id " + id);
			// should never get here.
		}

		// Create table with varying columns for different rows
		Table table = new Table(UnitValue.createPercentArray(new float[] { 30, 35, 35 }));
		table.setWidth(UnitValue.createPercentValue(100)); // Set the table width to 100%

		Image image = new Image(ImageDataFactory.create("classpath:jm_logo.jpeg")); // Replace with the path to
		// your
		image.setMaxHeight(120);
		image.setMaxWidth(150);// image
		table.addCell(
				new Paragraph("").add(image).setVerticalAlignment(VerticalAlignment.MIDDLE).setKeepTogether(true));

		table.addCell(createCellWithFixedSpace("Job Card No: ", stringNullCheck(jobCard.getJobId()), "\n", "Owner: ",
				stringNullCheck(jobCard.getOwnerName()), "\n", "Contact No: ",
				stringNullCheck(jobCard.getOwnerPhoneNumber())).setVerticalAlignment(VerticalAlignment.MIDDLE)
				.setHorizontalAlignment(HorizontalAlignment.LEFT));

		table.addCell(createCellWithFixedSpace("Date: ", createDateString(jobCard.getJobCreationDate()), "\n",
				"Email: ", stringNullCheck(jobCard.getOwnerEmailId()), "\n", "Driver: ",
				stringNullCheck(jobCard.getDriver())).setFontSize(11).setVerticalAlignment(VerticalAlignment.MIDDLE)
				.setHorizontalAlignment(HorizontalAlignment.LEFT));

		Table singleColumnTable = new Table(UnitValue.createPercentArray(new float[] { 100 }));
		singleColumnTable.setWidth(UnitValue.createPercentValue(100));
		singleColumnTable.addCell(createCellWithFixedSpace("Address: ", stringNullCheck(jobCard.getOwnerAddress()))
				.setFontSize(11).setVerticalAlignment(VerticalAlignment.MIDDLE)
				.setHorizontalAlignment(HorizontalAlignment.LEFT));

		Table doubleColumnTable = new Table(UnitValue.createPercentArray(new float[] { 50, 50 }));
		doubleColumnTable.setWidth(UnitValue.createPercentValue(100));
		Cell ColumnCell = new Cell()
				.add(createCellWithFixedSpace("Vehicle Reg. No: ", stringNullCheck(jobCard.getVehicleRegNo()), "\n",
						"Vehicle Model: ", stringNullCheck(jobCard.getVehicleModel()), "\n", "Technician Name: ",
						stringNullCheck(jobCard.getTechnicianName())));

		doubleColumnTable.addCell(ColumnCell);

		Cell singleColumnCell1 = new Cell()
				.add(createCellWithFixedSpace("Type of Vehicle: " + stringNullCheck(jobCard.getVehicleName()), "\n",
						"K.M: " + stringNullCheck(jobCard.getKiloMeters()), "\n",
						"Vehicle Out Date: " + createDateString(jobCard.getVehicleOutDate())));
		doubleColumnTable.addCell(singleColumnCell1);

		setMinWidth(table, 0, 1, 100f);

		Table table1 = new Table(UnitValue.createPercentArray(new float[] { 30, 20, 20, 30 }));
		table1.setWidth(UnitValue.createPercentValue(100));

		table1.addCell(new Cell().add(new Paragraph("ITEMS")).setBold());
		Cell okCell = new Cell().add(new Paragraph("OK")).setTextAlignment(TextAlignment.CENTER).setBold();
		table1.addCell(okCell);
		Cell notOkCell = new Cell().add(new Paragraph("NOT OK")).setTextAlignment(TextAlignment.CENTER).setBold();
		table1.addCell(notOkCell);
		Image image1 = new Image(ImageDataFactory.create("classpath:jm_scratch_pic.JPG"));

		image1.setMaxHeight(720);
		image1.setMaxWidth(150);
		image1.setPaddingLeft(1);// image

		table1.addCell(new Cell(11, 1).add(image1).setVerticalAlignment(VerticalAlignment.MIDDLE).setPaddingLeft(20));

		table1.addCell(new Cell().add(new Paragraph("COVER")).setBold());
		updateVechicleItems(table1, jobCard.getCover());

		table1.addCell(new Cell().add(new Paragraph("GLASS")).setBold());
		updateVechicleItems(table1, jobCard.getGlass());

		table1.addCell(new Cell().add(new Paragraph("DASHBOARD & TOOLS")).setBold());
		updateVechicleItems(table1, jobCard.getDashboardAndTools());

		table1.addCell(new Cell().add(new Paragraph("FUEL POINTS")).setBold());
		table1.addCell(stringNullCheck(jobCard.getFuelPoints()));
		table1.addCell("");

		table1.addCell(new Cell().add(new Paragraph("SPARE WHEEL")).setBold());
		updateVechicleItems(table1, jobCard.getSpareWheel());

		table1.addCell(new Cell().add(new Paragraph("JACKEY HANDLES")).setBold());
		updateVechicleItems(table1, jobCard.getJackeyHandles());

		table1.addCell(new Cell().add(new Paragraph("TOOL KITS")).setBold());
		updateVechicleItems(table1, jobCard.getToolKits());

		table1.addCell(new Cell().add(new Paragraph("PEN DRIVE")).setBold());
		updateVechicleItems(table1, jobCard.getPenDrive());

		table1.addCell(new Cell().add(new Paragraph("WHEEL CAP")).setBold());
		updateVechicleItems(table1, jobCard.getWheelCap());

		table1.addCell(new Cell().add(new Paragraph("A/c GRILLS")).setBold());
		updateVechicleItems(table1, jobCard.getAcGrills());

		Table table2 = new Table(UnitValue.createPercentArray(new float[] { 60, 20, 20 }));
		table2.setWidth(UnitValue.createPercentValue(100));

		Cell customerComplaintsCell = new Cell().add(new Paragraph("CUSTOMER COMPLAINTS"))
				.setTextAlignment(TextAlignment.CENTER).setBold();

		Cell completedCell = new Cell().add(new Paragraph("COMPLETED")).setTextAlignment(TextAlignment.CENTER)
				.setBold();

		Cell remarksCell = new Cell().add(new Paragraph("REMARKS")).setTextAlignment(TextAlignment.CENTER).setBold();

		table2.addHeaderCell(customerComplaintsCell);
		table2.addHeaderCell(completedCell);
		table2.addHeaderCell(remarksCell);

		for (JobCardInfo jobInfo : jobCard.getJobInfo()) {
			table2.addCell(stringNullCheck(jobInfo.getComplaints()));
			table2.addCell(stringNullCheck(jobInfo.getCompleted()));
			table2.addCell(stringNullCheck(jobInfo.getRemarks()));
		}

		Table table3 = null;
		if (jobSpares != null) {
			table3 = new Table(UnitValue.createPercentArray(new float[] { 50, 10, 20, 30 }));
			table3.setWidth(UnitValue.createPercentValue(100));
			if (jobSpares.getJobSparesInfo() != null) {

				Cell sparesCell = new Cell().add(new Paragraph("Spares")).setTextAlignment(TextAlignment.CENTER)
						.setBold();

				Cell qtySparesCell = new Cell().add(new Paragraph("Qty.")).setTextAlignment(TextAlignment.CENTER)
						.setBold();

				Cell rateSparesCell = new Cell().add(new Paragraph("Rate")).setTextAlignment(TextAlignment.CENTER)
						.setBold();

				Cell amountSparesCell = new Cell().add(new Paragraph("Amount")).setTextAlignment(TextAlignment.CENTER)
						.setBold();

				table3.addCell(sparesCell);
				table3.addCell(qtySparesCell);
				table3.addCell(rateSparesCell);
				table3.addCell(amountSparesCell);
				for (JobSparesInfo jobSparesInfo : jobSpares.getJobSparesInfo()) {
					table3.addCell(stringNullCheck(jobSparesInfo.getSparesAndLabour()));
					table3.addCell(new Cell().add(new Paragraph(jobSparesInfo.getQty().toString()))
							.setTextAlignment(TextAlignment.RIGHT));
					table3.addCell(new Cell().add(new Paragraph(jobSparesInfo.getRate().toString()))
							.setTextAlignment(TextAlignment.RIGHT));
					table3.addCell(new Cell().add(new Paragraph(jobSparesInfo.getAmount().toString()))
							.setTextAlignment(TextAlignment.RIGHT));
				}
			}
			if (jobSpares.getJobLaborInfo() != null) {
				Cell labourCell = new Cell().add(new Paragraph("Labour")).setTextAlignment(TextAlignment.CENTER)
						.setBold();
				Cell qtyLabourCell = new Cell().add(new Paragraph("Qty.")).setTextAlignment(TextAlignment.CENTER)
						.setBold();
				Cell rateLabourCell = new Cell().add(new Paragraph("Rate")).setTextAlignment(TextAlignment.CENTER)
						.setBold();
				Cell amountLabourCell = new Cell().add(new Paragraph("Amount")).setTextAlignment(TextAlignment.CENTER)
						.setBold();
				table3.addCell(labourCell);
				table3.addCell(qtyLabourCell);
				table3.addCell(rateLabourCell);
				table3.addCell(amountLabourCell);

				for (JobSparesInfo jobLaborInfo : jobSpares.getJobLaborInfo()) {
					table3.addCell(stringNullCheck(jobLaborInfo.getSparesAndLabour()));
					table3.addCell(new Cell().add(new Paragraph(jobLaborInfo.getQty().toString()))
							.setTextAlignment(TextAlignment.RIGHT));
					table3.addCell(new Cell().add(new Paragraph(jobLaborInfo.getRate().toString()))
							.setTextAlignment(TextAlignment.RIGHT));
					table3.addCell(new Cell().add(new Paragraph(jobLaborInfo.getAmount().toString()))
							.setTextAlignment(TextAlignment.RIGHT));
				}
			}

			table3.addCell(new Cell(1, 2).add(new Paragraph("Total Spares Value")).setTextAlignment(TextAlignment.RIGHT)
					.setBold());
			table3.addCell(new Cell(1, 2).add(new Paragraph(stringNullCheck(jobSpares.getTotalSparesValue()))
					.setTextAlignment(TextAlignment.RIGHT)));

			table3.addCell(new Cell(1, 2).add(new Paragraph("Total Labour Value")).setTextAlignment(TextAlignment.RIGHT)
					.setBold());
			table3.addCell(new Cell(1, 2).add(new Paragraph(stringNullCheck(jobSpares.getTotalLabourValue()))
					.setTextAlignment(TextAlignment.RIGHT)));

			table3.addCell(
					new Cell(1, 2).add(new Paragraph("Grand Total")).setTextAlignment(TextAlignment.RIGHT).setBold());
			table3.addCell(new Cell(1, 2).add(
					new Paragraph(stringNullCheck(jobSpares.getGrandTotal())).setTextAlignment(TextAlignment.RIGHT)));
		}

//		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//				PdfWriter pdfWriter = new PdfWriter(outputStream);
//				PdfDocument pdfDocument = new PdfDocument(pdfWriter);
//				Document document = new Document(pdfDocument)) {
//
//			document.add(table);
//			document.add(singleColumnTable);
//			document.add(doubleColumnTable);
//			document.add(table1);
//			document.add(table2);
//			if (table3 != null) {
//				document.add(table3);
//			}
//			ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());
//			String filename = jobCard.getJobId() + "_" + jobCard.getVehicleRegNo() + ".pdf";
//			HttpHeaders headers = new HttpHeaders();
//			headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
//			return ResponseEntity.ok().headers(headers).contentLength(resource.contentLength())
//					.contentType(MediaType.APPLICATION_PDF).body(resource);
//		} catch (Exception e) {
//			throw new Exception(e);
//		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PdfWriter pdfWriter = new PdfWriter(outputStream);
		PdfDocument pdfDocument = new PdfDocument(pdfWriter);
		Document document = new Document(pdfDocument);

		document.add(table);
		document.add(singleColumnTable);
		document.add(doubleColumnTable);
		document.add(table1);
		document.add(table2);
		if (table3 != null) {
			document.add(table3);
		}
		document.close();
		pdfDocument.close();
		pdfWriter.close();
		outputStream.close();

		ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());
		String filename = jobCard.getJobId() + "_" + jobCard.getVehicleRegNo() + ".pdf";
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
		return ResponseEntity.ok().headers(headers).contentLength(resource.contentLength())
				.contentType(MediaType.APPLICATION_PDF).body(resource);
	}

	private Paragraph createCellWithFixedSpace(String... texts) {
		// TODO Auto-generated method stub
		String regex = "Job Card No: |Owner: |Address: |Contact No: |Driver: |Vehicle Reg. No: |Vehicle Model: |Technician Name: |Type of Vehicle: |K.M: |Vehicle Out Date: |Date: |Email: ";
		Paragraph paragraph = new Paragraph();
		for (String text : texts) {
			if (text.equals("\n")) {
				paragraph.add(text);
			} else if (text.matches(regex)) {
				paragraph.add(new Text(text).setBold());
			} else {
				paragraph.add(new Text(text).setTextAlignment(TextAlignment.RIGHT));
			}

		}
		return paragraph;

	}

	private void setMinWidth(Table table, int rowIndex, int columnIndex, float minWidth) {
		if (rowIndex < table.getNumberOfRows() && columnIndex < table.getNumberOfColumns()) {
			table.getCell(rowIndex, columnIndex).setMinWidth(minWidth);
		}
	}

	private void updateVechicleItems(Table table1, String items) {
		if (items != null) {
			if (items.equals("OK")) {
				table1.addCell("OK");
				table1.addCell("");
			} else {
				table1.addCell("");
				table1.addCell("NOT OK");
			}
		} else {
			table1.addCell("");
			table1.addCell("");
		}
	}

	private String stringNullCheck(Object str) {
		if (str == null)
			return "";
		return String.valueOf(str);
	}

	private String createDateString(LocalDateTime date) {
		if (date != null) {
			return date.getDayOfMonth() + "/" + date.getMonthValue() + "/" + date.getYear() + " - " + date.getHour()
					+ ":" + date.getMinute();
		}
		return "";
	}

	private void sendNotifications(String title, String body) {
//		EmailDetails emailDetails = EmailDetails.builder().msgBody(title).subject(title).build();
//		emailService.sendTableMail(emailDetails, emailRecepients, body);

	}
}
