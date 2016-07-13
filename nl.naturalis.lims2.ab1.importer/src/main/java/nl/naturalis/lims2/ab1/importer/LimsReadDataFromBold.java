/**
 * 
 */
package nl.naturalis.lims2.ab1.importer;

import java.awt.EventQueue;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nl.naturalis.lims2.utils.LimsFrameProgress;
import nl.naturalis.lims2.utils.LimsImporterUtil;
import nl.naturalis.lims2.utils.LimsLogger;
import nl.naturalis.lims2.utils.LimsNotes;
import nl.naturalis.lims2.utils.LimsReadGeneiousFieldsValues;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.DocumentUtilities;
import com.biomatters.geneious.publicapi.documents.PluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceDocument;
import com.biomatters.geneious.publicapi.implementations.DefaultAlignmentDocument;
import com.biomatters.geneious.publicapi.implementations.sequence.DefaultNucleotideGraphSequence;
import com.biomatters.geneious.publicapi.implementations.sequence.DefaultNucleotideSequence;
import com.biomatters.geneious.publicapi.plugin.DocumentAction;
import com.biomatters.geneious.publicapi.plugin.DocumentOperationException;
import com.biomatters.geneious.publicapi.plugin.DocumentSelectionSignature;
import com.biomatters.geneious.publicapi.plugin.GeneiousActionOptions;
import com.opencsv.CSVReader;

/**
 * @author Reinier.Kartowikromo
 *
 */
public class LimsReadDataFromBold extends DocumentAction {

	private LimsNotes limsNotes = new LimsNotes();
	private LimsImporterUtil limsImporterUtil = new LimsImporterUtil();
	private LimsBoldFields limsBoldFields = new LimsBoldFields();
	private LimsReadGeneiousFieldsValues readGeneiousFieldsValues = new LimsReadGeneiousFieldsValues();
	private SequenceDocument sequenceDocument;
	private static final Logger logger = LoggerFactory
			.getLogger(LimsReadDataFromBold.class);

	/*
	 * String logFileName = limsImporterUtil.getLogPath() + File.separator +
	 * limsImporterUtil.getLogFilename();
	 * 
	 * LimsLogger limsLogger = new LimsLogger(logFileName);
	 */

	private String boldFilePath;
	private String boldFile;
	private String extractIDfileName;
	private List<AnnotatedPluginDocument> docs;
	private LimsFileSelector fcd = new LimsFileSelector();
	private List<String> msgList = new ArrayList<String>();
	private List<String> msgUitvalList = new ArrayList<String>();
	private List<String> verwerkingListCnt = new ArrayList<String>();
	private List<String> verwerkList = new ArrayList<String>();
	private List<String> lackBoldList = new ArrayList<String>();
	private AnnotatedPluginDocument[] documents = null;
	private DefaultAlignmentDocument defaultAlignmentDocument = null;
	private DefaultNucleotideSequence defaultNucleotideSequence = null;
	private Object documentFileName = "";
	private String boldFileSelected = "";
	private boolean result = false;
	private String logBoldFileName = "";
	public int importCounter;
	private int importTotal;
	private String[] record = null;
	private LimsLogger limsLogger = null;
	private boolean isRMNHNumber = false;
	private int crsTotaalRecords = 0;

	LimsFrameProgress limsFrameProgress = new LimsFrameProgress();

