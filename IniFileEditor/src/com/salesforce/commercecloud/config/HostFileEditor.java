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
 * Editor which can (for now) append new entries into the host file.
 * 
 * @author tzarr
 *
 */
public class HostFileEditor
{
	private File file;
	private HostFileChange hostFileChange;
	
	public HostFileEditor(File file, HostFileChange hostFileChange)
	{
		this.file = file;
		this.hostFileChange = hostFileChange;
	}
	
	private List<String> linesToCollection() throws IOException
	{
        FileReader fileReader = new FileReader(this.file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> lines = new ArrayList<String>();
        String line = null;
        
        while ((line = bufferedReader.readLine()) != null)
        {
            lines.add(line);
        }
        
        bufferedReader.close();
        return lines;
    }
	
	private String StringJoinForJava1_7(String delimiter, List<String> lines)
	{
		StringBuffer buffer = new StringBuffer();
		for(String line : lines)
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
	            //fileWriter.write(String.join("\n", lines));
				fileWriter.write(StringJoinForJava1_7("\r\n", lines));
	        }
			catch (IOException e)
			{
				Program.logger.error("Error in overwriteFile: " + e.toString());
	        }
			finally
			{
				if(fileWriter != null)
				{
					fileWriter.close();
				}
			}
		}
		catch(Exception e)
		{
			Program.logger.error("Error in overwriteFile: " + e.toString());
			throw e;
		}
	}
	
	public boolean MakeChanges()
	{
		Program.logger.info("MakeChanges: '" + this.file.toPath().toString() + "'...");
		long start = System.currentTimeMillis();
		int editStartIndex = 0;
		List<String> lines = null;
		
		try
		{
			lines = linesToCollection();
		}
		catch (IOException e)
		{
			String msg = "ERROR - Could not open file for editing: '" + this.file + "': " + e.toString();
			System.out.println("-------------------------------");
			System.out.println(msg);
			System.out.println("-------------------------------");
			Program.logger.error(msg);
			return false;
		}
		
		int editEndIndex = lines.size();
				
		//Add the entry or update the existing entry
		List<String> editData = upsertKey(lines, editStartIndex, editEndIndex, this.hostFileChange);
		deleteKey(editData, lines, editStartIndex, editEndIndex, this.hostFileChange);
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
		int editStartIndex = 0;
		int editEndIndex = -1;
		List<String> lines = null;
		
		try
		{
			lines = linesToCollection();
		}
		catch (IOException e)
		{
			String msg = "ERROR - Could not open file: '" + file.toPath().toString() + "' for verification. Suggestion: Check that the program is running as administrator and that the read-only attribute is not set.";
			System.out.println("-------------------------------");
			System.out.println(msg);
			System.out.println("-------------------------------");
			Program.logger.error(msg + e.toString());
			return verified;
		}
		
		editEndIndex = lines.size() - 1;
		verified = verifyKeyValuePairsPresent(lines, editStartIndex, editEndIndex, this.hostFileChange);
		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("VerifyChanges: '" + this.file.toPath().toString() + "' took " + duration + " ms.");
		return verified;
	}
	
	private boolean verifyKeyValuePairsPresent(List<String> lines, int startIndex, int endIndex, HostFileChange change)
	{
		for(int i=startIndex; i < endIndex; i++)
		{
			for(String key : change.getAddEntryDictionary().keySet())
			{
				Program.logger.debug("Checking for line: '" + key + "' in file '" + this.file.toPath().toString() + " file on line " + i + "...");
				
				if(lines.get(i).endsWith(key))
				{
					String[] chunks = lines.get(i).split(Pattern.quote("\t"));
					
					if(chunks.length > 1)
					{
						if(chunks[0] == change.getAddEntryDictionary().get(key))
						{
							Program.logger.info("Found line: '" + key + "' in file '" + this.file.toPath().toString() + " with value '" + change.getAddEntryDictionary().get(key) + "'....");
							return true;
						}
					}
				}
			}
		}
		
		return false;
	}
	
	private List<String> upsertKey(List<String> lines, int startIndex, int endIndex, HostFileChange change)
	{
		Program.logger.info("upsertKey invoked with line search range of " + startIndex + " to " + endIndex + " ...");
		long start = System.currentTimeMillis();
		ArrayList<String> editableLines = new ArrayList<String>(lines);
		ArrayList<String> editsMade = new ArrayList<String>();
		updateKeyValuePair(startIndex, endIndex, change, editableLines, editsMade);
		insertNewKeyValuePair(endIndex, change, editableLines, editsMade);
		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("upsertKey with line search range of " + startIndex + " - " + endIndex + " took " + duration + " ms.");
		return editableLines;
	}
	
	private void deleteKey(List<String> editableLines, List<String>lines, int startIndex, int endIndex, HostFileChange change)
	{
		long start = System.currentTimeMillis();
		deleteKeyValuePair(startIndex, endIndex, change, editableLines);
		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("upsertKey with line search range of " + startIndex + " - " + endIndex + " took " + duration + " ms.");
	}
	
	private void updateKeyValuePair(int startIndex, int endIndex, HostFileChange change, ArrayList<String> editableLines, ArrayList<String> editsMade)
	{
		Program.logger.info("updateKeyValuePair invoked...");
		long start = System.currentTimeMillis();
		
		for(int i=startIndex; i < endIndex; i++)
		{
			for(String key : change.getAddEntryDictionary().keySet())
			{
				if(editableLines.get(i).endsWith(key))
				{
					String newEntry = change.getAddEntryDictionary().get(key) + "\t" + key;
					Program.logger.info("Found : '" + key + "' in '" + editableLines.get(i) + "' on line " + (i + 1) + ".");
					Program.logger.info("Setting: '" + editableLines.get(i) + "' to '" + newEntry + "'."); 
					editableLines.set(i, newEntry);
					editsMade.add(key);
				}
			}
		}
		
		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("updateKeyValuePair took " + duration + " ms");
	}
	
	private void deleteKeyValuePair(int startIndex, int endIndex, HostFileChange change, List<String> editableLines)
	{
		Program.logger.info("updateKeyValuePair invoked...");
		long start = System.currentTimeMillis();
		
		for(int i=startIndex; i < editableLines.size(); i++)
		{
			for(String key : change.getRemoveEntryDictionary().keySet())
			{
				if(editableLines.get(i).endsWith(key))
				{
					Program.logger.info("Found ip address: '" + key + "' in '" + editableLines.get(i) + "' on line " + (i + 1) + ". Checking the host.");
					String entry = change.getRemoveEntryDictionary().get(key) + "\t" + key;
					if(editableLines.get(i).equals(entry))
					{
						Program.logger.info("Removing: '" + entry + "'."); 
						editableLines.remove(i);
						i--;
					}
				}
			}
		}
		
		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("updateKeyValuePair took " + duration + " ms");
	}
	
	//Insert operation - if the key is not present then insert a new entry at the bottom of that section
	private void insertNewKeyValuePair(int endIndex, HostFileChange change, ArrayList<String> editableLines, ArrayList<String> editsMade)
	{
		Program.logger.info("insertNewKeyValuePair invoked...");
		long start = System.currentTimeMillis();
		int insertIndex = endIndex;
		
		//Additions		
		for(String key : change.getAddEntryDictionary().keySet())
		{
			//Skip anything in our collection of keys which was used for an edit so we don't insert a second one
			if(editsMade.contains(key))
				continue;
			
			//Add the new stuff and bump the insertIndex with each addition
			String newEntry = change.getAddEntryDictionary().get(key) + "\t" + key ;
			Program.logger.info("Inserting: '" + newEntry + "'.");
			editableLines.add(insertIndex, newEntry);
			insertIndex++;
		}
		
		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("insertNewKeyValuePair took " + duration + " ms");
	}
}