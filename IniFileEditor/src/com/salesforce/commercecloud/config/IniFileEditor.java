package com.salesforce.commercecloud.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Editor which can 'upsert' entries in INI files.
 * 
 * @author tzarr
 */
public class IniFileEditor
{
	private File file;
	private IniFileChange iniFileChange;

	public IniFileEditor(File file, IniFileChange iniFileChange)
	{
		this.file = file;
		this.iniFileChange = iniFileChange;
	}

	private List<String> linesToCollection()
	{
		List<String> lines = new ArrayList<String>();
		try
		{
			FileReader fileReader = new FileReader(this.file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line = null;
	
			while ((line = bufferedReader.readLine()) != null)
			{
				lines.add(line);
			}
	
			bufferedReader.close();
		}
		catch (IOException e)
		{
			Program.logger.error("Error opening the file : '" + this.file + "'.");
		}
		return lines;
	}

	// Check if the file contains the section we are looking for. If handed
	// "connect1" we Look for "[connect1]" and return true if found
	public boolean containsSection()
	{
		boolean found = false;
		String searchingFor = "[" + this.iniFileChange.getSection() + "]";
		for (String line : linesToCollection())
		{
			if (line.contains(searchingFor))
			{
				found = true;
				break;
			}
		}
		return found;
	}

	private String StringJoinForJava1_7(String delimiter, List<String> lines)
	{
		StringBuffer buffer = new StringBuffer();
		for (String line : lines)
		{
			buffer.append(line).append(delimiter);
		}
		return buffer.toString();
	}

	private void overwriteFile(List<String> lines, File file) throws Exception
	{
		try
		{
			FileWriter fileWriter = new FileWriter(file, false);

			try
			{
				// fileWriter.write(String.join("\n", lines));
				fileWriter.write(StringJoinForJava1_7("\r\n", lines));
			}
			catch (IOException e)
			{
				Program.logger.error("Error in overwriteFile: " + e.toString());
			}
			finally
			{
				if (fileWriter != null)
				{
					fileWriter.close();
				}
			}
		}
		catch (Exception e)
		{
			Program.logger.error("Error in overwriteFile: " + e.toString());
			throw e;
		}
	}

	public boolean MakeChanges()
	{
		Program.logger.info("MakeChanges: '" + this.file.toPath().toString() + "'...");
		long start = System.currentTimeMillis();
		int editStartIndex = -1;
		int editEndIndex = -1;
		List<String> lines = linesToCollection();
		// Figure out a beginning and end index for the section
		String searchingFor = "[" + this.iniFileChange.getSection() + "]";
		int lineIndex = 0;

		for (String line : lines)
		{
			// found the starting line for our section
			if (line.equals(searchingFor) || line.contains(searchingFor))
			{
				editStartIndex = lineIndex;
			}
			// found the starting line prior and now our next section line
			else if (editStartIndex > -1 && line.contains("[") && line.contains("]"))
			{
				editEndIndex = lineIndex;
				break;
			}
			lineIndex++;
		}

		// assume the end of file if there is no next section with square
		// brackets
		if (editEndIndex == -1)
		{
			editEndIndex = lineIndex;
		}

		// Add one more to offset the section vs key so we start on the key and not the square bracket section
		editStartIndex++;
		// Add the entry or update the existing entry
		List<String> editData = upsertKey(lines, editStartIndex, editEndIndex, this.iniFileChange);
		try
		{
			overwriteFile(editData, this.file);
		}
		catch(Exception e)
		{
			return false;
		}
		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("MakeChanges: '" + this.file.toPath().toString() + "' took " + duration + " ms.");
		
		return true;
	}

	public boolean VerifyChanges()
	{
		Program.logger.info("VerifyChanges in file: '" + this.file.toPath().toString() + "'...");
		boolean verified = false;
		long start = System.currentTimeMillis();
		int editStartIndex = -1;
		int editEndIndex = -1;
		List<String> lines = linesToCollection();
		
		// Figure out a beginning and end index for the section
		String searchingFor = "[" + this.iniFileChange.getSection() + "]";
		int lineIndex = 0;

		for (String line : lines)
		{
			// found the starting line for our section
			if (line.equals(searchingFor) || line.contains(searchingFor))
			{
				editStartIndex = lineIndex;
			}
			// found the starting line prior and now our next section line
			else if (editStartIndex > -1 && line.contains("[") && line.contains("]"))
			{
				editEndIndex = lineIndex;
				break;
			}
			lineIndex++;
		}
		
		// assume the end of file if there is no next section with square
		// brackets
		if (editEndIndex == -1)
		{
			editEndIndex = lineIndex;
		}

		// Add one more to offset the section vs key so we start on the key and
		// not the square bracket section
		editStartIndex++;
		verified = verifyKeyValuePairsPresent(lines, editStartIndex, editEndIndex, this.iniFileChange);
		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("VerifyChanges: '" + this.file.toPath().toString() + "' took " + duration + " ms.");
		return verified;
	}

	private boolean verifyKeyValuePairsPresent(List<String> lines, int startIndex, int endIndex, IniFileChange change)
	{
		boolean found = false;

		for (int i = startIndex; i < endIndex; i++)
		{
			for (String key : change.getEntryDictionary().keySet())
			{
				String keyWithEqualsAndValue = key + "=" + change.getEntryDictionary().get(key);

				Program.logger.debug("Checking for line: '" + keyWithEqualsAndValue + "' in file '" + this.file.toPath().toString() + " file...");

				if (lines.get(i).equals(keyWithEqualsAndValue))
				{
					Program.logger.info("Found line: '" + keyWithEqualsAndValue + "' in file '" + this.file.toPath().toString() + ".");
					found = true;
				}
			}
		}

		return found;
	}

	private List<String> upsertKey(List<String> lines, int startIndex, int endIndex, IniFileChange change)
	{
		Program.logger.info("upsertKey invoked with line search range of " + startIndex + " to " + endIndex + " ...");
		long start = System.currentTimeMillis();
		ArrayList<String> editableLines = new ArrayList<String>(lines);
		ArrayList<String> editsMade = new ArrayList<String>();
		updateKeyValuePair(startIndex, endIndex, change, editableLines, editsMade);
		insertNewKeyValuePair(endIndex, change, editableLines, editsMade);
		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("upsertKey with line search range of " + startIndex + " - " + endIndex + " completed in " + duration + " ms.");
		return editableLines;
	}

	private void updateKeyValuePair(int startIndex, int endIndex, IniFileChange change, ArrayList<String> editableLines, ArrayList<String> editsMade)
	{
		Program.logger.info("updateKeyValuePair invoked...");
		long start = System.currentTimeMillis();

		for (int i = startIndex; i < endIndex; i++) //iterate the lines looking at a specific section up to the next square bracketed section
		{
			for (String key : change.getEntryDictionary().keySet()) //iterate all the keys per each line
			{
				String keyWithEquals = key + "=";
				String keyFromFile = editableLines.get(i).split(Pattern.quote("="))[0];   
				//Specifically check the key left of the quote in case names start similarly
				if (keyFromFile.equals(key)) //If there is already a line with that key update it in place
				{
					Program.logger.info("Found : '" + keyWithEquals + "' in '" + editableLines.get(i) + "' on line " + (i + 1) + ".");
					String newEntry = keyWithEquals + change.getEntryDictionary().get(key);
					Program.logger.info("Setting: '" + editableLines.get(i) + "' to '" + newEntry + "'.");
					editableLines.set(i, newEntry);
					editsMade.add(key);
					Program.logger.info("Done setting: '" + editableLines.get(i) + "' to '" + newEntry + "' in place.");
				}
			}
		}

		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("updateKeyValuePair took " + duration + " ms and made '" + editsMade.size()  + "' edits.");
	}

	// Insert operation - if the key is not present then insert a new entry at
	// the bottom of that section
	private void insertNewKeyValuePair(int endIndex, IniFileChange change, ArrayList<String> editableLines, ArrayList<String> editsMade)
	{
		Program.logger.info("insertNewKeyValuePair invoked...");
		long start = System.currentTimeMillis();
		int insertIndex = endIndex - 1;

		for (String key : change.getEntryDictionary().keySet())
		{
			// Skip anything in our collection of keys which was used for an edit so we don't insert a second one
			if (editsMade.contains(key))
				continue;

			// Add the new stuff and bump the insertIndex with each addition
			String keyWithEquals = key + "=";
			String newEntry = keyWithEquals + change.getEntryDictionary().get(key);
			Program.logger.info("Inserting: '" + newEntry + "'.");
			editableLines.add(insertIndex, newEntry);
			insertIndex++;
		}

		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("insertNewKeyValuePair took " + duration + " ms");
	}
}