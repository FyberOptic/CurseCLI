package net.fybertech.cursecli;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import net.fybertech.curselib.CurseLib;
import net.fybertech.curselib.CurseLib.EnumDatabaseType;
import net.fybertech.curselib.database.CurseDatabase;
import net.fybertech.curselib.database.CurseFile;
import net.fybertech.curselib.database.CurseFileStub;
import net.fybertech.curselib.database.CurseFilter;
import net.fybertech.curselib.database.CurseProject;
import net.fybertech.curselib.database.manifest.CurseManifest;
import net.fybertech.curselib.database.manifest.ManifestFile;

public class CurseCLI 
{
	
	public static boolean databaseUpdatesEnabled = false;
	
	public static CurseDatabase db = null; 
	public static List<CurseProject> projects = null;		
	public static List<CurseFilter> filters = new ArrayList<>();
	public static boolean clientRunning = true;
	
	public static Map<String, IClientCommand> commands = new HashMap<>();
	public static List<String> descriptions = new ArrayList<>();
	
	
	public static interface IClientCommand 
	{
		public void runCommand();
	}


	public static void registerCommand(String[] commandStrings, String description, IClientCommand commandClass)
	{
		String cmdString = "";
		
		for (String cmd : commandStrings) {
			commands.put(cmd.toLowerCase(), commandClass);
			cmdString += cmd + ", ";
		}
		
		cmdString = cmdString.substring(0, cmdString.length() - 2);
		
		while (cmdString.length() < 18) cmdString = cmdString + " ";
		
		descriptions.add(cmdString + " : " + description);		
	}
	
		
	
	
	public static void main(String[] args) throws IOException 
	{
	
		CurseDatabase[] databaseSet = new CurseDatabase[EnumDatabaseType.values().length];
		
		if (databaseUpdatesEnabled) {
			
			for (EnumDatabaseType dbType : EnumDatabaseType.values()) 
			{
				long version = CurseLib.getLatestDatabaseVersion(dbType);			
				if (!dbType.getFile().exists()) { 
					System.out.print("Can't locate " + dbType.getUrlSlug() + " database, downloading...");
					CurseLib.downloadDatabase(version, dbType); 
					System.out.println("done");
				}
				
				CurseDatabase thisDB = CurseDatabase.Open(dbType.getFile(), false);
				
				if (thisDB.getDatabaseVersion() < version) {
					System.out.print("Downloading latest " + dbType.getUrlSlug() + " database...");
					CurseLib.downloadDatabase(version, dbType); 
					System.out.println("done");
				}
				else databaseSet[dbType.ordinal()] = thisDB;
			}			
		}
		
		
		System.out.print("Opening databases...");
		for (EnumDatabaseType dbType : EnumDatabaseType.values()) {
			int ordinal = dbType.ordinal();
			if (databaseSet[ordinal] == null) databaseSet[ordinal] = CurseDatabase.Open(dbType.getFile(), false);
		}
		System.out.println("done");		
		
			
		db = databaseSet[0];
		if (db == null) {
			System.out.println("Error: No primary database loaded, cannot continue!");
			return;
		}
		
		
		System.out.print("Merging databases...");
		for (EnumDatabaseType dbType : EnumDatabaseType.values()) {
			int ordinal = dbType.ordinal();
			if (ordinal == 0) continue;		
			
			db.mergeDatabase(databaseSet[ordinal]);
		}
		System.out.println("done");
		
		
		System.out.print("Processing database data...");
		db.processDatabaseData();
		System.out.println("done");
		
		
		projects = db.getAllProjects();
		
		
		System.out.println("\nSections:");
		for (String s : db.getSections()) {
			List<CurseProject> list = db.getProjectsBySection(s);
			System.out.println("  " + s + " - " + list.size() + " projects");
		}
		System.out.println("");
		
		
		
		

		registerCommand(new String[] { "filter", "filters" }, "Create filter", new IClientCommand() {
			@Override
			public void runCommand() {
				//while (true) 
				{			
					String filtersString = "\nActive filters:\n";
					if (filters.size() < 1) filtersString += "  none\n";
					else for (CurseFilter filter : filters) {
						filtersString += ("  " + filter + "\n");
					}
					
					String filterType = InputQueue.getNext(
							filtersString + 
							"\nAvailable filter types:\n" +
							"  Author\n" +
							"  Category\n" + 
							"  Name\n" + 
							"  Section\n" + 
							"  Version\n" + 
							"\nEnter type (\"clear\" to remove all, or return to cancel): "
					);
					
					filterType = filterType.toLowerCase();
					if (filterType.length() > 1) {
						if (filterType.equals("clear")) {
							filters.clear();
							projects = db.filter(filters);
						}
						else if (filterType.startsWith("ver")) {							
							String chosenVersion = InputQueue.getNext("Enter version: ");
							if (chosenVersion.length() > 0) {
								filters.add(CurseFilter.Version(chosenVersion));
								projects = db.filter(filters);
							}
						}
						else if (filterType.startsWith("sec")) {
							String sectionsString = "\nSection:\n";
							for (String s : db.getSections()) sectionsString += "  " + s + "\n";
							
							String chosenSection = InputQueue.getNext(sectionsString + "\nEnter section (or return to cancel): ");
							chosenSection = chosenSection.toLowerCase();
							if (chosenSection.length() > 1) {
								for (String section : db.getSections()) {
									if (section.toLowerCase().startsWith(chosenSection)) {
										filters.add(CurseFilter.Section(section));
										projects = db.filter(filters);
									}
								}
							}
						}
						else if (filterType.equals("name")) {
							String chosenName = InputQueue.getNextFull("\nName: ");
							if (chosenName.length() > 1) {							
								filters.add(CurseFilter.Name(chosenName));
								projects = db.filter(filters);
							}
						}
						else if (filterType.startsWith("auth")) {
							String chosenAuthor = InputQueue.getNextFull("\nAuthor: ");
							if (chosenAuthor.length() > 1) {							
								filters.add(CurseFilter.Author(chosenAuthor));
								projects = db.filter(filters);
							}
						}
						else if (filterType.startsWith("cat")) {
							String chosenCategory = InputQueue.getNextFull("\nCategory: ");
							if (chosenCategory.length() > 1) {							
								filters.add(CurseFilter.Category(chosenCategory));
								projects = db.filter(filters);
							}
						}
					}
					//else break;
					
					System.out.println("\nActive filters:");
					if (filters.size() < 1) System.out.println("  none");
					else for (CurseFilter filter : filters) {
						System.out.println("  " + filter);
					}
				}				
			}
		});
		
		
		registerCommand(new String[] { "list", "ls", "dir" }, "List projects", new IClientCommand() {
			@Override
			public void runCommand() {
				System.out.println("");
				for (CurseProject data : projects) {
					//System.out.println(data);
					System.out.println("           Name: " + data.Name);
					System.out.println("             Id: " + data.Id);					
					System.out.println(" Primary Author: " + data.PrimaryAuthorName);
					System.out.println("        Section: " + data.CategorySection.Name);
					System.out.println("");
				}
			}
		});
		
		
		registerCommand(new String[] { "project" }, "Get info on a project", new IClientCommand() {
			@Override
			public void runCommand() {
				int id = -1;
				try {
					id = Integer.parseInt(InputQueue.getNext("ID: "));
				}
				catch (Exception e) {}				
				
				System.out.println("");;
				
				CurseProject cid = db.getProjectById(id);
				if (cid == null) System.out.println("Invalid project ID");
				else {
					System.out.println("             Name: " + cid.Name);
					System.out.println("               Id: " + cid.Id);
					System.out.println("          Section: " + cid.CategorySection.Name);
					System.out.println("   Primary Author: " + cid.PrimaryAuthorName);					
					System.out.println(" Primary Category: " + cid.PrimaryCategoryName);
					System.out.println("          Summary: " + cid.Summary);					
					System.out.println("            Likes: " + cid.Likes);
					System.out.println("          Website: " + cid.WebSiteURL);
					System.out.println("            Files: " + cid.GameVersionLatestFiles.length);
				}
			}
		});
		
		registerCommand(new String[] { "files" }, "List files for a project", new IClientCommand() {
			@Override
			public void runCommand() {
				int id = -1;
				try {
					id = Integer.parseInt(InputQueue.getNext("ID: "));
				}
				catch (Exception e) {}				
				
				System.out.println("");;
				
				CurseProject cid = db.getProjectById(id);
				if (cid == null) System.out.println("Invalid project ID");
				else {
					for (CurseFileStub stub : cid.GameVersionLatestFiles) {
						System.out.println("     Filename: " + stub.ProjectFileName);
						System.out.println("      File ID: " + stub.ProjectFileID);
						System.out.println(" Game Version: " + stub.GameVesion);
						System.out.println("");
					}
					
					for (CurseFile curseFile : cid.LatestFiles) {
						System.out.println("     Filename: " + curseFile.FileName);
						System.out.println("      File ID: " + curseFile.Id);
						System.out.println(" Game Version: " + Arrays.toString(curseFile.GameVersion));
						System.out.println("");
					}
				}
			}
		});
		
		registerCommand(new String[] { "modpack" }, "Modpack commands", new IClientCommand() {
			@Override
			public void runCommand() {
				int id = -1;
				try {
					id = Integer.parseInt(InputQueue.getNext("Modpack or file ID: "));
				}
				catch (Exception e) {}
				
				System.out.println("");
				
				CurseManifest manifest = null;				
				
				if ((db.getProjectById(id)) != null) {
					System.out.println("Modpack ID detected, using default file ID for pack");
					id = db.getProjectById(id).DefaultFileId;
				}
				
				if (!db.files.containsKey(id)) System.out.println("Error: Invalid file ID");				
				else {					
					System.out.println("");
					
					CurseProject parentProject = db.getParentProjectOfFile(id);					
					if (!parentProject.isModpack()) {
						System.out.println("Error: Parent of file isn't a modpack");
					}
					else {
						System.out.println("Parent Modpack: " + parentProject.Name);
						
						while (true) {
							
							String packcmd = InputQueue.getNext("Modpack> ").toLowerCase();
							
							if (packcmd.equals("mods")) 
							{
								if (manifest == null) manifest = db.getModpackManifest(id);
								
								System.out.println("Manifest: \n" + manifest);
								for (ManifestFile file : manifest.files) {
									//System.out.println(file);
									CurseProject project = db.getProjectById(file.projectID);
									if (project == null) System.out.println("Invalid project ID in mod list: " + file.projectID);
									else System.out.println(project.Name);
								}
							}
							
							if (packcmd.equals("download")) 
							{
								if (manifest == null) manifest = db.getModpackManifest(id);								
								
								System.out.println("Getting associated files");
								for (ManifestFile file : manifest.files) {
									db.getFileFromCache(file.projectID, file.fileID);
								}
							}
							
							if (packcmd.length() < 1) break;
						}
					}
				}
			}
		});
		
		registerCommand(new String[] { "categories" }, "List categories", new IClientCommand() {
			@Override
			public void runCommand() {
				System.out.println("");
				for (String category : db.getCategories()) {
					System.out.println("  " + category);
				}
			}
		});
		
		registerCommand(new String[] { "versions" }, "List versions", new IClientCommand() {
			@Override
			public void runCommand() {
				System.out.println("");
				for (String version : db.getVersions()) {
					System.out.println("  " + version);
				}
			}
		});
		
		registerCommand(new String[] { "sections" }, "List sections", new IClientCommand() {
			@Override
			public void runCommand() {
				System.out.println("");
				for (String section : db.getSections()) {
					System.out.println("  " + section);
				}
			}
		});
		
		registerCommand(new String[] { "help", "?" }, "Show help", new IClientCommand() {
			@Override
			public void runCommand() {
				System.out.println("\nCommand list:");
				//for (String cmd : commands.keySet()) {
				for (String cmd : descriptions) {
					System.out.println("  " + cmd);
				}				
			}
		});
		
		registerCommand(new String[] { "quit", "exit" }, "Exit CurseCLI", new IClientCommand() {
			@Override
			public void runCommand() {
				clientRunning = false;
			}
		});
		

		
		while (clientRunning) {
			InputQueue.clear();
			System.out.println("Projects: " + projects.size());			
			String cmd = InputQueue.getNext("> ");
			if (cmd.length() < 1) continue;		
			cmd = cmd.toLowerCase();
			
			if (commands.containsKey(cmd)) commands.get(cmd).runCommand();
			
			System.out.println("");			
		}
		
		InputQueue.close();	
	}	
	
	
}
