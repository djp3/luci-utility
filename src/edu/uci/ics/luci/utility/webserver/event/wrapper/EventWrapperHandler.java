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


package edu.uci.ics.luci.utility.webserver.event.wrapper;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.event.Event;
import edu.uci.ics.luci.utility.webserver.event.result.EventResult;
import edu.uci.ics.luci.utility.webserver.event.resultlistener.EventResultListener;


public class EventWrapperHandler implements com.lmax.disruptor.EventHandler<EventWrapper> {
	
	private static final String ERROR_EVENTWRAPPER_CANT_BE_NULL = "Event Wrapper can't be null";

	/*******************************************/
	private static transient volatile Logger log = null;
	public static Logger getLog() {
		if (log == null) {
			log = LogManager.getLogger(EventWrapperHandler.class);
		}
		return log;
	}
	/*******************************************/

	
	private ExecutorService executor;
	
	public EventWrapperHandler(ExecutorService e){
		this.executor = e;
	}
	
	private static class MyHandlerWrapper implements Callable<EventResult>{
		
		private static transient volatile Logger log = null;
		public static Logger getLog() {
			if (log == null) {
				log = LogManager.getLogger(MyHandlerWrapper.class);
			}
			return log;
		}

		private Event event;
		private List<EventResultListener> resultListeners;
		

		public MyHandlerWrapper(Event event, List<EventResultListener> resultListeners) {
			this.event = event;
			this.resultListeners = resultListeners;
		}


		@Override
		public EventResult call() throws Exception {
			try{
				EventResult result = this.event.onEvent();
				for(EventResultListener rl: this.resultListeners){
					try{
						rl.onFinish(result);
					} catch(RuntimeException e){
						getLog().error(e);
					}
				}
				
				return(result);
				
			}catch (Exception e){
				getLog().error(e);
				e.printStackTrace();
				throw e;
			}
		}
		
	}

	
	@Override
	public void onEvent(EventWrapper eventWrapper, long sequence, boolean endOfBatch) {
		if(eventWrapper == null){
			throw new IllegalArgumentException(ERROR_EVENTWRAPPER_CANT_BE_NULL);
		}
		executor.submit(new MyHandlerWrapper(eventWrapper.getEvent(),eventWrapper.getResultListeners()));
	}
}
