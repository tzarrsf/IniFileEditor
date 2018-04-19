package com.salesforce.commercecloud.config;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Recursive scan of the system seeking a file(s) of interests whilst allowing for reasonable exceptions and restricted search path.
 * @author tzarr
 */
public class FileFinder
{
	private String fileNamePattern;
	private List<File> directoryRestrictions;
	@SuppressWarnings("serial")
	private List<String> pathExemptions = new ArrayList<String>()
	{{
	    add("C:\\Windows");
	    add("C:\\Users");
	    add("C:\\ProgramData");
	}};
	
	public FileFinder(String fileNamePattern, List<File> directoryRestrictions)
	{
		this.fileNamePattern = fileNamePattern;
		this.directoryRestrictions = directoryRestrictions;
	}
	
	private List<File> getOnlyDirectories(File[] files)
	{
		Program.logger.debug("getOnlyDirectories...");
		long start = System.currentTimeMillis();
		List<File> result = new ArrayList<File>();
		
		for(File file : files)
		{
			if(file.isDirectory())
			{
				Program.logger.debug("Adding '" + file.toPath().toString() + "'");
				result.add(file);
			}
		}
		
		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.debug("getOnlyDirectories took " + duration + " ms.");
		return result;
	}
	
	private void traverseDirectories(File directory, List<File> foundPaths)
	{
		Program.logger.debug("traverseDirectories invoked (recursive function) on " + directory.toPath().toString());
		File[] files = directory.listFiles();
		
		//Trapping a bug here where a directory that does not exist can be provided
		if(files == null)
		{
			return;
		}
		
		for(File file : files)
		{
			//Add any file which matches to our list
			if(file != null && file.isFile() && file.getPath().toLowerCase().endsWith(this.fileNamePattern.toLowerCase()))
			{
				Program.logger.debug("Found a possible file '" + file.toPath().toString() + "' in: '" + directory + "'");
				foundPaths.add(file);
			}
		}
		 
		List<File> directories = getOnlyDirectories(files);
		 
		for(File dir : directories)
		{
			if(pathExemptions.contains(dir.toPath().toString()))
			{
				Program.logger.debug("Skipping directory: '" + dir.toString() + "' since it is in path exemptions.");
				continue;
			}
			 
			Program.logger.debug("Checking: '" + dir.toString() + "'");
			 
			if(dir.listFiles() != null && dir.listFiles().length > 0)
			{
				traverseDirectories(dir, foundPaths);	 
			}
		}
	}
	
	public List<File> findConfigFiles()
	{
		Program.logger.info("findConfigFiles invoked...");
		long start = System.currentTimeMillis(); 
		File[] roots = File.listRoots();
		List<File> foundPaths = new ArrayList<File>();
		
		//If there is a set of directory search restrictions only search those
		if(this.directoryRestrictions != null && this.directoryRestrictions.size() > 0)
		{
			for(File directoryRestriction : this.directoryRestrictions)
			{
				traverseDirectories(directoryRestriction, foundPaths);
			}
		}
		else
		{
			Program.logger.info("Checking drives...");
			
			for(File drive : roots)
			{
				Program.logger.info("Checking: '" + drive.toString() + "'");
				traverseDirectories(drive, foundPaths);
			}
		}
		
		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("findConfigFiles has " + foundPaths.size() + " elements.");
		Program.logger.info("findConfigFiles took " + duration + " ms.");
		return foundPaths;
	}
}
