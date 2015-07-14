package edu.uci.ics.luci.utility.webserver.event.result.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpStatus;

import edu.uci.ics.luci.utility.webserver.output.channel.Output;

public class APIEventResult_HTTP extends APIEventResult {

	public APIEventResult_HTTP() {
		this(null,null,null,null);
	}
	
	public APIEventResult_HTTP(Status status, DataType responseDataType, String responseBody,Output output) {
		super(status, responseDataType, responseBody,output);
	}

	/*
	 * Convenience functions that generate headers for various kinds of
	 * responses
	 */
	public static Map<String, Set<String>> getContentTypeHeader_JSON() {
		HashMap<String, Set<String>> ret = new HashMap<String, Set<String>>();
		Set<String> set = new HashSet<String>();
		set.add("application/json");
		set.add("charset=UTF-8");
		ret.put("Content-type", set);
		return ret;
	}

	public static HashMap<String, Set<String>> getContentTypeHeader_HTML() {
		HashMap<String, Set<String>> ret = new HashMap<String, Set<String>>();
		Set<String> set = new HashSet<String>();
		set.add("text/html");
		set.add("charset=UTF-8");
		ret.put("Content-type", set);
		return ret;
	}

	public static HashMap<String, Set<String>> getContentTypeHeader_CSS() {
		HashMap<String, Set<String>> ret = new HashMap<String, Set<String>>();
		Set<String> set = new HashSet<String>();
		set.add("text/css");
		set.add("charset=UTF-8");
		ret.put("Content-type", set);
		return ret;
	}

	public static HashMap<String, Set<String>> getContentTypeHeader_JS() {
		HashMap<String, Set<String>> ret = new HashMap<String, Set<String>>();
		Set<String> set = new HashSet<String>();
		set.add("text/javascript");
		set.add("charset=UTF-8");
		ret.put("Content-type", set);
		return ret;
	}

	public static HashMap<String, Set<String>> getContentTypeHeader_PNG() {
		HashMap<String, Set<String>> ret = new HashMap<String, Set<String>>();
		Set<String> set = new HashSet<String>();
		set.add("image/png");
		set.add("charset=UTF-8");
		ret.put("Content-type", set);
		return ret;
	}

	private int httpStatus;
	private Map<String, Set<String>> httpHeaders = new HashMap<String, Set<String>>();

	@Override
	public void setStatus(Status status) {
		super.setStatus(status);
		if(status != null){
			if (status.equals(APIEventResult.Status.OK)) {
				setHttpStatus(HttpStatus.SC_OK);
			} else if (status.equals(APIEventResult.Status.REDIRECT)) {
				setHttpStatus(HttpStatus.SC_TEMPORARY_REDIRECT);
			} else if (status.equals(APIEventResult.Status.PROXY)) {
				setHttpStatus(HttpStatus.SC_USE_PROXY);
			} else if (status.equals(APIEventResult.Status.NOT_FOUND)) {
				setHttpStatus(HttpStatus.SC_NOT_FOUND);
			} else {
				throw new IllegalArgumentException("Unhandled case: "
						+ status.toString());
			}
		}
	}

	@Override
	public void setDataType(DataType dataType) {
		super.setDataType(dataType);
		if(dataType != null){
			if (dataType.equals(APIEventResult.DataType.JSON)) {
				updateHttpHeaders(getContentTypeHeader_JSON());
			} else if (dataType.equals(APIEventResult.DataType.HTML)) {
				updateHttpHeaders(getContentTypeHeader_HTML());
			} else if (dataType.equals(APIEventResult.DataType.CSS)) {
				updateHttpHeaders(getContentTypeHeader_CSS());
			} else if (dataType.equals(APIEventResult.DataType.PNG)) {
				updateHttpHeaders(getContentTypeHeader_PNG());
			} else if (dataType.equals(APIEventResult.DataType.JAVASCRIPT)) {
				updateHttpHeaders(getContentTypeHeader_JS());
			} else {
				throw new IllegalArgumentException("Unhandled case: "
						+ dataType.toString());
			}
		}
	}

	public int getHttpStatus() {
		return httpStatus;
	}

	private void setHttpStatus(int httpStatus) {
		this.httpStatus = httpStatus;
	}

	public Map<String, Set<String>> getHttpHeaders() {
		return httpHeaders;
	}

	public void setHttpHeaders(Map<String, Set<String>> httpHeaders) {
		this.httpHeaders = httpHeaders;
	}

	public void updateHttpHeaders(Map<String, Set<String>> headers) {
		for (Entry<String, Set<String>> e : headers.entrySet()) {
			Set<String> set = null;
			if (getHttpHeaders().containsKey(e.getKey())) {
				set = getHttpHeaders().get(e.getKey());
			} else {
				set = new HashSet<String>();
			}
			set.addAll(e.getValue());
			getHttpHeaders().put(e.getKey(), set);
		}
	}

}
