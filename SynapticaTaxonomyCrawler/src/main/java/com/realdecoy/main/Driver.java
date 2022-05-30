package com.realdecoy.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.realdecoy.model.TaxonomyItem;
import com.realdecoy.util.IdUtil;
import com.realdecoy.util.TimeUtils;

public class Driver {

	private static final Logger LOGGER = Logger.getLogger(Driver.class.getName());
	private static final int MAX_ARCHIVE_FILES = 5;
	private static final String REGEX_PATTERN = "([FILE_NAME]){1}(.backup.[0-9]{4}-[0-9]{2}-[0-9]{2}.[0-9]{2}-[0-9]{2}-[0-9]{2})*";
	private static final IdUtil idUtil = new IdUtil();

	public static void main(String... args) {

		String user = "", password = "", url = "", outputFolderPath = "", fileName = "", dimensionName = "";

		if (args.length != 12) {
			LOGGER.severe("Invalid number of params supplied.");
			LOGGER.info("Usage: -u user -p password -url url -o outputFolderPath -f fileName -d dimensionName");
			LOGGER.info("Aborting");
			System.exit(1);
		}

		if (!args[0].equals("-u")) {
			LOGGER.severe("Unknown parameter found: " + args[0]);
			LOGGER.info("Aborting");
			System.exit(1);
		} else {
			user = args[1];
		}

		if (!args[2].equals("-p")) {
			LOGGER.severe("Unknown parameter found: " + args[2]);
			LOGGER.info("Aborting");
			System.exit(1);
		} else {
			password = args[3];
		}

		if (!args[4].equals("-url")) {
			LOGGER.severe("Unknown parameter found: " + args[4]);
			LOGGER.info("Aborting");
			System.exit(1);
		} else {
			url = args[5];
		}

		if (!args[6].equals("-o")) {
			LOGGER.severe("Unknown parameter found: " + args[6]);
			LOGGER.info("Aborting");
			System.exit(1);
		} else {
			outputFolderPath = args[7];
			if (!new File(outputFolderPath).exists()) {
				LOGGER.severe("Output folder path: " + outputFolderPath + " does not exist.");
				LOGGER.info("Aborting");
				System.exit(1);
			}

			if (!new File(outputFolderPath).isDirectory()) {
				LOGGER.severe("Output folder path: " + outputFolderPath + " is not a directory.");
				LOGGER.info("Aborting");
				System.exit(1);
			}
		}

		if (!args[8].equals("-f")) {
			LOGGER.severe("Unknown parameter found: " + args[8]);
			LOGGER.info("Aborting");
			System.exit(1);
		} else {
			fileName = args[9];
		}
		
		if (!args[10].equals("-d")) {
			LOGGER.severe("Unknown parameter found: " + args[10]);
			LOGGER.info("Aborting");
			System.exit(1);
		} else {
			dimensionName = args[11];
		}

		LOGGER.info("Initiating Synaptica Taxonomy Crawler process.");
		long startTime = System.currentTimeMillis();

		LOGGER.info("Retrieving taxonomy data.");
		ArrayList<TaxonomyItem> taxonomyItems = getTaxonomyData(url, user, password);
		LOGGER.info("Taxonomy data retrieval complete.");

		if (taxonomyItems.size() > 0) {
			LOGGER.info("Parsing taxonomy data.");
			Document document = parseTaxonomyData(dimensionName, taxonomyItems);
			LOGGER.info("Taxonomy data parsing complete.");
			if (document != null) {
				LOGGER.info("Starting data archival step.");
				archiveExistingFile(outputFolderPath, fileName);
				LOGGER.info("Data archival step complete.");
				LOGGER.info("Writing new dimension file.");
				writeNewDimensionFile(document, outputFolderPath, fileName);
				LOGGER.info("New dimension file write complete.");
			} else {
				LOGGER.warning("New dimension data not generated, aborting.");
			}
		} else {
			LOGGER.warning("No taxonomy data retrieved, aborting.");
		}

		long endTime = System.currentTimeMillis();
		LOGGER.info("Synaptica Taxonomy Crawler process complete.");
		TimeUtils.displayElapsedTime(startTime, endTime, LOGGER);
		System.exit(0);
	}

