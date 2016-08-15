/**
 * 
 */
package nl.naturalis.lims2.utils;

import java.awt.EventQueue;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.domainobject.util.debug.BeanPrinter;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.databaseservice.WritableDatabaseService;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.DocumentNote;
import com.biomatters.geneious.publicapi.documents.DocumentNoteType;
import com.biomatters.geneious.publicapi.documents.DocumentNoteUtilities;
import com.biomatters.geneious.publicapi.plugin.PluginUtilities;

/**
 * @author Reinier.Kartowikromo
 *
 */
public class LimsReadGeneiousFieldsValues {

	private String url = "";
	private String user = "";
	private String password = "";
	private String ssl = "";
	private static final Logger logger = LoggerFactory
			.getLogger(LimsReadGeneiousFieldsValues.class);
	private LimsImporterUtil limsImporterUtil = new LimsImporterUtil();
	private LimsLogList limsLogList = new LimsLogList();
	private String dummyPcrPlateIdSeqValue = "";
	private String dummyMarkerSeqValue = "";
	private String dummyRegistrNmbrSamplesValue = "";
	private String dummyScientificNameSamplesValue = "";
	private String dummySamplePlateIdSamplesValue = "";
	private String dummyPositionSamplesValue = "";
	private String dummyExtractIDSamplesValue = "";
	private String dummySeqStaffSamplesValue = "";
	private List<String> listDummyValues = new ArrayList<String>();
	public String activeDB = "";
	public String extractidSamplesFromDummy;
	public String samplePlateIdSamplesFromDummy;
	public String scientificNameSamplesFromDummy;
	public String registrnmbrSamplesFromDummy;
	public String positionSamplesFromDummy;
	public String extractPlateIDSamples;
	public String extractionMethodSamples;
	public String registrationScientificName;
	private SQLException exception = null;
	public int recordcount = 0;

	public LimsReadGeneiousFieldsValues() {

	}

	// TODO: VERWIJDEREN:
	/*
	 * Mag later worden verwijderd na opruiming van de volgende bestanden Used
	 * by LimsCRSImporter and LimsReadDataFromSamples (both depricated)
	 */
	public Object readValueFromAnnotatedPluginDocument(
			AnnotatedPluginDocument annotatedPluginDocuments, String noteCode,
			Object fieldName) {

		/** noteCode = "DocumentNoteUtilities-Registration number"; */
		DocumentNoteType noteType = DocumentNoteUtilities.getNoteType(noteCode);
		Object fieldValue = null;

		if (noteType != null) {
			AnnotatedPluginDocument.DocumentNotes documentNotes = annotatedPluginDocuments
					.getDocumentNotes(true);

			DocumentNote bos = documentNotes.getNote(noteCode);
			/** example: FieldName = "BasisOfRecordCode" */
			if (bos != null) {
				fieldValue = bos.getFieldValue((String) fieldName);
			} else {
				fieldValue = null;
			}
		}

		return fieldValue;
	}

	/*
	 * TODO: Verwijderen: Check if Notes value exists in a selected Document
	 * file Return Boolean value = true or false Used in LimsImportAB1Update and
	 * (Depricated)LimsReaddataFromSamples
	 */
	public boolean getValueFromAnnotatedPluginDocument(
			AnnotatedPluginDocument annotatedPluginDocuments, String noteCode,
			Object fieldName) {

		/** noteCode = "DocumentNoteUtilities-Registration number"; */
		DocumentNoteType noteType = DocumentNoteUtilities.getNoteType(noteCode);
		// Object fieldValue = "";
		boolean truefalse = false;

		if (noteType != null) {
			AnnotatedPluginDocument.DocumentNotes documentNotes = annotatedPluginDocuments
					.getDocumentNotes(true);

			DocumentNote bos = documentNotes.getNote(noteCode);
			/** example: FieldName = "BasisOfRecordCode" */
			if (bos != null) {
				truefalse = true;
			} else {
				truefalse = false;
			}
		}
		return truefalse;
	}

