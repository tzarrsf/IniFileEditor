package com.salesforce.commercecloud.config;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupCreator
{
	String originalPath;
	String newPath;
	
	public BackupCreator(String originalPath)
	{
		this.originalPath = originalPath;
	}
	
	private String calculateNewPath(String path)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String timeStamp = sdf.format(new Date());
		return path + ".bak_" + timeStamp;
	}
	
	public boolean backupFile()
	{
		Program.logger.info("backupFile invoked...");
		long start = System.currentTimeMillis();
		boolean result = false;
		File file = new File(this.originalPath);
		
		//If the original path does not exist then intentionally crash the program
		if(!file.exists())
		{
			throw new IllegalStateException("The original path: '" + this.originalPath + "' could not be found." );
		}
		
		appendBakToFile();
		
		try
		{
			Files.copy(Paths.get(this.originalPath), Paths.get(this.newPath));
			result = true;
		}
		catch (IOException e)
		{
			Program.logger.error("Error in backupFile: " + e.toString());
		}
		
		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("backupFile took " + duration + " ms.");
		return result;
	}

	/*
	 * Assume that the new file could already be there. If it is we need to keep appending ".bak"
	 * to the file as we do not want to remove any evidence of the original or overwrite other backups.
	 */
	private void appendBakToFile()
	{
		long start = System.currentTimeMillis();
		Program.logger.info("appendBakToFile invoked...");
		this.newPath = calculateNewPath(this.originalPath);
		File file = new File(this.newPath);
		boolean fileAlreadyThere = file.exists();
		
		while(fileAlreadyThere)
		{
			this.newPath = calculateNewPath(this.newPath);
			file = new File(this.newPath);
			fileAlreadyThere = file.exists();
		}
		
		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("appendBakToFile took " + duration + " ms.");
	}
}