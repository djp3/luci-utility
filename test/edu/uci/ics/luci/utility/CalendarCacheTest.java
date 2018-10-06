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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;

public class CalendarCacheTest {
	
	

	@Test
	public void testCalendarCache() {
		CalendarCache cc = CalendarCache.getCalendarCache();
		String tz = "America/Los_Angeles";
		
		GregorianCalendar cal = new GregorianCalendar(CalendarCache.TZ_LosAngeles);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		
		assertEquals(hour,cc.getCalendar(tz).get(Calendar.HOUR_OF_DAY));
		
		cal = new GregorianCalendar(CalendarCache.TZ_UTC);
		hour = cal.get(Calendar.HOUR_OF_DAY);
		
		assertTrue(hour != cc.getCalendar(tz).get(Calendar.HOUR_OF_DAY));
	}
	
	@Test
	public void testDefaultTimeZone() {
		CalendarCache.resetDefaults();
		assertEquals(CalendarCache.getDefaultTimeZoneTZ(),TimeZone.getTimeZone("UTC"));
		assertEquals(CalendarCache.getDefaultTimeZoneS(),TimeZone.getTimeZone("UTC").getID());
		CalendarCache.setDefaultTimeZone(CalendarCache.TZ_LosAngeles.getID());
		assertEquals(CalendarCache.getDefaultTimeZoneTZ(),TimeZone.getTimeZone("America/Los_Angeles"));
		assertEquals(CalendarCache.getDefaultTimeZoneS(),TimeZone.getTimeZone("America/Los_Angeles").getID());
	}
	
	@Test
	public void testCalendarCacheDefaults() {
		/* Make sure the default is working */
		CalendarCache cc = CalendarCache.getCalendarCache();
		GregorianCalendar cal = new GregorianCalendar(CalendarCache.getDefaultTimeZoneTZ());
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		assertEquals(hour,cc.getCalendar(CalendarCache.getDefaultTimeZoneS()).get(Calendar.HOUR_OF_DAY));
	}
	
	@Test
	public void testCalendarCacheCustomDefaults() {
		/* Make sure setting the default matters */
		CalendarCache.setDefaultTimeZone("Africa/Kampala");
		CalendarCache.reset();
		CalendarCache cc = CalendarCache.getCalendarCache();
		GregorianCalendar cal = new GregorianCalendar(CalendarCache.getDefaultTimeZoneTZ());
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		assertEquals(hour,cc.getCalendar(CalendarCache.getDefaultTimeZoneS()).get(Calendar.HOUR_OF_DAY));
	}
	
	@Test
	public void testGetCalendarDefault() {
		CalendarCache cc = CalendarCache.getCalendarCache();
		Calendar a = cc.getCalendar();
		Calendar b = cc.getCalendar(null);
		Calendar c = cc.getCalendar("");
		Calendar d = cc.getCalendar("Africa/Kampala");
		assertNotEquals(a,d);
		assertNotEquals(b,d);
		assertNotEquals(c,d);
		assertEquals(a,b);
		assertEquals(b,c);
	}
	
	@Test
	public void testGetCalendarSimple() {
		CalendarCache cc = CalendarCache.getCalendarCache();
		Calendar c = cc.getCalendar("Africa/Kampala");
		assertNotNull(c);
	}
	
	@Test
	public void testGetCalendarBad() {
		CalendarCache cc = CalendarCache.getCalendarCache();
		assertThrows(IllegalArgumentException.class,()->{
			cc.getCalendar("Brigadoon");
		});
	}
	
	@Test
	public void testGetCalendarString() {
		int howMany = 1000;
		CalendarCache cc = CalendarCache.getCalendarCache();
		
		long start,middle,end;
		
		start = System.currentTimeMillis();
		for(int i = 0 ; i < howMany; i++){
			Calendar.getInstance(CalendarCache.TZ_UTC);
		}
		
		middle = System.currentTimeMillis();
		
		for(int i = 0 ; i < howMany; i++){
			cc.getCalendar(CalendarCache.TZ_UTC.getID());
		}
		
		end = System.currentTimeMillis();
		
		//System.out.println("middle-start:"+(middle-start)+",end-middle:"+(end-middle));
		assertTrue((middle-start)>(end-middle));
	}


		
		
	
	@Test
	public void testClear() {
		int howMany = 1000;
		CalendarCache cc = CalendarCache.getCalendarCache();
		
		long start,middle,end;
		
		start = System.currentTimeMillis();
		for(int i = 0 ; i < howMany; i++){
			Calendar.getInstance(CalendarCache.TZ_UTC);
		}
		
		middle = System.currentTimeMillis();
		
		for(int i = 0 ; i < howMany; i++){
			cc.getCalendar(CalendarCache.TZ_UTC.getID());
			CalendarCache.reset();
		}
		
		end = System.currentTimeMillis();
		
		//System.out.println("middle-start:"+(middle-start)+",end-middle:"+(end-middle));
		assertTrue((middle-start)<(end-middle));
	}
	
	@Test
	public void testSetBadDefaultTZ() {
		assertThrows(IllegalArgumentException.class,()->{
			CalendarCache.setDefaultTimeZone("Brigadoon");
		});
	}
	

}
