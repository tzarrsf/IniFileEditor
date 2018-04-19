package com.salesforce.commercecloud.config;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Simple property reader to get values out of our config file
 *  
 * @author tzarr
 *
 */
public class PropertyReader
{
	private final String configFileName = "appconfig.xml";
	private File configFile;
	private Properties properties;
	private FileInputStream fis; 
	
	public PropertyReader()
	{
		try
		{
			Program.logger.info("Searching absolute path: '" + new File(".").getAbsolutePath() + "' for: '" + configFileName + "'.");
			this.configFile = new File(configFileName);
			fis = new FileInputStream(this.configFile);
			this.properties = new Properties();
			this.properties.loadFromXML(fis);
			fis.close();
		}
		catch (FileNotFoundException fnfe)
		{
			Program.logger.fatal("Error in PropertyReader - FileNotFoundException: " + fnfe.toString());
		}
		catch (IOException ioe)
		{
			Program.logger.fatal("Error in PropertyReader - IOException: " + ioe.toString());
		}
	}
	
	public Map<String,String> getEntryDictionary()
	{
		Map<String,String> dictionary = new HashMap<String,String>();
		
		for (Object key : this.properties.keySet())
		{
			String value = this.properties.getProperty(key.toString());
			dictionary.put(key.toString(), value);
		}
		
		return dictionary;
	}
}