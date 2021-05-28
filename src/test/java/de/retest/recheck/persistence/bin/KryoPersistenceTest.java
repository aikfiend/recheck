package de.retest.recheck.persistence.bin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;

import de.retest.recheck.persistence.IncompatibleReportVersionException;
import de.retest.recheck.persistence.Persistable;
import de.retest.recheck.report.SuiteReplayResult;
import de.retest.recheck.report.TestReport;
import de.retest.recheck.util.VersionProvider;

class KryoPersistenceTest {

	@Test
	void roundtrip_should_work( @TempDir final Path temp ) throws IOException {
		final Path file = temp.resolve( "test.test" );
		Files.createFile( file );
		final URI identifier = file.toUri();

		final KryoPersistence<TestReport> kryoPersistence = new KryoPersistence<>();
		final TestReport persisted = createDummyTest();
		kryoPersistence.save( identifier, persisted );
		final TestReport loaded = kryoPersistence.load( identifier );

		assertThat( loaded.getSuiteReplayResults().get( 0 ).getName() )
				.isEqualTo( persisted.getSuiteReplayResults().get( 0 ).getName() );
	}

	public TestReport createDummyTest() {
		final SuiteReplayResult suite = new SuiteReplayResult( "test", 23, null, "00", null );
		return new TestReport( suite );
	}

	@Test
	void incompatible_version_should_give_persisting_version( @TempDir final Path temp ) throws IOException {
		final Path file = temp.resolve( "incompatible-test.test" );
		Files.createFile( file );
		final URI identifier = file.toUri();

		final KryoPersistence<TestReport> kryoPersistence = new KryoPersistence<>();
		kryoPersistence.save( identifier, createDummyTest() );

		final Kryo kryoMock = mock( Kryo.class );
		when( kryoMock.readClassAndObject( any() ) ).thenThrow( KryoException.class );
		final KryoPersistence<TestReport> differentKryoPersistence = new KryoPersistence<>( kryoMock, "old Version" );

		assertThatThrownBy( () -> differentKryoPersistence.load( identifier ) )
				.isInstanceOf( IncompatibleReportVersionException.class )
				.hasMessageContaining( "Incompatible recheck versions: report was written with "
						+ VersionProvider.RECHECK_VERSION + ", but read with old Version." );
	}

	@Test
	void load_should_not_be_able_to_load_1_6_0_report_version() throws Exception {
		final Path report = Paths.get( getClass().getResource( "1.6.0.report" ).toURI() );

		final KryoPersistence<TestReport> cut = new KryoPersistence<>();

		assertThatThrownBy( () -> cut.load( report.toUri() ) ) //
				.isInstanceOf( IncompatibleReportVersionException.class ) //
				.hasMessageContaining( "Incompatible recheck versions: report was written with 1.6.0" );
	}

	@Test
	void unknown_version_should_give_correct_error() throws IOException {
		final Path file = Paths.get( "src/test/resources/de/retest/recheck/persistence/bin/old.report" );
		final URI identifier = file.toUri();

		final KryoPersistence<TestReport> differentKryoPersistence = new KryoPersistence<>();

		assertThatThrownBy( () -> differentKryoPersistence.load( identifier ) ) //
				.isInstanceOf( IncompatibleReportVersionException.class ) //
				.hasMessageContaining(
						"Incompatible recheck versions: report was written with an old recheck version (pre 1.5.0), but read with "
								+ VersionProvider.RECHECK_VERSION + "." );
	}

	@Test
	void on_error_file_should_be_deleted() throws IOException {
		final File nonexistent = new File( "nonexistent.report" );
		final Kryo kryoMock = mock( Kryo.class );
		doThrow( KryoException.class ).when( kryoMock ).writeClassAndObject( any(), any() );
		final KryoPersistence<TestReport> persistence = new KryoPersistence<>( kryoMock, "some version" );
		assertThatThrownBy( () -> persistence.save( nonexistent.toURI(), createDummyTest() ) )
				.isInstanceOf( KryoException.class );
		assertThat( nonexistent ).doesNotExist();
	}

	@Test
	void isCompatible_should_be_compatible_with_equal_or_higher_value() throws Exception {
		final KryoPersistence<TestReport> cut = new KryoPersistence<>();

		assertThat( cut.isCompatible( TestReport.class, TestReport.PERSISTENCE_VERSION ) ).isTrue();
		assertThat( cut.isCompatible( TestReport.class, TestReport.PERSISTENCE_VERSION + 1 ) ).isTrue();
	}

	@Test
	void isCompatible_should_incompatible_with_lower_value() throws Exception {
		final KryoPersistence<TestReport> cut = new KryoPersistence<>();

		assertThat( cut.isCompatible( TestReport.class, TestReport.PERSISTENCE_VERSION - 1 ) ).isFalse();
	}

	@Test
	void isCompatible_should_compatible_with_not_present_value() throws Exception {
		final KryoPersistence<Persistable> cut = new KryoPersistence<>();

		assertThat( cut.isCompatible( Persistable.class, 1 ) ).isTrue();
	}

	@Test
	void non_existent_file_should_properly_throw_correct_exception() throws Exception {
		final KryoPersistence<TestReport> cut = new KryoPersistence<>();

		assertThatThrownBy( () -> cut.load( Paths.get( "/non/existent/file" ).toUri() ) ) //
				.isInstanceOf( NoSuchFileException.class );
	}

	@Test
	void valid_version_formats_should_be_recognized() {
		assertThat( KryoPersistence.isKnownFormat( "1.8.0-SNAPSHOT" ) ).isTrue();
		assertThat( KryoPersistence.isKnownFormat( "1.8.4" ) ).isTrue();
		assertThat( KryoPersistence.isKnownFormat( "1.8.0-beta.1" ) ).isTrue();
		assertThat( KryoPersistence.isKnownFormat( "${pom.parent.version}" ) ).isTrue();
	}

	@Test
	void invalid_version_formats_should_be_recognized() {
		assertThat( KryoPersistence.isKnownFormat(
				"src/test/resources/retest/recheck/de.retest.recheck.example.MyUnbreakableSeleniumTest/login.00.recheckjava.util.TreeSet" ) )
						.isFalse();
		assertThat( KryoPersistence.isKnownFormat( "" ) ).isFalse();
		assertThat( KryoPersistence.isKnownFormat( null ) ).isFalse();
	}
}
