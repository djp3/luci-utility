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
