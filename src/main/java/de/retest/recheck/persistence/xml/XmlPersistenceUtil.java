package de.retest.recheck.persistence.xml;

import java.io.IOException;

import de.retest.recheck.persistence.Persistable;
import de.retest.recheck.persistence.migration.XmlMigrator;
import de.retest.recheck.persistence.xml.util.XmlVersionCheckResult;
import de.retest.recheck.util.NamedBufferedInputStream;
import jakarta.xml.bind.Unmarshaller.Listener;

public class XmlPersistenceUtil {

	private XmlPersistenceUtil() {}

	static <T extends Persistable> ReTestXmlDataContainer<T> migrateAndRead( final XmlTransformer xml,
			final NamedBufferedInputStream inputStream, final Listener unmarshallListener ) throws IOException {
		NamedBufferedInputStream bin = inputStream;

		final XmlVersionCheckResult checkResult = XmlVersionCheckResult.create( bin );

		if ( checkResult.newDataTypeInstance == null ) {
			throw new RuntimeException( "Unexpected data type " + checkResult.oldDataType );
		}

		if ( !checkResult.isCompatible() ) {
			bin = XmlMigrator.tryToMigrate( checkResult, bin );
			if ( bin == null ) {
				throw new RuntimeException( "Could not migrate XML." );
			}
		}

		return xml.fromXML( bin, unmarshallListener );
	}

}
