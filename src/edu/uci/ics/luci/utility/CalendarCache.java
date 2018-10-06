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

package edu.uci.ics.luci.utility;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class CalendarCache {
	public transient static final TimeZone TZ_LosAngeles = TimeZone.getTimeZone("America/Los_Angeles");
	public transient static final TimeZone TZ_UTC = TimeZone.getTimeZone("UTC");
	
	private transient static String defaultTimeZoneS;
	private transient static TimeZone defaultTimeZoneTZ;
	
	private transient static Map<String,Calendar> cache;
	
	/****** Singleton *******/
	private static final Object lock = new Object();
	private static CalendarCache calendarCache;
	
	public static CalendarCache getCalendarCache(){
		synchronized(lock) {
			if(calendarCache == null) {
				return new CalendarCache();
			}
			else {
				return calendarCache;
			}
		}
	}
	
	private CalendarCache(){
		synchronized(lock) {
			reset();
			calendarCache = this;
		}
	};
	
	public static void resetDefaults() {
		synchronized(lock) {
			defaultTimeZoneS = TZ_UTC.getID();
			defaultTimeZoneTZ = TZ_UTC;
		}
	}
	
	public static void resetCache() {
		synchronized(lock) {
			cache = Collections.synchronizedMap(new HashMap<String,Calendar>());
		}
	}
	
	public static void reset() {
		synchronized(lock) {
			resetDefaults();
			resetCache();
		}
	}
	
	
	
	/***********************/
	
	public static String getDefaultTimeZoneS() {
		synchronized(lock) {
			return defaultTimeZoneS;
		}
	}
	
	public static TimeZone getDefaultTimeZoneTZ() {
		synchronized(lock) {
			return defaultTimeZoneTZ;
		}
	}
	
	private static TimeZone findTimeZone(String tz) {
		for(String ctz:TimeZone.getAvailableIDs()){
			if(tz.equals(ctz)){
				return TimeZone.getTimeZone(tz);
			}
		}
		return null;
	}

	public static void setDefaultTimeZone(String defaultTimeZone) {
		TimeZone n = findTimeZone(defaultTimeZone);
		if(n != null) {
			synchronized(lock) {
				defaultTimeZoneS = defaultTimeZone;
				defaultTimeZoneTZ = n;
			}
		}
		else {
			throw new IllegalArgumentException("Could not resolve string:"+defaultTimeZone+" to a TimeZone");
		}
	}


	/**
	 * Get a particular calendar for a time zone from the cache
	 * @param tz The string description of the Calendar's time zone.  Null or "" to get the default.
	 * @return The cached calendar corresponding to @param tz
	 */
	public Calendar getCalendar(String tz) {
		if((tz == null)||(tz.length()==0)){
			tz = defaultTimeZoneS;
		}
		
		/* Get a calendar. Check cache first */
		Calendar c = cache.get(tz);
		if(c == null){
			TimeZone usersTimeZone = findTimeZone(tz);
			if(usersTimeZone != null) {
				c = Calendar.getInstance(usersTimeZone);
				cache.put(tz,(Calendar) c.clone());
			}
			else {
				throw new IllegalArgumentException("Could not resolve string:"+tz+" to a TimeZone");
			}
		}
		return c;
	}
	
	public Calendar getCalendar() {
		return getCalendar(null);
	}
}
