package de.retest.recheck.review.ignore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import de.retest.recheck.ui.descriptors.Attributes;
import de.retest.recheck.ui.descriptors.Element;
import de.retest.recheck.ui.descriptors.IdentifyingAttributes;
import de.retest.recheck.ui.diff.AttributeDifference;

class AttributeFilterTest {

	@Test
	void should_match_when_attribute_is_same() {
		final AttributeFilter filter = new AttributeFilter( "tag" );

		final Element element = mock( Element.class );

		final AttributeDifference attributeDifference = mock( AttributeDifference.class );
		when( attributeDifference.getKey() ).thenReturn( "tag" );

		assertThat( filter.matches( element, attributeDifference ) ).isTrue();
	}

	@Test
	void should_not_match_when_attribute_is_different() {
		final AttributeFilter filter = new AttributeFilter( "tag" );

		final Element element = mock( Element.class );
		final IdentifyingAttributes identAttribs = mock( IdentifyingAttributes.class );
		when( element.getIdentifyingAttributes() ).thenReturn( identAttribs );
		when( identAttribs.get( "type" ) ).thenReturn( "div" );
		final Attributes attribs = mock( Attributes.class );
		when( element.getAttributes() ).thenReturn( attribs );
		when( attribs.get( "mySpecialAttribute" ) ).thenReturn( "someValue" );

		final AttributeDifference attributeDifference = mock( AttributeDifference.class );
		when( attributeDifference.getKey() ).thenReturn( "mySpecialAttribute" );

		assertThat( filter.matches( element, attributeDifference ) ).isFalse();
	}

	@Test
	void should_match_with_attribute_key_when_attribute_is_same() {
		final AttributeFilter filter = new AttributeFilter( "tag" );

		final Element element = mock( Element.class );

		final String attributeKey = "tag";

		assertThat( filter.matches( element, attributeKey ) ).isTrue();
	}

	@Test
	void should_not_match_with_attribute_key_when_attribute_is_different() {
		final AttributeFilter filter = new AttributeFilter( "tag" );

		final Element element = mock( Element.class );
		final IdentifyingAttributes identAttribs = mock( IdentifyingAttributes.class );
		when( element.getIdentifyingAttributes() ).thenReturn( identAttribs );
		when( identAttribs.get( "type" ) ).thenReturn( "div" );
		final Attributes attribs = mock( Attributes.class );
		when( element.getAttributes() ).thenReturn( attribs );
		when( attribs.get( "mySpecialAttribute" ) ).thenReturn( "someValue" );

		final String attributeKey = "mySpecialAttribute";

		assertThat( filter.matches( element, attributeKey ) ).isFalse();
	}
}
