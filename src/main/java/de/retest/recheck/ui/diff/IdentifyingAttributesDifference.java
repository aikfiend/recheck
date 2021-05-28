package de.retest.recheck.ui.diff;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.retest.recheck.ui.descriptors.Attribute;
import de.retest.recheck.ui.descriptors.IdentifyingAttributes;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.EqualsAndHashCode;

@XmlRootElement
@XmlAccessorType( XmlAccessType.FIELD )
@EqualsAndHashCode
public class IdentifyingAttributesDifference implements LeafDifference {

	private static final long serialVersionUID = 1L;

	@XmlAttribute
	private final String differenceId;

	@XmlElement( name = "attribute" )
	private final List<Attribute> attributes;

	@XmlElement( name = "attributeDifference" )
	private final List<AttributeDifference> attributeDifferences;

	@SuppressWarnings( "unused" )
	private IdentifyingAttributesDifference() {
		// for JAXB
		differenceId = null;
		attributes = null;
		attributeDifferences = null;
	}

	public IdentifyingAttributesDifference( final IdentifyingAttributes expectedIdentAttributes,
			final List<AttributeDifference> attributeDifferences ) {
		attributes = expectedIdentAttributes.getAttributes();
		this.attributeDifferences = attributeDifferences;
		differenceId = AttributeDifference.getSumIdentifier( attributeDifferences );
	}

	@Override
	public String toString() {
		final StringBuilder expectedDiff = new StringBuilder();
		final StringBuilder actualDiff = new StringBuilder();
		for ( final AttributeDifference attributeDifference : attributeDifferences ) {
			expectedDiff.append( " expected " ).append( attributeDifference.getKey() ).append( ": " )
					.append( attributeDifference.getExpected() );
			actualDiff.append( " actual " ).append( attributeDifference.getKey() ).append( ": " )
					.append( attributeDifference.getActual() );
		}
		return expectedDiff.toString().trim() + " - " + actualDiff.toString().trim();
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public List<ElementDifference> getNonEmptyDifferences() {
		return Collections.emptyList();
	}

	@Override
	public Serializable getActual() {
		return attributeDifferences.stream() //
				.map( attributeDifference -> attributeDifference.getKey() + "=" + attributeDifference.getActual() ) //
				.collect( Collectors.joining( " " ) ) //
				.toString().trim();
	}

	@Override
	public Serializable getExpected() {
		return attributeDifferences.stream() //
				.map( attributeDifference -> attributeDifference.getKey() + "=" + attributeDifference.getExpected() ) //
				.collect( Collectors.joining( " " ) ) //
				.toString().trim();
	}

	@Override
	public List<ElementDifference> getElementDifferences() {
		return Collections.emptyList();
	}

	public List<AttributeDifference> getAttributeDifferences() {
		return attributeDifferences;
	}
}