	@Override
	public void actionPerformed(
			AnnotatedPluginDocument[] annotatedPluginDocuments) {

		/* Get Databasename */
		readGeneiousFieldsValues.activeDB = readGeneiousFieldsValues
				.getServerDatabaseServiceName();

		if (readGeneiousFieldsValues.activeDB != null) {
			if (DocumentUtilities.getSelectedDocuments().isEmpty()) {
				EventQueue.invokeLater(new Runnable() {

					@Override
					public void run() {
						Dialogs.showMessageDialog("Select at least one document.");
						return;
					}
				});
			}

			if (!DocumentUtilities.getSelectedDocuments().isEmpty()) {
				msgList.clear();

				/*
				 * for (int cnt = 0; cnt < DocumentUtilities
				 * .getSelectedDocuments().size(); cnt++) { isRMNHNumber =
				 * annotatedPluginDocuments[cnt].toString()
				 * .contains("RegistrationNumberCode_Samples"); }
				 */

				/*
				 * if (!isRMNHNumber) { Dialogs.showMessageDialog(
				 * "At least one selected document lacks Registr-nmbr (Sample)."
				 * ); return; }
				 */

				boldFileSelected = fcd.loadSelectedFile();
				if (boldFileSelected == null) {
					return;
				}

				logBoldFileName = limsImporterUtil.getLogPath()
						+ "Bold-Uitvallijst-"
						+ limsImporterUtil.getLogFilename();
				limsLogger = new LimsLogger(logBoldFileName);

				documents = annotatedPluginDocuments;
				logger.info("------------------------------S T A R T -----------------------------------");
				logger.info("Start adding Bold metadata to AB1 File(s)");

				try {
					docs = DocumentUtilities.getSelectedDocuments();
					msgUitvalList.add("Filename: " + boldFileSelected + "\n");
					for (int cnt = 0; cnt < docs.size(); cnt++) {
						documentFileName = annotatedPluginDocuments[cnt]
								.getFieldValue("cache_name");

						/* Add sequence name for the dialog screen */
						if (DocumentUtilities.getSelectedDocuments()
								.listIterator().hasNext()) {
							msgList.add(documentFileName + "\n");
						}

						result = false;

						/* Reads Assembly Contig 1 file */
						try {
							if (readGeneiousFieldsValues
									.getCacheNameFromGeneiousDatabase(
											documentFileName,
											"//document/hiddenFields/override_cache_name")
									.equals(documentFileName)) {

								if (documentFileName.toString()
										.contains("Copy")
										|| documentFileName.toString()
												.contains("kopie")) {
									defaultNucleotideSequence = (DefaultNucleotideGraphSequence) docs
											.get(cnt).getDocument();

									logger.info("Selected Contig document: "
											+ defaultNucleotideSequence
													.getName());

									setExtractIDfileName(defaultNucleotideSequence
											.getName());

									extractIDfileName = getExtractIDFromAB1FileName(defaultNucleotideSequence
											.getName());
									result = true;
								} else {
									defaultAlignmentDocument = (DefaultAlignmentDocument) docs
											.get(cnt).getDocument();

									logger.info("Selected Contig document: "
											+ defaultAlignmentDocument
													.getName());
									setExtractIDfileName(defaultAlignmentDocument
											.getName());
									extractIDfileName = getExtractIDFromAB1FileName(defaultAlignmentDocument
											.getName());
									result = true;
								}
							}
						} catch (IOException e2) {
							e2.printStackTrace();
						}

						/* Reads Assembly Contig 1 consensus sequence */
						try {
							if (readGeneiousFieldsValues
									.getCacheNameFromGeneiousDatabase(
											documentFileName,
											"//document/hiddenFields/cache_name")
									.equals(documentFileName)
									&& !documentFileName.toString().contains(
											"ab1")) {

								defaultNucleotideSequence = (DefaultNucleotideSequence) docs
										.get(cnt).getDocument();

								logger.info("Selected Contig consensus sequence document: "
										+ defaultNucleotideSequence.getName());

								setExtractIDfileName(defaultNucleotideSequence
										.getName());
								extractIDfileName = getExtractIDFromAB1FileName(defaultNucleotideSequence
										.getName());
								result = true;
							}
						} catch (IOException e1) {
							e1.printStackTrace();
						}

						/* AB1 file '//ABIDocument/name' */
						if (readGeneiousFieldsValues
								.getFileNameFromGeneiousDatabase(
										(String) documentFileName,
										"//ABIDocument/name").equals(
										documentFileName)) {

							sequenceDocument = (SequenceDocument) docs.get(cnt)
									.getDocument();
							logger.info("Selected AB1 document: "
									+ sequenceDocument.getName());
							setExtractIDfileName(sequenceDocument.getName());
							extractIDfileName = getExtractIDFromAB1FileName(sequenceDocument
									.getName());
							result = true;

						}

						if (result) {
							limsFrameProgress.createProgressGUI();
							logger.info("CSV Bold file: " + documentFileName);
							logger.info("Start with adding notes to the document");

							readDataFromBold(annotatedPluginDocuments[cnt],
									boldFileSelected, cnt,
									(String) documentFileName);

						}

					}
				} catch (DocumentOperationException e) {
					e.printStackTrace();
				}
				logger.info("Total of document(s) updated: " + docs.size());
				logger.info("------------------------------E N D -----------------------------------");
				logger.info("Done with reading bold file. ");
				EventQueue.invokeLater(new Runnable() {

					@Override
					public void run() {
						Dialogs.showMessageDialog(Integer
								.toString(crsTotaalRecords)
								+ " records have been read of which: "
								+ "\n"
								+ "[1]"
								+ "Bold: "
								+ Integer.toString(importTotal)
								+ " out of "
								+ Integer.toString(docs.size())
								+ " documents are imported."
								+ "\n"
								// + "[2]"
								// + msgList.toString()
								+ "[2] "
								+ "At least one or "
								+ lackBoldList.size()
								+ " selected document lacks Registr-nmbr (Sample).");

						logger.info("Bold: Total imported document(s): "
								+ msgList.size() + "\n");

						limsLogger.logToFile(logBoldFileName,
								msgUitvalList.toString());

						msgList.clear();
						msgUitvalList.clear();
						verwerkingListCnt.clear();
						verwerkList.clear();
						limsFrameProgress.hideFrame();
					}
				});
			}
		}
	}

