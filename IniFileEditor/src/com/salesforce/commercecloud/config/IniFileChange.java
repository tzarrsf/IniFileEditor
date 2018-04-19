package com.salesforce.commercecloud.config;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A DTO representing changes to be made to a .ini file such as a setup.ini file or tomax.ini file
 * 
 * @author tzarr
 */
public class IniFileChange
{
	private String filePattern;
	private String section;
	private String restrictSearchToDirectories;
	private String groupName;
	private Map<String, String> entryDictionary;
		
	public IniFileChange(String filePattern, String section, String restrictSearchToDirectories, Map<String, String> entryDictionary, String groupName)
	{
		this.filePattern = filePattern;
		this.section = section;
		this.restrictSearchToDirectories = restrictSearchToDirectories;
		this.groupName = groupName;
		this.entryDictionary = entryDictionary;
	}
	
	public String getFilePattern()
	{
		return this.filePattern;
	}
	
	public String getSection()
	{
		return this.section;
	}
	
	public String getRestrictSearchToDirectories()
	{
		return this.restrictSearchToDirectories;
	}
	
	public List<File> getRestrictSearchToDirectoriesAsList()
	{
		List<File> result = new ArrayList<File>();
		String[] chunks = this.restrictSearchToDirectories.split(";");
		for(String chunk : chunks)
		{
			result.add(new File(chunk));
		}
		return result;
	}
	
	public String getGroupName()
	{
		return this.groupName;
	}
	
	public Map<String, String> getEntryDictionary()
	{
		return this.entryDictionary;
	}
}