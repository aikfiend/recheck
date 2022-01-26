package de.retest.recheck;

import static de.retest.recheck.RecheckProperties.TEST_REPORT_FILE_EXTENSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.support.membermodification.MemberMatcher.method;

import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.retest.recheck.persistence.NamingStrategy;
import de.retest.recheck.persistence.ProjectLayout;
import de.retest.recheck.persistence.SeparatePathsProjectLayout;
import de.retest.recheck.ui.DefaultValueFinder;
import de.retest.recheck.ui.descriptors.IdentifyingAttributes;
import de.retest.recheck.ui.descriptors.RootElement;

@RunWith( PowerMockRunner.class )
@PrepareForTest( Rehub.class )
public class RecheckImplTest {

	@Rule
	TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void using_strange_stepText_should_be_normalized() throws Exception {
		final ProjectLayout spy = spy( new SeparatePathsProjectLayout( Paths.get( "" ), Paths.get( "" ) ) );
		final RecheckOptions opts = RecheckOptions.builder() //
				.projectLayout( spy ) //
				.build();
		final Recheck cut = new RecheckImpl( opts );

		final RecheckAdapter adapter = mock( RecheckAdapter.class );

		try {
			cut.check( mock( Object.class ), adapter, "!@#%$^&)te}{:|\\\":xt!(@*$" );
		} catch ( final Exception e ) {
			// Ignore Exceptions, fear AssertionErrors...
		}

		verify( spy ).getGoldenMaster( getClass().getName(), "using_strange_stepText_should_be_normalized",
				"!@#_$^&)te}{_____xt!(@_$" );
	}

	@Test
	public void test_class_name_should_be_default_result_file_name() throws Exception {
		final String suiteName = getClass().getName();
		final RecheckImpl cut = new RecheckImpl();
		final String resultFileName = cut.getResultFile().getName();
		assertThat( resultFileName ).isEqualTo( suiteName + TEST_REPORT_FILE_EXTENSION );
	}

	@Test
	public void suiteName_should_return_correct_path() {
		final RecheckImpl cut = new RecheckImpl( RecheckOptions.builder() //
				.suiteName( "name" ) //
				.namingStrategy( new NamingStrategyStub() ) //
				.build() );

		cut.startTest( "foo" );

		assertThat( cut.getGoldenMasterFile( "check" ) )
				.isEqualTo( new File( "src/test/resources/retest/recheck/name/foo.check.recheck" ) );
		assertThat( cut.getResultFile() ).isEqualTo( new File( "target/test-classes/retest/recheck/name.report" ) );
	}

	@Test
	public void exec_suite_name_should_be_used_for_result_file_name() throws Exception {
		final String suiteName = "FooBar";
		final RecheckOptions opts = RecheckOptions.builder() //
				.suiteName( suiteName ) //
				.build();
		final RecheckImpl cut = new RecheckImpl( opts );
		final String resultFileName = cut.getResultFile().getName();
		assertThat( resultFileName ).isEqualTo( suiteName + TEST_REPORT_FILE_EXTENSION );
	}

	@Test
	public void calling_check_without_startTest_should_work() throws Exception {
		final Path root = temp.newFolder().toPath();
		final RecheckOptions opts = RecheckOptions.builder() //
				.projectLayout( new WithinTempDirectoryProjectLayout( root ) ) //
				.namingStrategy( new NamingStrategyStub() ) //
				.build();
		final Recheck cut = new RecheckImpl( opts );
		cut.check( "String", new DummyStringRecheckAdapter(), "step" );
	}

	@Test
	public void calling_with_no_GM_should_produce_better_error_msg() throws Exception {
		final Path root = temp.newFolder().toPath();
		final RecheckOptions opts = RecheckOptions.builder() //
				.projectLayout( new WithinTempDirectoryProjectLayout( root ) ) //
				.namingStrategy( new NamingStrategyStub() ) //
				.build();
		final Recheck cut = new RecheckImpl( opts );

		final RootElement rootElement = mock( RootElement.class );
		when( rootElement.getIdentifyingAttributes() ).thenReturn( mock( IdentifyingAttributes.class ) );

		final RecheckAdapter adapter = mock( RecheckAdapter.class );
		when( adapter.canCheck( any() ) ).thenReturn( true );
		when( adapter.convert( any() ) ).thenReturn( Collections.singleton( rootElement ) );

		cut.startTest( "some-test" );
		cut.check( "to-verify", adapter, "some-step" );

		final String goldenMasterName = "SomeTestClass" + File.separator + "some-test.some-step.recheck";
		assertThatThrownBy( cut::capTest ) //
				.isExactlyInstanceOf( AssertionError.class ) //
				.hasMessageStartingWith( "'SomeTestClass': " + NoGoldenMasterActionReplayResult.MSG_LONG ) //
				.hasMessageEndingWith( goldenMasterName );
	}

	@Test
	public void headless_no_key_should_result_in_AssertionError() throws Exception {
		final RecheckOptions opts = RecheckOptions.builder() //
				.enableReportUpload() //
				.build();
		mockStatic( Rehub.class );
		doThrow( new HeadlessException() ).when( Rehub.class, method( Rehub.class, "authenticate" ) ).withNoArguments();
		assertThatThrownBy( () -> new RecheckImpl( opts ) ).isExactlyInstanceOf( AssertionError.class );
	}

	@org.junit.jupiter.api.Test
	public void constructor_should_invoke_project_layout_test_source_root( @TempDir final Path path ) {
		final ProjectLayout layout = mock( ProjectLayout.class );
		when( layout.getSuiteFolder( any() ) ).thenReturn( path );

		new RecheckImpl( RecheckOptions.builder() //
				.projectLayout( layout ) //
				.build() );

		verify( layout ).getTestSourcesRoot();
	}

	private static class DummyStringRecheckAdapter implements RecheckAdapter {

		@Override
		public DefaultValueFinder getDefaultValueFinder() {
			return null;
		}

		@Override
		public Set<RootElement> convert( final Object arg0 ) {
			final IdentifyingAttributes identifyingAttributes = mock( IdentifyingAttributes.class );
			final RootElement rootElement = mock( RootElement.class );
			when( rootElement.getIdentifyingAttributes() ).thenReturn( identifyingAttributes );
			return Collections.singleton( rootElement );
		}

		@Override
		public boolean canCheck( final Object arg0 ) {
			return false;
		}
	}

	private static class WithinTempDirectoryProjectLayout extends SeparatePathsProjectLayout {

		public WithinTempDirectoryProjectLayout( final Path root ) throws IOException {
			super( root, root );
		}
	}

	private static class NamingStrategyStub implements NamingStrategy {

		@Override
		public String getSuiteName() {
			return "SomeTestClass";
		}

		@Override
		public String getTestName() {
			return "someTestMethod";
		}
	}
}
