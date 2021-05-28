package de.retest.recheck.ui.actions;

import java.io.Serializable;

import de.retest.recheck.ui.descriptors.SutState;
import de.retest.recheck.ui.review.ActionChangeSet;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType( XmlAccessType.FIELD )
public class ActionState implements Serializable {

	private static final long serialVersionUID = 2L;

	// resulting state
	@XmlElement
	private final SutState state;

	@XmlElement
	private final long duration;

	@XmlAnyElement( lax = true )
	private final Action action;

	@SuppressWarnings( "unused" )
	private ActionState() {
		// for JAXB
		action = null;
		state = null;
		duration = 0l;
	}

	public ActionState( final Action action, final SutState resultingState, final long duration ) {
		if ( action == null || resultingState == null ) {
			throw new NullPointerException( "action and resultingState cannot be null." );
		}
		this.action = action;
		state = resultingState;
		this.duration = duration;
	}

	public SutState getState() {
		return state;
	}

	public Action getAction() {
		return action;
	}

	public long getDuration() {
		return duration;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (action == null ? 0 : action.hashCode());
		result = prime * result + (int) (duration ^ duration >>> 32);
		result = prime * result + (state == null ? 0 : state.hashCode());
		return result;
	}

	@Override
	public boolean equals( final Object obj ) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final ActionState other = (ActionState) obj;
		if ( action == null ) {
			if ( other.action != null ) {
				return false;
			}
		} else if ( !action.equals( other.action ) ) {
			return false;
		}
		if ( duration != other.duration ) {
			return false;
		}
		if ( state == null ) {
			if ( other.state != null ) {
				return false;
			}
		} else if ( !state.equals( other.state ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return action.toString();
	}

	public ActionState applyChanges( final ActionChangeSet actionChangeSet ) {
		final Action action = getAction().applyChanges( actionChangeSet );
		final SutState resultingState = getState().applyChanges( actionChangeSet );
		return new ActionState( action, resultingState, duration );
	}
}
