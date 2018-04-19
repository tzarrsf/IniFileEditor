package com.salesforce.commercecloud.config;
import java.util.Map;

/**
 * A DTO representing changes to be made to a .ini file such as a setup.ini file
 * 
 * @author tzarr
 */
public class HostFileChange
{
	private String filePattern;
	private Map<String, String> addEntryDictionary;
	private Map<String, String> removeEntryDictionary;
		
	public HostFileChange(String filePattern, Map<String, String> addEntryDictionary, Map<String,String> removeEntryDictionary)
	{
		this.filePattern = filePattern;
		this.addEntryDictionary = addEntryDictionary;
		this.removeEntryDictionary = removeEntryDictionary;
	}
	
	public HostFileChange(String filePattern)
	{
		this(filePattern, null, null);
	}
	
	public String getFilePattern()
	{
		return this.filePattern;
	}
	
	public Map<String, String> getAddEntryDictionary()
	{
		return this.addEntryDictionary;
	}
	
	public void setAddEntryDictionary(Map<String, String> addEntryDictionary)
	{
		this.addEntryDictionary = addEntryDictionary;
	}
	
	public Map<String, String> getRemoveEntryDictionary()
	{
		return this.removeEntryDictionary;
	}
	
	public void setRemoveEntryDictionary(Map<String, String> removeEntryDictionary)
	{
		this.removeEntryDictionary = removeEntryDictionary;
	}
	
	public int getHostAddCount()
	{
		return getAddEntryDictionary() != null ? getAddEntryDictionary().size() : 0;
	}
	
	public int getHostRemoveCount()
	{
		return getRemoveEntryDictionary() != null ? getRemoveEntryDictionary().size() : 0;
	}
	
	public int getTotalChangeCount()
	{
		return getHostAddCount() + getHostRemoveCount();
	}
}