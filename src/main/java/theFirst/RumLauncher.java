package theFirst;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RumLauncher {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
	//private static final String LPSOLVE55 = "lpsolve55";
	//private static final String LPSOLVE55J = "lpsolve55j";

	public static void main(String[] args) {
		// required Helper Class that avoids Missing Components exception when starting runnable Jar
		// Alternative would be to add this to VM options: --module-path /path/to/JavaFX/lib --add-modules=javafx.controls
		logger.debug("Starting RuM launcher");
		Locale.setDefault(Locale.UK);
		
		// lpsolve55 is needed for DataAware Declare Replayer
		//loadLpSolve55();

		// clingo is needed for ASP Log Generator
		Path clingoPath = Paths.get("asp_generator");
		extractFoldersFromJar(clingoPath); //TODO: Can cause "java.lang.IllegalArgumentException: URI is not hierarchical" exception, which should be further investigated
		
		// used by the ltl checker in the filtering section
		extractFoldersFromJar(Paths.get("ltlformulas")); //TODO: Can cause "java.lang.IllegalArgumentException: URI is not hierarchical" exception, which should be further investigated
		
		// Starting the application itself
		Rum.main(args);
	}

	private static void extractFoldersFromJar(Path... paths) {
		final File jarFile = new File(RumLauncher.class.getProtectionDomain().getCodeSource().getLocation().getPath());

		for (Path resourcePath : paths) {

			if (Files.exists(resourcePath)) {
				logger.debug("No need to extract \"{}\", already existing.", resourcePath);
				break;
			}

			if (jarFile.isFile()) {	// Run with JAR file
				
				try (JarFile jar = new JarFile(jarFile)) {
					
					final Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						final String name = entries.nextElement().getName().replace('/', File.separatorChar);
						// Saving all entries contained in resource path that are not folder paths
						if (name.startsWith(resourcePath.toString()) && !name.endsWith(File.separator))
							extractResource(name);
					}

					logger.debug("Extracted \"{}\" from RuM jar.", resourcePath);

				} catch (IOException e) {
					logger.warn("Unable to extract \"{}\" from RuM jar.\n",resourcePath, e);
				}

			} else {	// Run with IDE
				
				try {
					URI uri = RumLauncher.class.getResource("/" + resourcePath).toURI();
					Path basePath = new File(uri).toPath();
					
					List<String> resources = new ArrayList<>();
					try ( Stream<Path> pathStream = Files.walk(basePath) ) {
						resources = pathStream.filter(path -> !Files.isDirectory(path)).map(Path::toString).collect(Collectors.toList());
					}

					Path classesPath = new File( jarFile.toURI() ).toPath();

					for (String res : resources)
						extractResource( res.substring(classesPath.toString().length()+1) );
					
					logger.debug("Extracted \"{}\" from project folder.", resourcePath);
					
				} catch (IOException | URISyntaxException e) {
					logger.warn("Unable to extract \"{}\" from project folder.\n",resourcePath, e);
				}
			}
		}
	}

	private static void extractResource(String pathString) throws IOException {
		String folderPath = pathString.substring(0, pathString.lastIndexOf(File.separatorChar));
		File outputFolder = new File(folderPath);
		outputFolder.mkdirs();
		
		String resourceName = pathString.substring(pathString.lastIndexOf(File.separatorChar) +1);
		File outputFile = new File(folderPath, resourceName);
		
		try (InputStream inputStream = RumLauncher.class.getClassLoader().getResource(pathString.replace(File.separatorChar, '/')).openStream()) {
			try (OutputStream outputStream = new FileOutputStream(outputFile)) {
				IOUtils.copy(inputStream, outputStream);
				outputFile.setExecutable(true);
			}
		}
	}
	
	/*
	// Checks if lpsolve55 and lpsolve55j are present on the system and if not then extracts and loads both
	private static void loadLpSolve55() {
		logger.debug("Loading lpsolve55");
		try {
			System.loadLibrary(LPSOLVE55);
			logger.debug("Loaded from system {}", LPSOLVE55);
		} catch (UnsatisfiedLinkError e) {
			extractAndLoadLibrary(LPSOLVE55);
		}
		try {
			System.loadLibrary(LPSOLVE55J);
			logger.debug("Loaded from system {}", LPSOLVE55J);
		} catch (UnsatisfiedLinkError e) {
			extractAndLoadLibrary(LPSOLVE55J);
		}
	}
	
	// Extracts and loads a library from the RuM jar file
	private static void extractAndLoadLibrary(String libraryName) {
		libraryName = System.mapLibraryName(libraryName);
		InputStream inputStream = RumLauncher.class.getClassLoader().getResourceAsStream(libraryName);

		File outputFile = new File(libraryName);
		try (OutputStream outputStream = new FileOutputStream(outputFile)) {
			
			IOUtils.copy(inputStream, outputStream);
			inputStream.close();

	        System.load(outputFile.getAbsolutePath());
	        logger.debug("Extracted from RuM jar and loaded: {}", libraryName);

		} catch (IOException e) {
			logger.warn("Unable to load {}", libraryName, e);
		}
	}
	*/
}