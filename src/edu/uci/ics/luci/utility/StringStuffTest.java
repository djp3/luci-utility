package edu.uci.ics.luci.utility;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class StringStuffTest {
	@Test
	public void testJoin() {
		List<String> list1 = new ArrayList<String>();
		list1.add("A man");
		list1.add("a plan");
		list1.add("a canal");
		list1.add("Panama");
		assertEquals(StringStuff.join(", ", list1), "A man, a plan, a canal, Panama");
		assertEquals(StringStuff.join("", list1), "A mana plana canalPanama");
		
		List<String> list2 = new ArrayList<String>();
		list2.add("");
		list2.add(" ");
		list2.add("123");
		list2.add("");
		list2.add("");
		list2.add("456");
		list2.add("");
		assertEquals(StringStuff.join(", ", list2), ",  , 123, , , 456, ");
		assertEquals(StringStuff.join("", list2), " 123456");
		
		List<String> list3 = new ArrayList<String>();
		list3.add("ABC");
		list3.add("DEF");
		list3.add("GHI");
		list3.add("JKL");
		assertEquals(StringStuff.join("\n", list3), "ABC\nDEF\nGHI\nJKL");
	}

	@Test
	public void testRepeatString() {
		assertEquals(StringStuff.repeatString("ABC", 4), "ABCABCABCABC");
		assertEquals(StringStuff.repeatString("", 4), "");
		assertEquals(StringStuff.repeatString("ABC, ", 4), "ABC, ABC, ABC, ABC, ");
		assertEquals(StringStuff.repeatString("ABC, ", 0), "");
	}
}
