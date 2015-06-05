package edu.uci.ics.luci.utility.webserver.input.request;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import edu.uci.ics.luci.utility.webserver.Channel.Protocol;

public class RequestTest {

	@Test
	public void testToString() {
		String command = "foo";
		Protocol protocol = Protocol.HTTP_GET;
		String source= "127.0.0.1";
		Map<String, List<String>> headers = new HashMap<String,List<String>>();
		ArrayList<String> arrayList1 = new ArrayList<String>();
		arrayList1.add("a");
		arrayList1.add("bb");
		headers.put("one", arrayList1);
		ArrayList<String> arrayList2 = new ArrayList<String>();
		arrayList2.add("ccc");
		arrayList2.add("dddd");
		headers.put("two", arrayList2);
		Map<String, Set<String>> parameters = new HashMap<String,Set<String>>();
		Set <String> hashset1 = new HashSet<String>();
		hashset1.add("eeeee");
		hashset1.add("ffffff");
		parameters.put("three", hashset1);
		Set <String> hashset2 = new HashSet<String>();
		hashset2.add("ggggggg");
		hashset2.add("hhhhhhhh");
		parameters.put("four", hashset2);
		Request r = new Request();
		r.setCommand(command);
		assertEquals(command,r.getCommand());
		r.setProtocol(protocol);
		assertEquals(protocol,r.getProtocol());
		r.setSource(source);
		assertEquals(source,r.getSource());
		r.setHeaders(headers);
		assertEquals(headers,r.getHeaders());
		r.setParameters(parameters);
		assertEquals(parameters,r.getParameters());
		//System.err.println(r.toString());
		String output =	"{\"request\":\n"+
						"\t{\"source\":\"127.0.0.1\",\n"+
						"\t \"command\":\"foo\",\n"+
					   	"\t \"protocol\":\"HTTP_GET\",\n"+
						"\t \"headers\":\n"+
						"\t\t{\"two\":\n"+
						"\t\t\t[\"ccc\",\n"+
						"\t\t\t \"dddd\"\n" +
						"\t\t\t],\n"+
						"\t\t \"one\":\n"+
						"\t\t\t[\"a\",\n"+
						"\t\t\t \"bb\"\n" +
						"\t\t\t]\n"+
						"\t\t},\n"+
						"\t \"parameters\":\n"+
						"\t\t{\"three\":\n"+
						"\t\t\t[\"eeeee\",\n"+
						"\t\t\t \"ffffff\"\n" +
						"\t\t\t],\n"+
						"\t\t \"four\":\n"+
						"\t\t\t[\"ggggggg\",\n"+
						"\t\t\t \"hhhhhhhh\"\n" +
						"\t\t\t]\n"+
						"\t\t}\n"+
						"\t}\n"+
						"}";
		assertEquals(output,r.toString());
		
		/* Count how many fields we have to make sure the class hasn't changed (ignore synthetic classes such as those created by Eclemma) */
		int count = 0;
		for(Field f : Request.class.getDeclaredFields()){
			//System.err.println(f.toGenericString());
			if(!f.toGenericString().contains(".$")){
				count++;
			}
		}
		/* If this fails then the Request.toString() method needs to be updated to output the additional fields */
		assertEquals(Integer.valueOf(5),Integer.valueOf(count));
	}

}
