package edu.uci.ics.luci.utility.webserver.input.request;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import java.util.Map.Entry;

import edu.uci.ics.luci.utility.webserver.Channel.Protocol;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;

public class RequestTest {

	@Test
	public void testToString() {
		String command = "/alpha";
		String commandLine = "/alpha/beta";
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
		r.setCommandLine(commandLine);
		assertEquals(commandLine,r.getCommandLine());
		r.setProtocol(protocol);
		assertEquals(protocol,r.getProtocol());
		r.setSource(source);
		assertEquals(source,r.getSource());
		r.setHeaders(headers);
		assertEquals(headers,r.getHeaders());
		r.setParameters(parameters);
		assertEquals(parameters,r.getParameters());
		//System.err.println(r.toString());
		
		JSONObject ret = new JSONObject();
		JSONObject ret_ret = new JSONObject();
		ret_ret.put("source", source);
		ret_ret.put("command", command);
		ret_ret.put("command_line", commandLine);
		ret_ret.put("protocol", protocol.toString());
		
		JSONObject ret_headers = new JSONObject();
		for(Entry<String, List<String>> e: headers.entrySet()){
			JSONArray values = new JSONArray();
			for(String s: e.getValue()){
				values.add(s);
			}
			ret_headers.put(e.getKey(), values);
		}
		ret_ret.put("headers", ret_headers);
		
		JSONObject ret_parameters = new JSONObject();
		for(Entry<String, Set<String>> e: parameters.entrySet()){
			JSONArray values = new JSONArray();
			for(String s: e.getValue()){
				values.add(s);
			}
			ret_parameters.put(e.getKey(), values);
		}
		ret_ret.put("parameters", ret_parameters);
		
		ret.put("request",ret_ret);
		assertEquals(ret.toJSONString(JSONStyle.LT_COMPRESS),r.toString());
		
		/* Count how many fields we have to make sure the class hasn't changed (ignore synthetic classes such as those created by Eclemma) */
		int count = 0;
		for(Field f : Request.class.getDeclaredFields()){
			//System.err.println(f.toGenericString());
			if(!f.toGenericString().contains(".$")){
				count++;
			}
		}
		/* If this fails then the Request.toString() method needs to be updated to output the additional fields */
		assertEquals(Integer.valueOf(6),Integer.valueOf(count));
	}

}
