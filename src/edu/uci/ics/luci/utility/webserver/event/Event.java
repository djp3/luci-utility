/*
	Copyright 2007-2018
		Donald J. Patterson 
*/
/*
	This file is part of the Laboratory for Ubiquitous Computing java Utility package, i.e. "Utilities"

    Utilities is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Utilities is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Utilities.  If not, see <http://www.gnu.org/licenses/>.
*/


package edu.uci.ics.luci.utility.webserver.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.event.result.EventResult;

public abstract class Event{
	
	private static transient volatile Logger log = null;
	public static Logger getLog() {
		if (log == null) {
			log = LogManager.getLogger(Event.class);
		}
		return log;
	}
	
	public static final String ERROR_TYPE_MISMATCH = "Internal error, event type mismatch";
	public static final String ERROR_PARAMETERS_NOT_CHECKED = "Parameters were not checked before calling onEvent";
	public static final String ERROR_GLOBALS_NULL = "Global variables have not been initialized";
	
	protected static final String ERROR_UNABLE_TO_HANDLE_WEB_EVENT = "Unable to handle web event";
	protected static final String ERROR_SET_ENCOUNTERED_TYPE_MISMATCH = "Trying to set Event failed due to type mismatch";
	
	
	
	/* Constructors */
	public Event(){
		super();
	}
	
	
	
	/**
	 * This method should make the existing event equivalent to the event that is passed.
	 * It is basically cloning into an existing event.
	 * @param copyMe
	 * @return
	 */
	public abstract void set(Event copyMe);
	
	/**
	 * This does the work that the event is designed to do.
	 * @return
	 */
	public abstract EventResult onEvent();

}
