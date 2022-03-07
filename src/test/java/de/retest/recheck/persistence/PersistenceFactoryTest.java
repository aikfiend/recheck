package de.retest.recheck.persistence;

import static de.retest.recheck.RecheckProperties.FILE_OUTPUT_FORMAT_PROPERTY_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.retest.recheck.persistence.PersistenceFactory.DynamicLoadPersistenceProxy;
import de.retest.recheck.persistence.bin.KryoPersistence;
import de.retest.recheck.persistence.xml.XmlFolderPersistence;
import de.retest.recheck.persistence.xml.XmlTransformer;
import de.retest.recheck.persistence.xml.XmlZipPersistence;

@PrepareForTest( { PersistenceFactory.class, XmlZipPersistence.class, XmlFolderPersistence.class } )
@RunWith( PowerMockRunner.class )
public class PersistenceFactoryTest {

	@Rule
	public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

	private PersistenceFactory factory;

	public XmlZipPersistence<Persistable> xmlZipPersistence;

	public XmlFolderPersistence<Persistable> xmlFolderPersistence;

	@Before
	@SuppressWarnings( "unchecked" )
	public void setUp() throws Exception {
		factory = new PersistenceFactory( (XmlTransformer) null );
		xmlZipPersistence = mock( XmlZipPersistence.class );
		PowerMockito.whenNew( XmlZipPersistence.class ).withAnyArguments().thenReturn( xmlZipPersistence );
		xmlFolderPersistence = mock( XmlFolderPersistence.class );
		PowerMockito.whenNew( XmlFolderPersistence.class ).withAnyArguments().thenReturn( xmlFolderPersistence );
	}

	@Test
	public void returns_dynamic_load_persistence() {
		final Persistence<? extends Persistable> persistence = factory.getPersistence();
		assertThat( persistence ).isInstanceOf( DynamicLoadPersistenceProxy.class );
	}

	@Test
	public void load_starts_zip_persistence_if_its_a_zip_file() throws Exception {
		final URI identifier = Paths.get( "src/test/resources/persistence/empty.zip" ).toUri();
		factory.getPersistence().load( identifier );

		verify( xmlZipPersistence ).load( identifier );
		verify( xmlFolderPersistence, never() ).load( identifier );
	}

	@Test
	public void load_starts_folder_persistence_if_its_a_folder() throws Exception {
		final URI identifier = Paths.get( "src/test/resources/persistence/" ).toUri();
		factory.getPersistence().load( identifier );

		verify( xmlFolderPersistence ).load( identifier );
		verify( xmlZipPersistence, never() ).load( identifier );
	}

	@Test
	public void save_use_zip_persistence_when_zip_is_default() throws Exception {
		final URI identifier = new URI( "" );
		final Persistable element = mock( Persistable.class );
		System.setProperty( FILE_OUTPUT_FORMAT_PROPERTY_KEY, FileOutputFormat.ZIP.name() );

		factory.getPersistence().save( identifier, element );

		verify( xmlZipPersistence ).save( identifier, element );
		verify( xmlFolderPersistence, never() ).save( identifier, element );
	}

	@Test
	public void save_use_folder_persistence_when_folder_is_default() throws Exception {
		final URI identifier = new URI( "" );
		final Persistable element = mock( Persistable.class );
		System.setProperty( FILE_OUTPUT_FORMAT_PROPERTY_KEY, FileOutputFormat.PLAIN.name() );

		factory.getPersistence().save( identifier, element );

		verify( xmlFolderPersistence ).save( identifier, element );
		verify( xmlZipPersistence, never() ).save( identifier, element );
	}

	@Test
	public void load_persistence_for_result_replay_should_be_kryo() throws Exception {
		final URI identifier = new File( "test.report" ).toURI();
		final Persistence<Persistable> loadPersistence = factory.getLoadPersistenceForIdentifier( identifier );
		assertThat( loadPersistence ).isInstanceOf( KryoPersistence.class );
	}

	@Test
	public void save_persistence_for_result_replay_should_be_kryo() throws Exception {
		final URI identifier = new File( "test.report" ).toURI();
		final Persistence<Persistable> savePersistence = factory.getSavePersistenceForIdentifier( identifier );
		assertThat( savePersistence ).isInstanceOf( KryoPersistence.class );
	}

}
