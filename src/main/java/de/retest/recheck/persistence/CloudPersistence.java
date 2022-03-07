package de.retest.recheck.persistence;

import static de.retest.recheck.XmlTransformerUtil.getXmlTransformer;
import static org.apache.commons.lang3.StringUtils.abbreviate;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import de.retest.recheck.RecheckProperties;
import de.retest.recheck.Rehub;
import de.retest.recheck.persistence.bin.KryoPersistence;
import de.retest.recheck.persistence.xml.XmlFolderPersistence;
import de.retest.recheck.report.SuiteReplayResult;
import de.retest.recheck.report.TestReport;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CloudPersistence<T extends Persistable> implements Persistence<T> {
	private static final int MAX_REPORT_NAME_LENGTH = 50;

	private final KryoPersistence<T> kryoPersistence = new KryoPersistence<>();
	private final XmlFolderPersistence<T> folderPersistence = new XmlFolderPersistence<>( getXmlTransformer() );

	public static final String RECHECK_API_KEY = "RECHECK_API_KEY";

	@Override
	public void save( final URI identifier, final T element ) throws IOException {
		kryoPersistence.save( identifier, element );

		if ( isAggregatedReport( identifier ) && element instanceof TestReport ) {
			final TestReport report = (TestReport) element;
			try {
				saveToCloud( report, Files.readAllBytes( Paths.get( identifier ) ) );
			} catch ( final IOException e ) {
				if ( !report.containsChanges() ) {
					log.warn(
							"Could not read report '{}' for upload. Ignoring exception because the report does not have any differences.",
							identifier, e );
				} else {
					log.error( "Could not read report '{}' for upload. Rethrowing because report has differences.",
							identifier, e );
					throw e;
				}
			}
		}
	}

	private boolean isAggregatedReport( final URI identifier ) {
		return identifier.getPath().endsWith( RecheckProperties.AGGREGATED_TEST_REPORT_FILE_NAME );
	}

	private List<String> getTestClasses( final TestReport report ) {
		return report.getSuiteReplayResults().stream() //
				.map( SuiteReplayResult::getName ) //
				.collect( Collectors.toList() );
	}

	private void saveToCloud( final TestReport report, final byte[] data ) {

		final String uploadUrl = getUploadUrlForTestReport( report );

		if ( uploadUrl != null ) {
			log.trace( "Upload URL: {}", uploadUrl );

			final int maxAttempts = RecheckProperties.getInstance().rehubReportUploadAttempts();
			for ( int remainingAttempts = maxAttempts - 1; remainingAttempts >= 0; remainingAttempts-- ) {
				try {
					uploadReport( uploadUrl, data );
					break; // Successful, abort retry
				} catch ( final UnirestException e ) {
					if ( !report.containsChanges() ) {
						log.warn(
								"Failed to upload report. Ignoring exception because the report does not have any differences.",
								e );
						break;
					}
					if ( remainingAttempts == 0 ) {
						log.error(
								"Failed to upload report. Aborting, because maximum retries have been reached. If this happens often, consider increasing the property '{}={}'.",
								RecheckProperties.REHUB_REPORT_UPLOAD_ATTEMPTS, maxAttempts, e );
						throw e;
					} else {
						log.warn( "Failed to upload report. Retrying another {} times.", remainingAttempts, e );
					}
				}
			}
		} else {
			log.error( "Failed to upload report. Aborting, because could not obtain a upload URL." );
		}
	}

	private void uploadReport( final String uploadUrl, final byte[] data ) {
		final long start = System.currentTimeMillis();
		final HttpResponse<?> uploadResponse = Unirest.put( uploadUrl ) //
				.body( data ) //
				.asEmpty();

		if ( uploadResponse.isSuccess() ) {
			final long duration = System.currentTimeMillis() - start;
			log.info( "Successfully uploaded report to rehub in {} ms", duration );
		}
	}

	WeakHashMap<TestReport, UUID> reportIdCache = new WeakHashMap<>();

	private String getUploadUrlForTestReport( final TestReport report ) {
		final UUID id = reportIdCache.computeIfAbsent( report, report2 -> UUID.randomUUID() );
		final String reportName = String.join( ", ", getTestClasses( report ) );
		final String token = String.format( "Bearer %s", Rehub.getAccessToken() );

		final HttpResponse<String> uploadUrlResponse =
				Unirest.post( RecheckProperties.getInstance().rehubReportUploadUrl() + "/" + id ) //
						.queryString( "name", abbreviate( reportName, MAX_REPORT_NAME_LENGTH ) ) //
						.header( "Authorization", token )//
						.asString();

		if ( uploadUrlResponse.isSuccess() ) {
			return uploadUrlResponse.getBody();
		} else {
			return null;
		}

	}

	@Override
	public T load( final URI identifier ) throws IOException {
		if ( Paths.get( identifier ).toFile().isDirectory() ) {
			return folderPersistence.load( identifier );
		}
		return kryoPersistence.load( identifier );
	}

}
