package edu.uci.ics.luci.utility;

import java.util.List;

public class StringStuff {
	public static String join(String delimiter, List<String> strings) {
		String joinedString = "";
		for (String s : strings){
			joinedString += s + delimiter;
		}
		return joinedString.substring(0, joinedString.length() - delimiter.length());
	}
	
	public static String repeatString(String s, int numberOfRepeats) {
		String repeatString = "";
		for (int i=0; i<numberOfRepeats; ++i){
			repeatString += s;
		}
		return repeatString;
	}
}
