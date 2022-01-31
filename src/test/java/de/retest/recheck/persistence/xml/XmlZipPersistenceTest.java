package de.retest.recheck.persistence.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.retest.recheck.ioerror.ReTestLoadException;
import de.retest.recheck.ioerror.ReTestSaveException;
import de.retest.recheck.util.ApprovalsUtil;
import de.retest.recheck.util.FileUtil;

class XmlZipPersistenceTest {

	File baseFolder;
	XmlTransformer xmlTransformer;
	XmlZipPersistence<TestPersistable> persistence;

	@BeforeEach
	void setUp( @TempDir final Path temp ) {
		baseFolder = temp.toFile();
		xmlTransformer = new XmlTransformer( TestPersistable.class );
		persistence = new XmlZipPersistence<>( xmlTransformer );
	}

	@Test
	void simple_save_to_file() throws Exception {
		final TestPersistable element = new TestPersistable();

		persistence.save( baseFolder.toURI(), element );

		assertThat( baseFolder ).exists();
		ApprovalsUtil.verifyXml( readReTestXml( baseFolder ) );
	}

	String readReTestXml( final File file ) throws IOException {
		return FileUtil.readFromZipFile( file, in -> {
			final ZipEntry entry = in.getEntry( "retest.xml" );
			return IOUtils.toString( in.getInputStream( entry ), StandardCharsets.UTF_8 );
		} );
	}

	// TODO test case for existing file

	@Disabled( "Looks like we simply overwrite this?" )
	@Test
	void try_to_save_to_file_where_a_folder_with_the_same_name_already_exists() throws Exception {
		final TestPersistable element = new TestPersistable();
		persistence.save( baseFolder.toURI(), element );
		assertThatThrownBy( () -> persistence.save( baseFolder.toURI(), element ) )
				.isInstanceOf( ReTestSaveException.class );
	}

	@Test
	void simple_load_from_folder() throws Exception {
		final URI identifier = getClass().getResource( "/persistence/simple_zip_persisted.zip" ).toURI();

		final TestPersistable element = persistence.load( identifier );

		assertThat( element ).isNotNull();
	}

	@Test
	void try_to_load_from_non_existing_file() throws Exception {
		final File file = new File( baseFolder, "this will hopefully never exist" );
		assertThat( file ).doesNotExist();
		assertThatThrownBy( () -> persistence.load( file.toURI() ) ).isInstanceOf( ReTestLoadException.class );
	}

}
