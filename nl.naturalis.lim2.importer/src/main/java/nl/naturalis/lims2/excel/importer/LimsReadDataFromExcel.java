/**
 * 
 */
package nl.naturalis.lims2.excel.importer;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nl.naturalis.lims2.utils.LimsImporterUtil;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.DocumentUtilities;
import com.biomatters.geneious.publicapi.documents.PluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceDocument;
import com.biomatters.geneious.publicapi.plugin.DocumentAction;
import com.biomatters.geneious.publicapi.plugin.DocumentOperationException;
import com.biomatters.geneious.publicapi.plugin.DocumentSelectionSignature;
import com.biomatters.geneious.publicapi.plugin.GeneiousActionOptions;
import com.opencsv.CSVReader;

/**
 * @author Reinier.Kartowikromo
 *
 */
public class LimsReadDataFromExcel extends DocumentAction {

	static final Logger logger;

	static {
		logger = LoggerFactory.getLogger(LimsReadDataFromExcel.class);
	}
	PluginDocument annotatedPluginDocument;
	LimsImporterUtil limsImporterUtil = new LimsImporterUtil();
	LimsExcelFields limsExcelFields = new LimsExcelFields();
	LimsNotes limsNotes = new LimsNotes();
	String extractIDfileName = null;

	List<String> listExtractID = new ArrayList<String>();
	SequenceDocument seq;

	@Override
	public void actionPerformed(
			AnnotatedPluginDocument[] annotatedPluginDocuments) {

		logger.info("-----------------------------------------------------------------");
		logger.info("Start");

		if (annotatedPluginDocuments[0] != null) {
			List<AnnotatedPluginDocument> docs;
			try {
				docs = DocumentUtilities.getSelectedDocuments();
				for (int cnt = 0; cnt < docs.size(); cnt++) {

					seq = (SequenceDocument) docs.get(cnt).getDocument();
					logger.info("Selected document: " + seq.getName());
					extractIDfileName = getExtractIDFromAB1FileName(seq
							.getName());
					readDataFromExcel(annotatedPluginDocuments);

					/*
					 * setNoteToAB1FileName(AnnotatedPluginDocument[]
					 * annotatedPluginDocuments, String fieldCode, String
					 * textNoteField, String noteTypeCode, String fieldValue)
					 */

					/* set note for Extract-ID */
					limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
							"ExtractIdCode", "Extract ID", "Extract-ID",
							limsExcelFields.getExtractID(), cnt);

					/* set note for Project Plaatnummer */
					limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
							"ProjectPlaatnummerCode", "Project Plaatnummer",
							"Project Plaatnummer",
							limsExcelFields.getProjectPlaatNummer(), cnt);

					/* Set note for Extract Plaatnummer */
					limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
							"ExtractPlaatNummerCode", "Extract Plaatnummer",
							"Extract Plaatnummer",
							limsExcelFields.getExtractPlaatNummer(), cnt);

					/* set note for Taxonnaam */
					limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
							"TaxonNaamCode", "Taxon naam", "Taxon naam",
							limsExcelFields.getTaxonNaam(), cnt);

					/* set note for Registrationnumber */
					limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
							"BasisOfRecordCode", "Registrationnumber",
							"Registrationnumber",
							limsExcelFields.getRegistrationNumber(), cnt);

					/* set note for Plaat positie */
					limsNotes.setNoteToAB1FileName(annotatedPluginDocuments,
							"PlaatpositieCode", "Plaat positie",
							"Plaat positie", limsExcelFields.getPlaatPositie(),
							cnt);

				}
			} catch (DocumentOperationException e) {
				e.printStackTrace();
			}
		}
		logger.info("-----------------------------------------------------------------");
		logger.info("Done with reading excel file. ");

	}

	@Override
	public GeneiousActionOptions getActionOptions() {
		return new GeneiousActionOptions("Read data from Excel")
				.setInMainToolbar(true);
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

	private void readDataFromExcel(
			AnnotatedPluginDocument[] annotatedPluginDocuments) {
		String csvPath = null;
		String csvFile = null;
		try {
			csvFile = limsImporterUtil.getFileFromPropertieFile();
			csvPath = limsImporterUtil.getPropValues() + csvFile;
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("CSV file: " + csvPath);

		try {
			CSVReader csvReader = new CSVReader(new FileReader(csvPath), '\t',
					'\'', 0);

			String[] record = null;
			csvReader.readNext();

			try {
				while ((record = csvReader.readNext()) != null) {
					if (record.length == 0) {
						continue;
					}

					String ID = "e" + record[3];

					if (ID.equals(extractIDfileName)) {
						limsExcelFields.setProjectPlaatNummer(record[0]);
						limsExcelFields.setPlaatPositie(record[1]);
						limsExcelFields.setExtractPlaatNummer(record[2]);
						if (record[3] != null) {
							limsExcelFields.setExtractID(ID);
						}
						limsExcelFields.setRegistrationNumber(record[4]);
						limsExcelFields.setTaxonNaam(record[5]);
						// limsExcelFields.setSubSample(record[0]);

						logger.info("Extract-ID: "
								+ limsExcelFields.getExtractID());
						logger.info("Project plaatnummer: "
								+ limsExcelFields.getProjectPlaatNummer());
						logger.info("Extract plaatnummer: "
								+ limsExcelFields.getExtractPlaatNummer());
						logger.info("Taxon naam: "
								+ limsExcelFields.getTaxonNaam());
						logger.info("Registrationnumber: "
								+ limsExcelFields.getRegistrationNumber());
						logger.info("Plaat positie: "
								+ limsExcelFields.getPlaatPositie());

						logger.info("Done with adding notes to the document");

					} // end IF
				} // end While
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

	/**
	 * Extract the ID from the filename
	 * 
	 * @param annotatedPluginDocuments
	 *            set the param
	 * @return
	 */
	private String getExtractIDFromAB1FileName(String fileName) {
		/* for example: e4010125015_Sil_tri_MJ243_COI-A01_M13F_A01_008.ab1 */
		logger.info("Document Filename: " + fileName);
		String[] underscore = StringUtils.split(fileName, "_");
		return underscore[0];
	}

}