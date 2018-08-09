package com.jsparser.rhino;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

public class ProjectMigrator {
	
	public static void main(String[] args) {
		try {
			new ProjectMigrator().migrateJSfiles("/Users/ravik/wm/migration_ang_1_to_5/tests/ezsource_1.2");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void migrateJSfiles(String folderPath) throws IOException {

		Stream<Path> jsFilePathStream = null;
		try {
			jsFilePathStream = Files.find(Paths.get(folderPath), Integer.MAX_VALUE,
					(filePath, fileAttr) -> {
						
						boolean isJsFile = filePath.getFileName().toString().endsWith(".js");
						if(!isJsFile) {
							return false;
						}
						
						//Grand parent Path name is "pages" i.e. ezsource_1.2/src/main/webapp/pages
						boolean isInPagesOrWebappFolder = filePath.getParent().getParent().endsWith(Paths.get("pages")) || filePath.getParent().endsWith(Paths.get("webapp"));
						if(!isInPagesOrWebappFolder) {
							return false;
						}
						
						//page name is same as the folder name
						boolean isPageOrAppJs = filePath.getFileName().endsWith(filePath.getParent().getFileName().toString() + ".js") ||
													filePath.getFileName().endsWith("app.js");
						return isPageOrAppJs;
						
						/*return filePath.getFileName().toString().endsWith(".js")
								&& ((filePath.toString().contains(Paths.get("/pages/").toString())
										|| filePath.toString().contains(Paths.get("/webapp/app.js").toString())
										|| filePath.toString().contains(Paths.get("/webapp/app_9_x.js").toString()))
										&& !filePath.toString().contains(Paths.get("/WEB-INF/").toString()));*/
					});
			
			/*jsFilePathStream.forEach((filePath) -> {
				System.out.println(filePath.toString());
			});*/
			jsFilePathStream.forEach((filePath) -> {
				try {
					Path newPath = filePath.resolveSibling(getNewFileName(filePath.getFileName().toString()));
					ScriptMigrator scriptMigrator = new ScriptMigrator(new FileReader(filePath.toFile()), filePath.toString(), null);
					String newScript = scriptMigrator.getMigratedScript();
					if(newScript != null && !newScript.trim().isEmpty()) {
						Files.move(filePath, newPath, StandardCopyOption.ATOMIC_MOVE);
						Files.write(filePath, newScript.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
					}
				} catch (IOException e) {
					throw new IllegalArgumentException("error Processing" , e);
				}
			});
		} finally {
			jsFilePathStream.close();
		}
	}
	
	private String getNewFileName(String fileName) {
//		String newName = fileName.substring(0, fileName.lastIndexOf("_9_x.js")) + ".js";
		String newName = fileName.substring(0, fileName.lastIndexOf(".js")) + "_9_x.js";
		return newName;
	}
}