	@Override
	public GeneiousActionOptions getActionOptions() {
		return new GeneiousActionOptions("4 Bold").setInPopupMenu(true)
				.setMainMenuLocation(GeneiousActionOptions.MainMenu.Tools, 3.0)
				.setInMainToolbar(true).setInPopupMenu(true)
				.setAvailableToWorkflows(true);
	}

	@Override
	public String getHelp() {
		return null;
	}

	@Override
	public DocumentSelectionSignature[] getSelectionSignatures() {
		return new DocumentSelectionSignature[] { new DocumentSelectionSignature(
				PluginDocument.class, 0, Integer.MAX_VALUE) };
	}

	private void readDataFromBold(
			AnnotatedPluginDocument annotatedPluginDocument, String fileName,
			int cnt, String documentName) {

		String[] headerCOI = null;

		if (crsTotaalRecords == 0) {
			try {
				CSVReader csvReadertot = new CSVReader(
						new FileReader(fileName), '\t', '\'', 1);
				crsTotaalRecords = csvReadertot.readAll().size();
				csvReadertot.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			CSVReader csvReader = new CSVReader(new FileReader(fileName), '\t',
					'\'', 0);

			int counter = 0;
			int cntVerwerkt = 0;

			headerCOI = csvReader.readNext();

			try {
				msgUitvalList
						.add("-----------------------------------------------"
								+ "\n");

				msgUitvalList.add("Bold filename: " + documentFileName + "\n");

				while ((record = csvReader.readNext()) != null) {
					if (record.length == 1 && record[0].isEmpty()) {
						continue;
					}

					/** DocumentNoteUtilities-Registration number */
					/** Get value from "RegistrationnumberCode_Samples" */
					String cacheName = "";
					if (documentName.contains("Copy")
							|| documentName.contains("kopie")) {
						cacheName = "//document/hiddenFields/override_cache_name";
					} else {
						cacheName = "//document/hiddenFields/cache_name";
					}

					Object fieldValue = readGeneiousFieldsValues
							.getRegistrationNumberFromTableAnnotatedDocument(
									documentName,
									"//document/notes/note/RegistrationNumberCode_Samples",
									cacheName);

					isRMNHNumber = DocumentUtilities.getSelectedDocuments()
							.get(cnt).toString()
							.contains("RegistrationNumberCode_Samples");

					if (!isRMNHNumber) {
						if (!lackBoldList.contains(DocumentUtilities
								.getSelectedDocuments().get(cnt).getName())) {
							lackBoldList.add(DocumentUtilities
									.getSelectedDocuments().get(cnt).getName());
							logger.info("At least one selected document lacks Registr-nmbr (Sample)."
									+ DocumentUtilities.getSelectedDocuments()
											.get(cnt).getName());
						}
					}

					/** Match only on registration number */
					if (record[2].equals(fieldValue) && isRMNHNumber) {

						limsFrameProgress.showProgress("Match: " + documentName
								+ "\n");
						String processID = record[1];
						String boldURI = "";
						if (processID != null) {
							boldURI = limsImporterUtil.getPropValues("bolduri")
									+ record[1];
						}

						/*
						 * BoldID = 1, NumberofImagesBold = 9, BoldProjectID =
						 * 0, FieldID = 3, BoldBIN = 4, BoldURI = uit
						 * LimsProperties File
						 */
						setNotesThatMatchRegistrationNumber(record[1],
								record[9], record[0], record[3], record[4],
								boldURI);
						setNotesToBoldDocumentsRegistration(documents, cnt);
					} else {
						limsFrameProgress.showProgress("No match: "
								+ documentName + "\n");
					}

					/** Match only on registration number and Marker */
					if (record[2].equals(fieldValue)
							&& headerCOI[6].equals("COI-5P Seq. Length")
							&& isRMNHNumber) {

						limsFrameProgress.showProgress("Match: " + documentName
								+ "\n");

						setNotesThatMatchRegistrationNumberAndMarker(record[6],
								record[7], record[8]);
						setNotesToBoldDocumentsRegistrationMarker(documents,
								cnt);
					} else {
						limsFrameProgress.showProgress("No match: "
								+ documentName + "\n");
					}

					cntVerwerkt++;
					verwerkingListCnt.add(Integer.toString(cntVerwerkt));
					verwerkList.add(record[2]);

					if (!verwerkList.contains(record[5])) {
						if (!msgUitvalList.contains(record[5])) {
							msgUitvalList.add("Catalognumber: " + record[5]
									+ "\n");
						}
					}

					counter++;

				} // end While
				importTotal = counter;
				counter = importTotal - verwerkingListCnt.size();
				msgUitvalList.add("Total records: " + Integer.toString(counter)
						+ "\n");

			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				csvReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getBoldFilePath() {
		return boldFilePath;
	}

	public void setBoldFilePath(String boldFilePath) {
		this.boldFilePath = boldFilePath;
	}

	public String getBoldFile() {
		return boldFile;
	}

	public void setBoldFile(String boldFile) {
		this.boldFile = boldFile;
	}

	public String getExtractIDfileName() {
		return extractIDfileName;
	}

	public void setExtractIDfileName(String extractIDfileName) {
		this.extractIDfileName = extractIDfileName;
	}

	/**
	 * Extract the ID from the filename
	 * 
	 * @param annotatedPluginDocuments
	 *            set the param
	 * @return
	 */
	private String getExtractIDFromAB1FileName(String fileName) {
		/* for example: e4010125015_Sil_tri_MJ243_COI-A01_M13F_A01_008.ab1 */
		String[] underscore = StringUtils.split(fileName, "_");
		return underscore[0];
	}

	/* Set value to Notes */
	private void setNotesToBoldDocumentsRegistrationMarker(
			AnnotatedPluginDocument[] annotatedPluginDocuments, int cnt) {
		/** set note for TraceFile Presence */
		limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
				"TraceFilePresenceCode_Bold", "N traces (Bold)",
				"N traces (Bold)", limsBoldFields.getTraceFilePresence(), cnt);

		/** set note for Nucleotide Length */
		limsNotes
				.setNoteToAB1FileName(annotatedPluginDocuments,
						"NucleotideLengthCode_Bold", "Nucl-length (Bold)",
						"Nucl-length (Bold)",
						limsBoldFields.getNucleotideLength(), cnt);

		/** set note for GenBankID */
		limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
				"GenBankIDCode_Bold", "GenBank ID (Bold)", "GenBank ID (Bold)",
				limsBoldFields.getGenBankID(), cnt);

		/** set note for GenBank URI */
		try {
			limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
					"GenBankURICode_FixedValue_Bold", "GenBank URI (Bold)",
					"GenBank URI (Bold)",
					limsImporterUtil.getPropValues("boldurigenbank")
							+ limsBoldFields.getCoi5PAccession(), cnt);
		} catch (IOException e) {
			e.printStackTrace();
		}

		logger.info("Done with adding notes to the document");
		logger.info(" ");
	}

	/* Set value to Notes */
	private void setNotesToBoldDocumentsRegistration(
			AnnotatedPluginDocument[] annotatedPluginDocuments, int cnt) {
		/** set note for BOLD-ID */
		limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
				"BOLDIDCode_Bold", "BOLD ID (Bold)", "BOLD ID (Bold)",
				limsBoldFields.getBoldID(), cnt);

		/** set note for Number of Images */
		limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
				"NumberOfImagesCode_Bold", "N images (Bold)",
				"N images (Bold)", limsBoldFields.getNumberOfImagesBold(), cnt);