	/**
	 * Get value from Notes from a selected Document file Return a string
	 * version value;
	 * 
	 * Used in LimsImportSamples and (Depricated)LimsReaddataFromSamples
	 * 
	 * @param annotatedPluginDocuments
	 *            , noteCode, fieldName, i
	 * @return
	 */
	public Object getVersionValueFromAnnotatedPluginDocument(
			AnnotatedPluginDocument[] annotatedPluginDocuments,
			String noteCode, Object fieldName, int i) {

		/** noteCode = "DocumentNoteUtilities-Registration number"; */
		DocumentNoteType noteType = DocumentNoteUtilities.getNoteType(noteCode);
		Object fieldValue = null;

		if (noteType != null) {
			AnnotatedPluginDocument.DocumentNotes documentNotes = annotatedPluginDocuments[i]
					.getDocumentNotes(true);

			DocumentNote bos = documentNotes.getNote(noteCode);
			/** example: FieldName = "BasisOfRecordCode" */
			if (bos != null) {
				fieldValue = bos.getFieldValue((String) fieldName);
			} else {
				fieldValue = null;
			}
		}
		return fieldValue;
	}

	// TODO: VERWIJDEREN
	/*
	 * Mag later worden verwijderd na opruiming van de volgende bestanden
	 * LimsReadDataFromBold and LimsCRSImporter
	 */
	public String getRegistrationNumberFromTableAnnotatedDocument(
			Object filename, String xmlNotesRegistration, String xmlNotesName)
			throws IOException {

		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		ssl = limsImporterUtil.getDatabasePropValues("ssl");
		url = limsImporterUtil.getDatabasePropValues("url");
		user = limsImporterUtil.getDatabasePropValues("user");
		password = limsImporterUtil.getDatabasePropValues("password");

		String result = "";

		try {

			final String SQL = " SELECT max(a.registrationnumber), a.name"
					+ " FROM " + " ( "
					+ " SELECT TRIM(EXTRACTVALUE(document_xml,  ' "
					+ xmlNotesRegistration + " ')) AS registrationnumber, "
					+ " TRIM(EXTRACTVALUE(document_xml,  ' " + xmlNotesName
					+ " ')) AS name " + " FROM annotated_document" + " ) AS a "
					+ " WHERE a.name =?";

			con = DriverManager.getConnection(url + activeDB + ssl, user,
					password);
			con.clearWarnings();
			pst = con.prepareStatement(SQL);
			pst.setString(1, (String) filename);
			rs = pst.executeQuery();
			while (rs.next()) {
				if (rs.getNString(1) != null) {
					result = rs.getObject(1).toString();
				} else {
					result = "no value";
				}
				logger.debug("Registrationnumber: " + result);
			}
		} catch (SQLException ex) {
			logger.info(ex.getMessage(), ex);
			exception = ex;

			EventQueue.invokeLater(new Runnable() {

				@Override
				public void run() {
					Dialogs.showMessageDialog("Get regsitrationnumber: "
							+ exception.getMessage());
				}
			});

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException ex) {
				logger.warn(ex.getMessage(), ex);
			}
		}
		return result;
	}

	// TODO: VERWIJDEREN
	/*
	 * Mag later worden verwijderd na opruiming van de volgende bestanden
	 * LimsReadDataFromBold and LimsReadDataFromSamples
	 */
	public String getFileNameFromGeneiousDatabase(String filename,
			String xmlnotes) {

		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			ssl = limsImporterUtil.getDatabasePropValues("ssl");
			url = limsImporterUtil.getDatabasePropValues("url");
			user = limsImporterUtil.getDatabasePropValues("user");
			password = limsImporterUtil.getDatabasePropValues("password");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String result = "";

		try {

			final String SQL = " SELECT a.name" + " FROM " + " ( "
					+ " SELECT	TRIM(EXTRACTVALUE(plugin_document_xml, ' "
					+ xmlnotes + " ' )) AS name " + " FROM annotated_document"
					+ " ) AS a " + " WHERE a.name =?";

			con = DriverManager.getConnection(url + activeDB + ssl, user,
					password);
			con.clearWarnings();
			pst = con.prepareStatement(SQL);
			pst.setString(1, filename);
			rs = pst.executeQuery();
			while (rs.next()) {
				result = rs.getObject(1).toString();
				logger.debug("Filename: " + result
						+ " already exists in the geneious database.");
				limsLogList.UitvalList.add("Filename: " + result
						+ " already exists in the geneious database." + "\n");
			}
		} catch (SQLException ex) {
			logger.info(ex.getMessage(), ex);
			logger.info("Url maybe not correct: " + url);
			exception = ex;

			EventQueue.invokeLater(new Runnable() {

				@Override
				public void run() {
					Dialogs.showMessageDialog("Get filename from database: "
							+ exception.getMessage() + "\n" + url
							+ " settings is not correct");
				}
			});

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException ex) {
				logger.warn(ex.getMessage(), ex);
			}
		}
		return result;
	}

	private HashSet<String> fastaAb1Cache = new HashSet<>(100);

	/**
	 * Check if Fasts filename exists in the Database Used in LimsImportAB1 and
	 * LimsImportAB1Update
	 * 
	 * @param fileName
	 *            , fieldName, xnlnotes
	 * @return
	 * */
	public boolean checkOfFastaOrAB1Exists(String fileName, String fieldName,
			String xmlnotes) {

		if (fastaAb1Cache.contains(fileName + '|' + fieldName))
			return true;

		boolean truefalse = false;
		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			ssl = limsImporterUtil.getDatabasePropValues("ssl");
			url = limsImporterUtil.getDatabasePropValues("url");
			user = limsImporterUtil.getDatabasePropValues("user");
			password = limsImporterUtil.getDatabasePropValues("password");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String result = "";

		try {

			final String SQL = " SELECT a.name" + " FROM " + " ( "
					+ " SELECT	TRIM(EXTRACTVALUE(" + fieldName + ", ' "
					+ xmlnotes + " ')) AS name " + " FROM annotated_document"
					+ " ) AS a " + " WHERE a.name =?";

			con = DriverManager.getConnection(url + activeDB + ssl, user,
					password);
			con.clearWarnings();
			pst = con.prepareStatement(SQL);
			pst.setString(1, fileName);
			rs = pst.executeQuery();
			while (rs.next()) {
				result = rs.getObject(1).toString();
				if (rs.wasNull())
					truefalse = false;
				else
					truefalse = true;

				logger.debug("Filename: " + result
						+ " already exists in the geneious database.");
				limsLogList.UitvalList.add("Filename: " + result
						+ " already exists in the geneious database." + "\n");
			}
		} catch (SQLException ex) {
			logger.info(ex.getMessage(), ex);
			exception = ex;

			EventQueue.invokeLater(new Runnable() {

				@Override
				public void run() {
					Dialogs.showMessageDialog("Check of fasta/Ab1 file exists: "
							+ exception.getMessage());
				}
			});

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException ex) {
				logger.warn(ex.getMessage(), ex);
			}
		}
		if (truefalse)
			fastaAb1Cache.add(fileName + '|' + fieldName);
		return truefalse;
	}

	/**
	 * Check if AB1 filename exists in the Database Used in LimsImportAB1 and
	 * LimsImportAB1Update
	 * 
	 * @param filename
	 * @return
	 * */
	public boolean fileNameExistsInGeneiousDatabase(String filename) {

		boolean truefalse = false;
		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			ssl = limsImporterUtil.getDatabasePropValues("ssl");
			url = limsImporterUtil.getDatabasePropValues("url");
			user = limsImporterUtil.getDatabasePropValues("user");
			password = limsImporterUtil.getDatabasePropValues("password");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String result = "";

		try {

			final String SQL = " SELECT a.name, count(a.name) as count"
					+ " FROM "
					+ " ( "
					+ " SELECT	TRIM(EXTRACTVALUE(plugin_document_xml, '//ABIDocument/name')) AS name "
					+ " FROM annotated_document" + " ) AS a "
					+ " WHERE a.name = ?";

			con = DriverManager.getConnection(url + activeDB + ssl, user,
					password);
			con.clearWarnings();
			pst = con.prepareStatement(SQL);
			pst.setString(1, filename);
			rs = pst.executeQuery();
			while (rs.next()) {
				result = rs.getString("name");
				recordcount = rs.getInt("count");

				if (rs.wasNull())
					truefalse = false;
				else
					truefalse = true;

				logger.debug("Filename: " + result
						+ " already exists in the geneious database.");
				limsLogList.UitvalList.add("Filename: " + result
						+ " already exists in the geneious database." + "\n");
			}
		} catch (SQLException ex) {
			logger.info(ex.getMessage(), ex);
			exception = ex;

			EventQueue.invokeLater(new Runnable() {

				@Override
				public void run() {
					Dialogs.showMessageDialog("SQL Exception Information: "
							+ exception.getMessage());
				}
			});

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
				if (con != null) {
					con.close();
				}
			} catch (SQLException ex) {
				logger.warn(ex.getMessage(), ex);
			}
		}
		return truefalse;
	}

	/**
	 * Get Name from Samples files in the database Used LimsImportSamples and
	 * LimsReadDataFromSamples
	 * 
	 * @param extractid
	 * @return
	 * */
	public String getFastaIDForSamples_GeneiousDB(String extractid)
			throws IOException {

		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		ssl = limsImporterUtil.getDatabasePropValues("ssl");
		url = limsImporterUtil.getDatabasePropValues("url");
		user = limsImporterUtil.getDatabasePropValues("user");
		password = limsImporterUtil.getDatabasePropValues("password");

		String result = "";
		try {

			final String SQL = " SELECT a.name"
					+ " FROM "
					+ " ( "
					+ " SELECT	TRIM(EXTRACTVALUE(plugin_document_xml, '//XMLSerialisableRootElement/name')) AS name "
					+ " FROM annotated_document" + " ) AS a "
					+ " WHERE a.name like ?";

			con = DriverManager.getConnection(url + activeDB + ssl, user,
					password);
			con.clearWarnings();
			pst = con.prepareStatement(SQL);
			pst.setString(1, "%" + extractid + "%");
			rs = pst.executeQuery();
			while (rs.next()) {
				result = rs.getObject(1).toString();
				logger.info("Extract ID: " + result
						+ " already exsist in the geneious database.");
			}

		} catch (SQLException ex) {
			logger.info(ex.getMessage(), ex);

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException ex) {
				logger.warn(ex.getMessage(), ex);
			}
		}
		return result;
	}

	/**
	 * Get Cache name from XML field document_xml table annotated_document
	 * //document/hiddenFields/override_cache_name Used in LimsImportAB1 and
	 * LimsReadDataFromBold
	 * */
	public String getCacheNameFromGeneiousDatabase(Object filename,
			String xmlNotesName) throws IOException {

		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		ssl = limsImporterUtil.getDatabasePropValues("ssl");
		url = limsImporterUtil.getDatabasePropValues("url");
		user = limsImporterUtil.getDatabasePropValues("user");
		password = limsImporterUtil.getDatabasePropValues("password");

		String result = "";

		try {

			final String SQL = " SELECT a.name" + " FROM " + " ( "
					+ " SELECT	TRIM(EXTRACTVALUE(document_xml,  ' "
					+ xmlNotesName + " ')) AS name "
					+ " FROM annotated_document" + " ) AS a "
					+ " WHERE a.name =?";
			try {
				con = DriverManager.getConnection(url + activeDB + ssl, user,
						password);
				// con.clearWarnings();
			} catch (SQLException e) {
				throw new IllegalStateException("Cannot connect the database!",
						e);
			}
			pst = con.prepareStatement(SQL);
			pst.setString(1, (String) filename);
			rs = pst.executeQuery();
			while (rs.next()) {
				result = rs.getObject(1).toString();
				logger.debug("Filename: " + result
						+ " already exists in the geneious database.");
				limsLogList.UitvalList.add("Filename: " + result
						+ " already exists in the geneious database." + "\n");
			}
		} catch (SQLException ex) {
			logger.info(ex.getMessage(), ex);
			exception = ex;

			EventQueue.invokeLater(new Runnable() {

				@Override
				public void run() {
					Dialogs.showMessageDialog("Get cachename from DB: "
							+ exception.getMessage());
				}
			});

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException ex) {
				logger.warn(ex.getMessage(), ex);
			}
		}
		return result;
	}

	/**
	 * Get the ID and Name from a file(Parameter fileName) in the Database Used
	 * in LimsImportAB1
	 * 
	 * @param filename
	 *            , xmlNotesName
	 * @return
	 * */
	public String getIDFromTableAnnotatedDocument(Object filename,
			String xmlNotesName) throws IOException {

		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		ssl = limsImporterUtil.getDatabasePropValues("ssl");
		url = limsImporterUtil.getDatabasePropValues("url");
		user = limsImporterUtil.getDatabasePropValues("user");
		password = limsImporterUtil.getDatabasePropValues("password");

		String result = "";

		try {

			final String SQL = " SELECT a.id, a.name" + " FROM " + " ( "
					+ " SELECT id as ID, TRIM(EXTRACTVALUE(document_xml,  ' "
					+ xmlNotesName + " ')) AS name "
					+ " FROM annotated_document" + " ) AS a "
					+ " WHERE a.name =?";

			con = DriverManager.getConnection(url + activeDB + ssl, user,
					password);
			con.clearWarnings();
			pst = con.prepareStatement(SQL);
			pst.setString(1, (String) filename);
			rs = pst.executeQuery();
			while (rs.next()) {
				result = rs.getObject(1).toString();
				logger.debug("Annotated document id : " + result);
			}
		} catch (SQLException ex) {
			logger.info(ex.getMessage(), ex);
			exception = ex;

			EventQueue.invokeLater(new Runnable() {

				@Override
				public void run() {
					Dialogs.showMessageDialog("Get ID from annotateddocument table: "
							+ exception.getMessage());
				}
			});

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException ex) {
				logger.warn(ex.getMessage(), ex);
			}
		}
		return result;
	}

	/**
	 * Delete Dummy documents when Import file match Dummy filename Used in
	 * LimsImportAB1
	 * 
	 * @param ID
	 * @throws IOException
	 * */
	public void DeleteDummyRecordFromTableAnnotatedtDocument(Object ID)
			throws IOException {

		Connection con = null;
		PreparedStatement pst = null;

		ssl = limsImporterUtil.getDatabasePropValues("ssl");
		url = limsImporterUtil.getDatabasePropValues("url");
		user = limsImporterUtil.getDatabasePropValues("user");
		password = limsImporterUtil.getDatabasePropValues("password");

		try {

			final String SQL = "DELETE FROM annotated_document"
					+ System.lineSeparator() + "WHERE id =?";

			try {
				con = DriverManager.getConnection(url + activeDB + ssl, user,
						password);
				// con.clearWarnings();
			} catch (SQLException e) {
				throw new IllegalStateException("Cannot connect the database!",
						e);
			}
			pst = con.prepareStatement(SQL);
			pst.setString(1, (String) ID);
			pst.executeUpdate();
			logger.debug("Delete Dummy Annotated document id : " + ID
					+ "From tabel annotated_document ");
		} catch (SQLException ex) {
			logger.info(ex.getMessage(), ex);
			exception = ex;

			EventQueue.invokeLater(new Runnable() {

				@Override
				public void run() {
					Dialogs.showMessageDialog("Delete dummy: "
							+ exception.getMessage());
				}
			});

		} finally {
			try {
				if (pst != null) {
					pst.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException ex) {
				logger.warn(ex.getMessage(), ex);
			}
		}
	}

	/**
	 * Used in LimsImportAB1Update Get Version for FAsta and AB1 files
	 * 
	 * @param fileName
	 * */
	public int getLastVersion_For_AB1_Fasta(String fileName) {

		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			ssl = limsImporterUtil.getDatabasePropValues("ssl");
			url = limsImporterUtil.getDatabasePropValues("url");
			user = limsImporterUtil.getDatabasePropValues("user");
			password = limsImporterUtil.getDatabasePropValues("password");
		} catch (IOException e) {
			e.printStackTrace();
		}

		int result = 0;

		try {

			final String SQL = " SELECT MAX(CAST(a.version as UNSIGNED)) as version, a.name, a.reference_count "
					+ " FROM "
					+ " ( "
					+ " SELECT	TRIM(EXTRACTVALUE(document_xml, '//document/notes/note/DocumentVersionCode_Seq')) AS version, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/hiddenFields/cache_name')) AS name, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/hiddenFields/strong_referenced_documents/strong_referenced_documents')) AS reference_count "
					+ " FROM annotated_document) AS a "
					+ " WHERE a.name =?"
					+ " AND   a.reference_count = 0 " + " AND   a.version > 0";
			con = DriverManager.getConnection(url + activeDB + ssl, user,
					password);
			con.clearWarnings();
			pst = con.prepareStatement(SQL);
			pst.setString(1, (String) fileName);
			rs = pst.executeQuery();
			if (rs.next()) {
				do {
					result = rs.getInt(1);
					logger.debug("Versionnumber : " + result);
				} while (rs.next());
			}
		} catch (SQLException ex) {
			logger.info(ex.getMessage(), ex);
			exception = ex;

			EventQueue.invokeLater(new Runnable() {

				@Override
				public void run() {
					Dialogs.showMessageDialog("Get last version: "
							+ exception.getMessage());
				}
			});
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
				if (con != null) {
					con.close();
				}
			} catch (SQLException ex) {
				logger.warn(ex.getMessage(), ex);
			}
		}
		return result;
	}

	/**
	 * Used in LimsImportAB1Update
	 * 
	 * @param fileName
	 * @return result
	 */
	public int getLastVersionFromDocument(String fileName) {

		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			ssl = limsImporterUtil.getDatabasePropValues("ssl");
			url = limsImporterUtil.getDatabasePropValues("url");
			user = limsImporterUtil.getDatabasePropValues("user");
			password = limsImporterUtil.getDatabasePropValues("password");
		} catch (IOException e) {
			e.printStackTrace();
		}

		int result = 0;

		try {

			final String SQL = " SELECT MAX(CAST(a.version as UNSIGNED)) as version, a.name  "
					+ " FROM "
					+ " ( "
					+ " SELECT	TRIM(EXTRACTVALUE(document_xml, '//document/notes/note/DocumentVersionCode_Seq')) AS version, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/hiddenFields/cache_name')) AS name "
					+ " FROM annotated_document) AS a " + " WHERE a.name =?";
			con = DriverManager.getConnection(url + activeDB + ssl, user,
					password);
			con.clearWarnings();
			pst = con.prepareStatement(SQL);
			pst.setString(1, (String) fileName);
			rs = pst.executeQuery();
			if (rs.next()) {
				do {
					result = rs.getInt(1);
					logger.debug("Versionnumber : " + result);
				} while (rs.next());
			}
		} catch (SQLException ex) {
			logger.info(ex.getMessage(), ex);
			exception = ex;

			EventQueue.invokeLater(new Runnable() {

				@Override
				public void run() {
					Dialogs.showMessageDialog("Get last version: "
							+ exception.getMessage());
				}
			});

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException ex) {
				logger.warn(ex.getMessage(), ex);
			}
		}
		return result;
	}

	public String getDummyPcrPlateIdSeqValue() {
		return dummyPcrPlateIdSeqValue;
	}

	public void setDummyPcrPlateIdSeqValue(String dummyPcrPlateIdSeqValue) {
		this.dummyPcrPlateIdSeqValue = dummyPcrPlateIdSeqValue;
	}

	public String getDummyMarkerSeqValue() {
		return dummyMarkerSeqValue;
	}

	public void setDummyMarkerSeqValue(String dummyMarkerSeqValue) {
		this.dummyMarkerSeqValue = dummyMarkerSeqValue;
	}

	public String getDummyRegistrNmbrSamplesValue() {
		return dummyRegistrNmbrSamplesValue;
	}

	public void setDummyRegistrNmbrSamplesValue(
			String dummyRegistrNmbrSamplesValue) {
		this.dummyRegistrNmbrSamplesValue = dummyRegistrNmbrSamplesValue;
	}

	public String getDummyScientificNameSamplesValue() {
		return dummyScientificNameSamplesValue;
	}

	public void setDummyScientificNameSamplesValue(
			String dummyScientificNameSamplesValue) {
		this.dummyScientificNameSamplesValue = dummyScientificNameSamplesValue;
	}

	public String getDummySamplePlateIdSamplesValue() {
		return dummySamplePlateIdSamplesValue;
	}

	public void setDummySamplePlateIdSamplesValue(
			String dummySamplePlateIdSamplesValue) {
		this.dummySamplePlateIdSamplesValue = dummySamplePlateIdSamplesValue;
	}

	public String getDummyPositionSamplesValue() {
		return dummyPositionSamplesValue;
	}

	public void setDummyPositionSamplesValue(String dummyPositionSamplesValue) {
		this.dummyPositionSamplesValue = dummyPositionSamplesValue;
	}

	public String getDummyExtractIDSamplesValue() {
		return dummyExtractIDSamplesValue;
	}

	public void setDummyExtractIDSamplesValue(String dummyExtractIDSamplesValue) {
		this.dummyExtractIDSamplesValue = dummyExtractIDSamplesValue;
	}

	public String getDummySeqStaffSamplesValue() {
		return dummySeqStaffSamplesValue;
	}

	public void setDummySeqStaffSamplesValue(String dummySeqStaffSamplesValue) {
		this.dummySeqStaffSamplesValue = dummySeqStaffSamplesValue;
	}

	public List<String> getDummySamplesValues(Object filename) {

		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			ssl = limsImporterUtil.getDatabasePropValues("ssl");
			url = limsImporterUtil.getDatabasePropValues("url");
			user = limsImporterUtil.getDatabasePropValues("user");
			password = limsImporterUtil.getDatabasePropValues("password");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String result = "";

		try {

			final String SQL = " SELECT a.name, a.pcrplateid, a.marker, a.Registrationnumber, a.ScientificName, "
					+ " a.SamplePlateId, a.Position, a.ExtractID, a.Seqstaff, a.extractPlateNumberIDSamples, a.extractMethod, a.registrationScientificName "
					+ " FROM( "
					+ " SELECT "
					+ " TRIM(EXTRACTVALUE(document_xml,  '//document/hiddenFields/cache_name')) AS name, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/notes/note/PCRplateIDCode_Seq')) AS pcrplateid, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/notes/note/MarkerCode_Seq')) AS marker, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/notes/note/RegistrationNumberCode_Samples')) AS registrationnumber, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/notes/note/TaxonName2Code_Samples')) AS scientificName, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/notes/note/ProjectPlateNumberCode_Samples')) AS samplePlateId, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/notes/note/PlatePositionCode_Samples')) AS position, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/notes/note/ExtractIDCode_Samples')) AS extractID, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/notes/note/SequencingStaffCode_FixedValue_Samples')) AS seqStaff, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/notes/note/ExtractPlateNumberCode_Samples')) AS extractPlateNumberIDSamples, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/notes/note/SampleMethodCode_Samples')) AS extractMethod, "
					+ " TRIM(EXTRACTVALUE(document_xml, '//document/notes/note/RegistrationNumberCode_TaxonName2Code_Samples')) AS registrationScientificName "
					+ " FROM annotated_document) AS a " + " WHERE a.name =?";

			con = DriverManager.getConnection(url + activeDB + ssl, user,
					password);
			con.clearWarnings();
			pst = con.prepareStatement(SQL);
			pst.setString(1, (String) filename);
			rs = pst.executeQuery();
			ResultSetMetaData metadata = rs.getMetaData();
			int numberOfColumns = metadata.getColumnCount();
			listDummyValues.clear();
			while (rs.next()) {
				registrnmbrSamplesFromDummy = rs
						.getString("Registrationnumber");
				scientificNameSamplesFromDummy = rs.getString("ScientificName");
				samplePlateIdSamplesFromDummy = rs.getString("SamplePlateId");
				positionSamplesFromDummy = rs.getString("Position");
				extractidSamplesFromDummy = rs.getString("ExtractID");
				extractPlateIDSamples = rs
						.getString("extractPlateNumberIDSamples");
				extractionMethodSamples = rs.getString("extractMethod");
				registrationScientificName = rs
						.getString("registrationScientificName");
				int i = 1;
				while (i <= numberOfColumns) {
					listDummyValues.add(rs.getString(i++));
					logger.debug("Annotated document id : " + result);
				}
			}
		} catch (SQLException ex) {
			logger.info(ex.getMessage(), ex);
			exception = ex;

			EventQueue.invokeLater(new Runnable() {

				@Override
				public void run() {
					Dialogs.showMessageDialog("Get samples Value: "
							+ exception.getMessage());
				}
			});

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException ex) {
				logger.warn(ex.getMessage(), ex);
			}
		}
		return listDummyValues;
	}

	/**
	 * Get database name
	 * 
	 * @return
	 * */
	public String getServerDatabaseServiceName() {
		List<String> lstdb = new ArrayList<String>();

		String output = "";
		String dbResult = "";

		ListIterator<?> itr = PluginUtilities.getWritableDatabaseServiceRoots()
				.listIterator();

		while (itr.hasNext()) {
			String dbsvc = itr.next().toString();
			if (dbsvc.contains("geneious")) {
				String st[] = dbsvc.split("name=");
				Map<String, Integer> mp = new TreeMap<String, Integer>();
				for (int i = 0; i < st.length; i++) {
					Integer count = mp.get(st[i]);
					if (count == null) {
						count = 0;
					}
					mp.put(st[i], ++count);
					String strGeneious = st[i];

					if (strGeneious.contains("geneious")) {
						int indexEnd = strGeneious.indexOf(",");
						output = strGeneious.substring(0, indexEnd);
						lstdb.add(output);
					}
				}
			}
		}

		Iterator<String> lstitr = lstdb.listIterator();
		while (lstitr.hasNext()) {
			dbResult = lstitr.next().toString();
		}
		return dbResult; // databaseName[0];
	}

	public static <T> List<T> searchIn(List<T> list, Matcher<T> m) {
		List<T> r = new ArrayList<T>();
		for (T t : list) {
			if (m.matches(t)) {
				r.add(t);
			}
		}
		return r;
	}

	/**
	 * Get database name
	 * 
	 * @return
	 * */
	public String checkIfLocalOrServerDatabase(String databaseURL) {
		List<String> lstdb = new ArrayList<String>();

		String output = "";
		String urlResult = "";

		ListIterator<WritableDatabaseService> itr = PluginUtilities
				.getWritableDatabaseServiceRoots().listIterator();

		while (itr.hasNext()) {
			WritableDatabaseService dbsvc = itr.next().getDatabaseRoot();
			BeanPrinter bp = new BeanPrinter("C:/temp/bp.txt");
			bp.dump(dbsvc);

			System.out.println("Item: " + (dbsvc instanceof Collection));
			break;
			/*
			 * itr.nextIndex(); if (dbsvc.toString().contains(databaseURL)) {
			 * String st[] = dbsvc.toString().split("@"); Map<String, Integer>
			 * mp = new TreeMap<String, Integer>(); for (int i = 0; i <
			 * st.length; i++) { Integer count = mp.get(st[i]); if (count ==
			 * null) { count = 0; } mp.put(st[i], ++count); String strGeneious =
			 * st[i];
			 * 
			 * if (strGeneious.contains(databaseURL)) { int indexEnd =
			 * strGeneious.indexOf("@"); output = strGeneious.substring(0,
			 * indexEnd); lstdb.add(output); } } }
			 */

		}

		Iterator<String> lstitr = lstdb.listIterator();
		while (lstitr.hasNext()) {
			urlResult = lstitr.next().toString();
		}
		return urlResult; // URL[0];
	}
}
