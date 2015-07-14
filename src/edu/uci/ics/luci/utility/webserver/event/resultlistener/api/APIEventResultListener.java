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

package edu.uci.ics.luci.utility.webserver.event.resultlistener.api;

import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONArray;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.event.result.EventResult;
import edu.uci.ics.luci.utility.webserver.event.result.api.APIEventResult;
import edu.uci.ics.luci.utility.webserver.event.resultlistener.EventResultListener;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;

//public class WebEventHandlerResultListener_Dispatch_Server extends WebEventHandlerResultListener {
public class APIEventResultListener extends EventResultListener {

	private static transient volatile Logger log = null;
	public static Logger getLog() {
		if (log == null) {
			log = LogManager
					.getLogger(APIEventResultListener.class);
		}
		return log;
	}

	public void onFinish(EventResult _result) {
		
		boolean error = false;
		List<String> errors = new ArrayList<String>();

		APIEventResult result = null;
		if (_result instanceof APIEventResult) {
			result = (APIEventResult) _result;
		} else {
			error = true;
			String e = ERROR_UNABLE_TO_HANDLE_WEB_EVENT_RESULT_LISTENER + ","
							+ this.getClass().getCanonicalName()
							+ " can't handle event of type "
							+ _result.getClass().getCanonicalName();
			errors.add(e);
			getLog().error(e);
		}


		if((!error) && (result != null)) {
			Output output = result.getOutput();
			try{
				if (result.getStatus() == APIEventResult.Status.OK) {
					if (result.getResponseBody() == null) {
						errors.add("Request Handler returned null response to this request\n"
								+ result.toString());
						error = true;
					}

					if (error) {
						result.setDataType(APIEventResult.DataType.JSON);

						JSONArray jsonArray = new JSONArray();
						jsonArray.addAll(errors);
						result.setResponseBody(jsonArray.toString());
					}
					getLog().info("Sending back a response the wire:\n"+result.getResponseBody());
					output.send_OK(result);
				} else {
					if (result.getStatus() == APIEventResult.Status.REDIRECT) {
						output.send_Redirect(result);
					} else {
						output.send_Proxy(result);
					}
				}
			} finally {
				if (output != null) {
					output.closeChannel();
				}
			}
		}
	}
}
