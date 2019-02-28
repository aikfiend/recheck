package de.retest.recheck;

import java.io.File;

import de.retest.recheck.persistence.FileNamer;

public class MavenConformFileNamerStrategy implements FileNamerStrategy {

	public static final String DEFAULT_RETEST_WORKSPACE_PATH = "src/test/resources/retest/";
	public static final String DEFAULT_RETEST_TESTREPORTS_PATH = "target/test-classes/retest/";

	@Override
	public FileNamer createFileNamer( final String... baseNames ) {
		return new FileNamer() {
			@Override
			public File getFile( final String extension ) {
				final String baseName = String.join( "/", baseNames );
				return new File( DEFAULT_RETEST_WORKSPACE_PATH + File.separator + RECHECK_FOLDER_NAME + File.separator
						+ baseName + extension );
			}

			@Override
			public File getResultFile( final String extension ) {
				final String baseName = String.join( "/", baseNames );
				return new File( DEFAULT_RETEST_TESTREPORTS_PATH + File.separator + RECHECK_FOLDER_NAME + File.separator
						+ baseName + extension );
			}
		};
	}

}
