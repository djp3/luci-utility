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

/**
 * This extends the GlobalsTest class and is supposed to be used for various Test cases that need a Globals
 * @author djp3
 *
 */
public class GlobalsForTesting extends GlobalsTest {
	
	
	public static void reset(String log4jFileName){
		GlobalsTest.reset(log4jFileName);
		//"testSupport/GlobalsForTesting.log4j.xml");
	}
	
	/**
	 * This method provides access to the global singleton if it is of type GlobalsForTesting
	 * @return the current GlobalsForTesting singleton object or null if it has not been set or is not a GlobalsForTesting implementation
	 */
	public static GlobalsForTesting getGlobalsForTesting(){
		Globals singleton = Globals.getGlobals();
		if(singleton == null) {
			return null;
		}
		else {
			if(singleton instanceof GlobalsForTesting) {
				return (GlobalsForTesting)singleton;
			}
			else {
				throw new IllegalStateException("The Globals object is not of type GlobalsForTesting, it is "+singleton.getClass());
			}
		}	
	}
	
	
	public GlobalsForTesting(){
		super();
		this.setTesting(true);
	}
	
	

}