		/** set note for BoldProjectID */
		limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
				"BOLDprojIDCode_Bold", "BOLD proj-ID (Bold)",
				"BOLD proj-ID (Bold)", limsBoldFields.getBoldProjectID(), cnt);

		/** set note for FieldID */
		limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
				"FieldIDCode_Bold", "Field ID (Bold)", "Field ID (Bold)",
				limsBoldFields.getFieldID(), cnt);

		/** set note for BOLD BIN Code */
		limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
				"BOLDBINCode_Bold", "BOLD BIN (Bold)", "BOLD BIN (Bold)",
				limsBoldFields.getBoldBIN(), cnt);

		/** set note for BOLD URI */
		limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
				"BOLDURICode_FixedValue_Bold", "BOLD URI (Bold)",
				"BOLD URI (Bold)", limsBoldFields.getBoldURI(), cnt);

		logger.info("Done with adding notes to the document");
	}

	/* Set value to variable */
	private void setNotesThatMatchRegistrationNumber(String boldID,
			String numberOfImagesBold, String boldProjectID, String fieldID,
			String boldBIN, String boldURI) {

		logger.info("Match Bold record only on registrationnumber.");

		limsBoldFields.setBoldID(boldID);
		limsBoldFields.setNumberOfImagesBold(numberOfImagesBold);
		limsBoldFields.setBoldProjectID(boldProjectID);
		limsBoldFields.setFieldID(fieldID);
		limsBoldFields.setBoldBIN(boldBIN);
		limsBoldFields.setBoldURI(boldURI);

		logger.info("Bold-ID: " + limsBoldFields.getBoldID());
		logger.info("Number of Images Bold: "
				+ limsBoldFields.getNumberOfImagesBold());
		logger.info("BoldProjectID: " + limsBoldFields.getBoldProjectID());
		logger.info("FieldID: " + limsBoldFields.getFieldID());
		logger.info("BoldBIN: " + limsBoldFields.getBoldBIN());
		logger.info("BoldURI: " + limsBoldFields.getBoldURI());

	}

	/* Set value to variable */
	private void setNotesThatMatchRegistrationNumberAndMarker(
			String nucleotideLength, String tracebestandPresence,
			String coi5pAccession) {

		logger.info("Match Bold record on registrationnumber and marker.");

		limsBoldFields.setNucleotideLength(nucleotideLength);
		limsBoldFields.setTraceFilePresence(tracebestandPresence);
		limsBoldFields.setGenBankID(coi5pAccession);
		limsBoldFields.setCoi5PAccession(coi5pAccession);

		logger.info("Nucleotide length: "
				+ limsBoldFields.getNucleotideLength());
		logger.info("TraceFile Presence: "
				+ limsBoldFields.getTraceFilePresence());
		logger.info("GenBankID: " + limsBoldFields.getCoi5PAccession());
		try {
			logger.info("GenBankUri: "
					+ limsImporterUtil.getPropValues("boldurigenbank")
					+ limsBoldFields.getCoi5PAccession());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
