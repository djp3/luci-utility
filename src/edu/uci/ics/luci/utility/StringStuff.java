/*
	Copyright 2007-2015
		University of California, Irvine (c/o Donald J. Patterson)
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

import java.util.List;

public class StringStuff {
	public static String join(String delimiter, List<String> strings) {
		StringBuffer joinedString = new StringBuffer();
		for (String s : strings){
			joinedString.append(s);
			joinedString.append(delimiter);
		}
		joinedString.delete(joinedString.length()-delimiter.length(), joinedString.length());
		return joinedString.toString();
	}
	
	public static String repeatString(String s, int numberOfRepeats) {
		Integer foo=null;
		if(foo==null){
			foo = 1;
		}
		StringBuffer repeatString = new StringBuffer();
		for (int i=0; i<numberOfRepeats; ++i){
			repeatString.append(s);
		}
		return repeatString.toString();
	}
}