	private static void archiveExistingFile(String outputFolderPath, String fileName) {
		File directory = new File(outputFolderPath);
		File dimensionFile = new File(directory.getAbsolutePath().concat(File.separator).concat(fileName));

		if (dimensionFile.exists()) {
			try {
				Date date = new Date();
			    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd.HH-mm-ss");
				String newFileName = fileName.concat(".backup.").concat(simpleDateFormat.format(date));
				File archiveFile = new File(directory.getAbsolutePath().concat(File.separator).concat(newFileName));
				dimensionFile.renameTo(archiveFile);
				LOGGER.info("Starting data archive clean up step.");
				cleanUpArchive(outputFolderPath, fileName);
				LOGGER.info("Archive clean up step complete.");
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to archive existing file.", e);
				System.exit(1);
			}
		} else {
			LOGGER.info("No existing file to archive, skipping step.");
		}
	}

	private static void cleanUpArchive(String outputFolderPath, final String fileName) {
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File directory, String name) {
				return name.matches(REGEX_PATTERN.replace("[FILE_NAME]", fileName));
			}
		};

		File directory = new File(outputFolderPath);
		File[] files = directory.listFiles(filter);

		if (files != null && files.length > 0) {
			if (files.length <= MAX_ARCHIVE_FILES) {
				LOGGER.info("Maximum archive file theshold not reached. Skipping step.");
			} else {
				// Sorts files by last modified date.
				Arrays.sort(files, new Comparator<File>() {
					public int compare(File f1, File f2) {
						return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
					}
				});

				int numberOfFilesToDelete = files.length - MAX_ARCHIVE_FILES;

				for (int i = 0; i < numberOfFilesToDelete; i++) {
					File file = files[i];
					try {
						LOGGER.info("Deleting file: " + file.getName() + ". Reason: Max archive file threshold reached.");
						file.delete();
						LOGGER.info("File Deleted.");
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Failed to delete file: " + file.getName() + ".", e);
					}
				}
			}
		}
	}

	private static ArrayList<TaxonomyItem> getTaxonomyData(String urlString, String username, String password) {
		ArrayList<TaxonomyItem> taxonomyItems = null;
		Gson gson = new Gson();

		try {
			String authenticationString = username + ":" + password;
			String encodedAuthenticationString = DatatypeConverter.printBase64Binary(authenticationString.getBytes("UTF-8"));
			URL url = new URL(urlString);
			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Authorization", "Basic " + encodedAuthenticationString);
			InputStream inputStream = urlConnection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));

			Type listType = new TypeToken<ArrayList<TaxonomyItem>>() {
			}.getType();
			taxonomyItems = gson.fromJson(readAll(reader), listType);

			reader.close();
			inputStream.close();
		} catch (MalformedURLException e) {
			LOGGER.log(Level.SEVERE, "Error occured whilst retrieving taxonomy data. URL: " + urlString + ".", e);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error occured whilst retrieving taxonomy data. URL: " + urlString + ".", e);
		}

		return taxonomyItems;
	}

	private static Document parseTaxonomyData(String dimensionName, ArrayList<TaxonomyItem> taxonomyItems) {
		Document document = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.newDocument();

			Element externalDimensions = document.createElement("external_dimensions");
			document.appendChild(externalDimensions);

			Element root = document.createElement("node");
			root.setAttribute("id", dimensionName);
			root.setAttribute("name", dimensionName);
			externalDimensions.appendChild(root);

			processTaxonomyItems(taxonomyItems, document, externalDimensions, dimensionName, null);

		} catch (ParserConfigurationException e) {
			LOGGER.log(Level.SEVERE, "An error occured whislt parsing the taxonomy data.", e);
		}

		return document;
	}

	private static void processLevel(Document document, Element externalDimensions, TaxonomyItem taxonomyItem, String directParent, String uniquePath) {
		Element node = document.createElement("node");
		Element synonym = document.createElement("synonym");
		Element property = document.createElement("property");
		//Set the Synaptica ID Term
		Element propertySyn = document.createElement("property");

		node.setAttribute("parent", directParent);
		node.setAttribute("classify", "false");
		node.setAttribute("search", "true");
		// Enables the Synaptica Term ID to be visible
		

		// Change to generated ID in future
		// node.setAttribute("id", taxonomyItem.getUid());
		node.setAttribute("id", taxonomyItem.getGeneratedUid());

		node.setAttribute("name", taxonomyItem.getName());

		synonym.setAttribute("search", "false");
		synonym.setAttribute("name", taxonomyItem.getUid());
		synonym.setAttribute("classify", "true");

		property.setAttribute("name", "UNIQUE_PATH");

		propertySyn.setAttribute("name", "SID");
		propertySyn.setTextContent(taxonomyItem.getUid());
		

		if (uniquePath == null) {
			// Should be same as id, change to generated value in future
//			uniquePath = taxonomyItem.getUid();
			uniquePath = taxonomyItem.getGeneratedUid();
		} else {
			// Should be same as id, change to generated value in future
//			uniquePath = uniquePath.concat(",").concat(taxonomyItem.getUid());
			uniquePath = uniquePath.concat(",").concat(taxonomyItem.getGeneratedUid());
			
		}
		property.setTextContent(uniquePath);

		node.appendChild(synonym);
		node.appendChild(property);
		node.appendChild(propertySyn);

		externalDimensions.appendChild(node);
	}

	private static void processTaxonomyItems(ArrayList<TaxonomyItem> taxonomyItems, Document document, Element externalDimensions, String directParent, String uniquePath) {

		for (TaxonomyItem taxonomyItem : taxonomyItems) {
			
			taxonomyItem.setGeneratedUid(idUtil.getId());

			// Process data for the current taxonomy item
			processLevel(document, externalDimensions, taxonomyItem, directParent, uniquePath);

			if (taxonomyItem.hasRelatedTerms()) {
				// Build updated unique path to pass on to related terms.
				String newUniquePath;
				if (uniquePath == null) {
					newUniquePath = taxonomyItem.getGeneratedUid();
				} else {
					newUniquePath = uniquePath.concat(",").concat(taxonomyItem.getGeneratedUid());
				}

				// Process the list of related terms recursively.
				processTaxonomyItems(taxonomyItem.getRelatedTerms(), document, externalDimensions, taxonomyItem.getGeneratedUid(), newUniquePath);
			}
		}

	}

	private static String readAll(BufferedReader reader) {
		StringBuilder builder = new StringBuilder();
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error occured whilst reading input stream.", e);
		}
		return builder.toString();
	}

	private static void rollback(String outputFolderPath, final String fileName) {
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File directory, String name) {
				return name.matches(REGEX_PATTERN.replace("[FILE_NAME]", fileName));
			}
		};

		File directory = new File(outputFolderPath);
		File[] files = directory.listFiles(filter);

		if (files != null && files.length > 0) {
			// Sorts files by last modified date.
			Arrays.sort(files, new Comparator<File>() {
				public int compare(File f1, File f2) {
					return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
				}
			});

			try {
				File lastArchiveFile = files[files.length - 1];
				LOGGER.info("Attempting to rollback to: " + lastArchiveFile.getName());
				File rolledbackFile = new File(directory.getAbsolutePath().concat(File.separator).concat(fileName));
				lastArchiveFile.renameTo(rolledbackFile);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to rollback file from archive.", e);
				System.exit(1);
			}
		} else {
			LOGGER.warning("No archive files found to rollback to.");
		}
	}

	private static void writeNewDimensionFile(Document document, String outputFolderPath, String fileName) {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(document);
			File directory = new File(outputFolderPath);
			File outputFile = new File(directory.getAbsolutePath().concat(File.separator).concat(fileName));
			StreamResult result = new StreamResult(outputFile);
			transformer.transform(source, result);
		} catch (TransformerException e) {
			LOGGER.log(Level.SEVERE, "Failed to write new dimension file.", e);
			LOGGER.info("Initiating rollback step.");
			rollback(outputFolderPath, fileName);
			LOGGER.info("Rollback step complete.");
		}
	}

}
