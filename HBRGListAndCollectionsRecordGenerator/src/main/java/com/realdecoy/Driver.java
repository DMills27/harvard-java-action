package com.realdecoy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Driver {

	private static final String CASE_COLLECTION_FOLDER = "CaseCollections";
	private static final String COLUMN_DELIMITER = "||";
	private static final String CONTENT_FILE_NAME = "_.json";
	private static final Logger LOGGER = Logger.getLogger(Driver.class.getName());
	private static final String READING_LIST_FOLDER = "ReadingList";
	private static final String RECORD_DELIMITER = "REC";

	public static void main(String[] args) {
		LOGGER.info("Initiating process.");

		ArrayList<JSONObject> contentItems = new ArrayList<>();
		File contentSource = null, destination = null;
		String contentSourcePath = "", destinationPath = "", filename = "";

		LOGGER.info("Parsing parameters.");

		try {

			if (args.length != 6) {
				LOGGER.severe("Invalid number of params supplied.");
				System.out.println("Usage: -c contentSourcePath -d destinationPath -f filename");
				System.exit(1);
			}

			if (!args[0].equals("-c")) {
				LOGGER.severe("Unknown parameter found: " + args[0]);
				System.exit(1);
			} else {
				contentSourcePath = args[1];
				contentSource = new File(contentSourcePath);

				if (!contentSource.exists()) {
					LOGGER.severe("Content Source Path Path does not exist.");
					System.exit(1);
				}

				if (!contentSource.isDirectory()) {
					LOGGER.severe("Content Source Path Path is not a directory.");
					System.exit(1);
				}
			}

			if (!args[2].equals("-d")) {
				LOGGER.severe("Unknown parameter found: " + args[2]);
				System.exit(1);
			} else {
				destinationPath = args[3];
				destination = new File(destinationPath);

				if (!destination.exists()) {
					LOGGER.severe("Destination Path does not exist.");
					System.exit(1);
				}

				if (!destination.isDirectory()) {
					LOGGER.severe("Destination Path is not a directory.");
					System.exit(1);
				}
			}

			if (!args[4].equals("-f")) {
				LOGGER.severe("Unknown parameter found: " + args[4]);
				System.exit(1);
			} else {
				filename = args[5];
			}

			processContentDirectory(contentItems, new File(contentSource.getAbsolutePath() + File.separator + CASE_COLLECTION_FOLDER));
			processContentDirectory(contentItems, new File(contentSource.getAbsolutePath() + File.separator + READING_LIST_FOLDER));

			if (contentItems.size() > 0) {
				processAndWriteJsonData(contentItems, destination, filename);
			}

			LOGGER.info("Process complete.");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "An unexpected error occurred.", e);
		}

	}

	private static void parseContentItem(ArrayList<JSONObject> contentItems, File contentItemFolder) {
		LOGGER.info("Parsing content item: " + contentItemFolder.getName() + ".");

		File content = new File(contentItemFolder.getAbsolutePath() + File.separator + CONTENT_FILE_NAME);
		JSONObject jsonObject = new JSONObject();
		StringBuilder jsonString = new StringBuilder();

		try {
			FileInputStream fileInputStream = new FileInputStream(content);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				jsonString.append(line.trim());
			}

			bufferedReader.close();
			jsonObject = new JSONObject(jsonString.toString());
			contentItems.add(jsonObject);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error encountered while reading file: " + content.getName() + ".", e);
		} catch (JSONException e) {
			LOGGER.log(Level.SEVERE, "Error encountered while reading file: " + content.getName() + ".", e);
		}
	}

	private static void processAndWriteJsonData(ArrayList<JSONObject> contentItems, File destinationFolder, String filename) {
		LOGGER.info("Writing data to file.");

		try {
			FileOutputStream fileOutputStream = new FileOutputStream(new File(destinationFolder.getAbsolutePath() + File.separator + filename));
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
			BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

			for (JSONObject contentItem : contentItems) {
				String contentCreatedDate = contentItem.getString("ecr:createDate");
				JSONObject content = contentItem.getJSONObject("contentItem");

				String contentType = content.getString("@name");
				String id = contentType.replace(" ", "").concat("_").concat(contentCreatedDate);
				String absractDescription = content.getString("dek");
				String title = content.getString("title");
				String curationDate = content.getString("curationDate");
				String image = content.getString("image");
				String catgeory = content.getString("category");
				JSONArray featuredRecords = content.getJSONObject("recordSelection").getJSONObject("filterState").getJSONArray("featuredRecords");

				bufferedWriter.write("id" + COLUMN_DELIMITER + id + System.lineSeparator());
				bufferedWriter.write("product_core_id" + COLUMN_DELIMITER + id + System.lineSeparator());
				bufferedWriter.write("product_availability_id" + COLUMN_DELIMITER + id + System.lineSeparator());
				bufferedWriter.write("title" + COLUMN_DELIMITER + title + System.lineSeparator());
				bufferedWriter.write("category" + COLUMN_DELIMITER + catgeory + System.lineSeparator());
				bufferedWriter.write("curation_date" + COLUMN_DELIMITER + curationDate + System.lineSeparator());
				bufferedWriter.write("abstract" + COLUMN_DELIMITER + absractDescription + System.lineSeparator());
				bufferedWriter.write("image" + COLUMN_DELIMITER + image + System.lineSeparator());
				bufferedWriter.write("content_type" + COLUMN_DELIMITER + contentType + System.lineSeparator());
				bufferedWriter.write("asset_source" + COLUMN_DELIMITER + "XM" + System.lineSeparator());
				bufferedWriter.write("business_unit_eligibility" + COLUMN_DELIMITER + "hbr" + System.lineSeparator());
				bufferedWriter.write("record_count" + COLUMN_DELIMITER + featuredRecords.length() + System.lineSeparator());

				for (int i = 0; i < featuredRecords.length(); i++) {
					String recordId = featuredRecords.getString(i);
					String featuredRecord = String.format("%02d|%s", i+1, recordId);
					bufferedWriter.write("featured_records" + COLUMN_DELIMITER + featuredRecord + System.lineSeparator());
				}

				bufferedWriter.write(RECORD_DELIMITER + System.lineSeparator());

			}

			bufferedWriter.close();
			outputStreamWriter.close();
			fileOutputStream.close();

		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "An unexpected error occurred.", e);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "An unexpected error occurred.", e);
		}

	}

	private static void processContentDirectory(ArrayList<JSONObject> contentItems, File contentFolder) {
		LOGGER.info("Processing content folder: " + contentFolder.getName() + ".");
		String[] contentItemNames = contentFolder.list();

		if (contentItemNames.length > 0) {

			for (String contentItemName : contentItemNames) {
				File contentItemFolder = new File(contentFolder.getAbsolutePath() + File.separator + contentItemName);

				if (contentItemFolder.isDirectory()) {
					parseContentItem(contentItems, contentItemFolder);
				}
			}
		} else {
			LOGGER.warning("Zero files found at location: " + contentFolder.getAbsolutePath());
		}
	}

}
