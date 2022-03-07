package de.retest.recheck.persistence;

import static de.retest.recheck.XmlTransformerUtil.getXmlTransformer;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Set;

import de.retest.recheck.RecheckAdapter;
import de.retest.recheck.RecheckProperties;
import de.retest.recheck.meta.MetadataProviderService;
import de.retest.recheck.ui.descriptors.RootElement;
import de.retest.recheck.ui.descriptors.SutState;

public class RecheckSutState {

	private static final PersistenceFactory persistenceFactory = new PersistenceFactory( getXmlTransformer() );

	private RecheckSutState() {}

	public static SutState convert( final Object toCheck, final RecheckAdapter adapter ) {
		final Set<RootElement> converted = adapter.convert( toCheck );
		if ( converted == null || converted.isEmpty() ) {
			throw new IllegalStateException( "Cannot check empty state!" );
		}
		return new SutState( converted, MetadataProviderService.of( () -> adapter.retrieveMetadata( toCheck ) ) );
	}

	public static SutState createNew( final File file, final SutState actual ) {
		try {
			persistenceFactory.getPersistence().save( file.toURI(), actual );
		} catch ( final IOException e ) {
			throw new UncheckedIOException( "Could not save SUT state '" + actual + "' to '" + file + "'.", e );
		}
		return new SutState( new ArrayList<>() );
	}

	public static SutState loadExpected( final File file ) {
		// Folder could exist, but not the retest.xml...
		if ( !file.exists() || !new File( file, RecheckProperties.DEFAULT_XML_FILE_NAME ).exists() ) {
			return null;
		}
		try {
			return (SutState) persistenceFactory.getPersistence().load( file.toURI() );
		} catch ( final IOException e ) {
			throw new UncheckedIOException( "Could not load SUT state from '" + file + "'.", e );
		}
	}
}
