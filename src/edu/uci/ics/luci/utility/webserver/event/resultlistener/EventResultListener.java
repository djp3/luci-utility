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


package edu.uci.ics.luci.utility.webserver.event.resultlistener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.event.result.EventResult;

public abstract class EventResultListener {
	
	//private static String ERROR_NO_ZERO_PARAMETERS = "No zero parameter constructors available";
	protected String ERROR_UNABLE_TO_HANDLE_WEB_EVENT_RESULT_LISTENER = "Unable to handle this kind of WebEventHandlerResultListener";
	
	private static transient volatile Logger log = null;

	public static Logger getLog() {
		if (log == null) {
			log = LogManager.getLogger(EventResultListener.class);
		}
		return log;
	}

	
	/*
	public static EventResultListener instantiate(String type) {
    	try {
   	    	@SuppressWarnings("unchecked")
			Class<? extends EventHandlerResultListener> c = (Class<? extends EventResultListener >) Class.forName(type);
   	    	Constructor<?>[] allConstructors = c.getDeclaredConstructors();
   	    	Constructor<?> constructor = null;
   	    	for (Constructor<?> ctor : allConstructors) {
   	    		if (ctor.getGenericParameterTypes().length == 0){
   	    			constructor = ctor;
   	    			break;
   	    		}
   	    	}
   	    	if(constructor == null){
   	    		throw new IllegalArgumentException(ERROR_NO_ZERO_PARAMETERS+" for class "+type);
   	    	}
   	    	else{
   	    		try {
   	    			constructor.setAccessible(true);
   	    			WebEventHandlerResultListener w = (WebEventHandlerResultListener)constructor.newInstance();
   	    			return(w);
   	    		} catch (InstantiationException x) {
   	    			getLog().error(x);
   	    		} catch (InvocationTargetException x) {
   	    			getLog().error(x);
   	    		} catch (IllegalAccessException x) {
   	    			getLog().error(x);
   	    		}
   	    	}
   	    } catch (ClassNotFoundException x) {
   			getLog().error(x);
   	    }
    	return null;
	}
	*/
	
/*	
	public JSONObject toJSON() {
		JSONObject ret = new JSONObject();
		ret.put("event_handler_result_listener_type", this.getClass().getCanonicalName());
		return ret;
	}
	

	static public WebEventHandlerResultListener fromJSON(JSONObject in) {
		String _type = (String) in.get("event_handler_result_listener_type");
		WebEventHandlerResultListener we = WebEventHandlerResultListener.instantiate(_type);
		we.initializeFromJSON(in);
		return(we);
	}
	
	public abstract void initializeFromJSON(JSONObject in);
	*/
	
	public abstract void onFinish(EventResult result);

}
