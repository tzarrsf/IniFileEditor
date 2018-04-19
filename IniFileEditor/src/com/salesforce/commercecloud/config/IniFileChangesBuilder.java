package com.salesforce.commercecloud.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class IniFileChangesBuilder {
	final static String iniString = "ini";
	static List<String> reservedSuffixes = null;

	// Yes, This is a static constructor - Java needs to catch up to C#!
	static {
		if (reservedSuffixes == null) {
			reservedSuffixes = new ArrayList<String>();
			reservedSuffixes.add("FileNamePattern");
			reservedSuffixes.add("SectionName");
			reservedSuffixes.add("RestrictSearchToDirectories");
		}
	}

	/**
	 * CreateIniFileChangeList is where we build the data structures for what
	 * you want to edit in the INI files. Structure is like this: IniFileChange
	 * - filePatternName (String for the file you want to look for in a basic
	 * match) - section (String representing the section name without square
	 * brackets, i.e. "section1" becomes "[section1]" in the search - groupName
	 * (String representing the group form the ini file, like 1,2,3 or whatever
	 * the user configured - entryDictionary And we build a collection of these
	 * IniFileChange structures to do our dirty work.
	 * 
	 * @return List of the IniFileChange(s) to be made
	 */
	public static List<IniFileChange> BuildIniFileChanges(Map<String, String> config) {
		// A little tricky making this all dynamic...
		// Use a firstIndexOf and lastIndexOf approach for the prefix and suffix
		// and use the indices to get the middle (groupName).
		// Then loop against prefix, groupName and suffix checking for null and
		// if relevant data is found build the list of type
		// IniFileChange.
		Program.logger.info("Creating Ini file change list...");
		long start = System.currentTimeMillis();
		Set<String> groupNames = getGroupNames(config);

		// Now that we have clean groups check all possible keys and build the
		// relevant IniFileChange object fully intact with a groupName on it
		List<IniFileChange> result = new ArrayList<IniFileChange>();

		for (String groupName : groupNames) {
			if (meetsBasicCriteriaForConstruction(config, groupName)) {
				// Add the changes to make outside the reserved suffixes to our
				// entryDictionary
				Map<String, String> entryDictionary = new HashMap<String, String>();

				for (String key : config.keySet()) {
					if (key.startsWith(iniString + groupName)) // Is a member of
																// this group
					{
						// We have to remove the prefix and group name leaving
						// us with only the suffix before inserting it
						// as this will be used for the *actual* change made to
						// the file i.e 'timeout' not 'ini1Timeout'
						// and we only want to add things not in the
						// reservedSuffixes.
						String suffix = key.replaceFirst(Pattern.quote("ini" + groupName), "");
						if (!reservedSuffixes.contains(suffix)) {
							entryDictionary.put(suffix, config.get(key));
						}
					}
				}

				// Instantiate the object with the entryDictionary intact
				IniFileChange iniFileChange = new IniFileChange(config.get(iniString + groupName + "FileNamePattern"),
						config.get(iniString + groupName + "SectionName"),
						config.get(iniString + groupName + "RestrictSearchToDirectories"), entryDictionary, groupName);
				result.add(iniFileChange);
			}
		}

		long stop = System.currentTimeMillis();
		long duration = stop - start;
		Program.logger.info("Creating Ini file change list took " + duration + " ms.");
		return result;
	}

	private static boolean meetsBasicCriteriaForConstruction(Map<String, String> config, String groupName) {
		return config.get(iniString + groupName + "FileNamePattern") != null
				&& config.get(iniString + groupName + "SectionName") != null;
	}

	private static Set<String> getGroupNames(Map<String, String> config) {
		// Cycle through the keys looking for prefix of 'ini' and a matching
		// suffix like 'FileNamePattern' i.e. 'ini1FileNamePattern',
		// 'ini2FileNamePattern', etc.
		Set<String> groupNames = new HashSet<String>();

		for (String key : config.keySet()) {
			if (!key.startsWith(iniString))
				continue;

			for (String suffix : reservedSuffixes) {
				int startIndex = key.indexOf(iniString) + iniString.length();
				int suffixStartIndex = key.indexOf(suffix);

				if (startIndex != -1 && suffixStartIndex != -1) {
					// Extract a unique list of groups
					groupNames.add(key.substring(startIndex, suffixStartIndex));
				}
			}
		}

		return groupNames;
	}
}