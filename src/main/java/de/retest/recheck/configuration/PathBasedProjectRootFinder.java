package de.retest.recheck.configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.retest.recheck.RecheckProperties;

public class PathBasedProjectRootFinder implements ProjectRootFinder {

	private static final Logger logger = LoggerFactory.getLogger( PathBasedProjectRootFinder.class );
	private final Set<Path> indicators = new HashSet<>();

	public PathBasedProjectRootFinder() {
		indicators.add( Paths.get( RecheckProperties.RETEST_FOLDER_NAME ) );
		indicators.add( Paths.get( "src/main/java" ) );
		indicators.add( Paths.get( "src/test/java" ) );
	}

	@Override
	public Optional<Path> findProjectRoot( final Path base ) {
		if ( base == null || !base.toAbsolutePath().toFile().exists() ) {
			logger.error( "Project root not found, base path does not exist." );
			return Optional.empty();
		}

		final Path absoluteBasePath = base.toAbsolutePath();
		logger.debug( "Searching for project root under '{}'.", absoluteBasePath );

		for ( Path currentFolder = absoluteBasePath; currentFolder != null; currentFolder =
				currentFolder.getParent() ) {
			if ( containsSubPath( currentFolder ) ) {
				logger.debug( "Found project root in '{}'.", currentFolder );
				return Optional.of( currentFolder.toAbsolutePath() );
			}
		}

		logger.error( "Project root not found in {} or any parent folder.", absoluteBasePath );
		return Optional.empty();
	}

	private boolean containsSubPath( final Path basePath ) {
		if ( basePath != null ) {
			return indicators.stream().anyMatch( subPath -> basePath.resolve( subPath ).toFile().exists() );
		}
		return false;
	}

}
