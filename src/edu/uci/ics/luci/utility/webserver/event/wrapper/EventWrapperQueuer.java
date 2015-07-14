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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import edu.uci.ics.luci.utility.Quittable;

public class EventWrapperQueuer implements Quittable {

	/*******************************************/
	private static transient volatile Logger log = null;
	public static Logger getLog() {
		if (log == null) {
			log = LogManager.getLogger(EventWrapperQueuer.class);
		}
		return log;
	}
	/*******************************************/
	

	private final RingBuffer<EventWrapper> ringBuffer;
	private final Disruptor<EventWrapper> disruptor;
	private final BufferedOutputStream logWriter;
	
	
	/** Quittable interface */
	
	private boolean quitting = false;

	@Override
	public void setQuitting(boolean quitting) {
		if (this.quitting && !quitting) {
			getLog().warn("Already quitting, can't unquit");
		} else {
			if (quitting) {
				this.quitting = quitting;
				if (logWriter != null) {
					try {
						logWriter.close();
					} catch (IOException e) {
					}
				}
				if (disruptor != null) {
					disruptor.shutdown();
				}
			}
		}
	}
	

	@Override
	public boolean isQuitting() {
		return quitting;
	}
	

	/**
	 * Constructor that doesn't log to a file
	 * 
	 * @param ringBuffer
	 */
	public EventWrapperQueuer(Disruptor<EventWrapper> disruptor, RingBuffer<EventWrapper> ringBuffer) {
		this(disruptor, ringBuffer, null);
	}
	

	/**
	 * Constructor that logs to a file
	 * 
	 * @param ringBuffer
	 * @param logFileName
	 */
	public EventWrapperQueuer(Disruptor<EventWrapper> disruptor, RingBuffer<EventWrapper> ringBuffer, File logFile) {
		
		if (disruptor == null) {
			getLog().fatal("disruptor can't be null");
			throw new IllegalArgumentException("disruptor can't be null");
		}
		this.disruptor = disruptor;

		
		if (ringBuffer == null) {
			getLog().fatal("ringBuffer can't be null");
			throw new IllegalArgumentException("ringBuffer can't be null");
		}
		this.ringBuffer = ringBuffer;

		
		/* Try and set up a file logger */
		BufferedOutputStream _logWriter = null;
		try{
			if(logFile != null){
				_logWriter = new BufferedOutputStream(new FileOutputStream(logFile));
			}
			else{
				_logWriter = null;
			}
		}catch (FileNotFoundException e) {
			getLog().error(e);
		}finally{
			logWriter = _logWriter;
		}

	}

	
	private static final EventTranslatorOneArg<EventWrapper, EventWrapper> TRANSLATOR = new EventTranslatorOneArg<EventWrapper, EventWrapper>() {
		public void translateTo(EventWrapper eventWrapper, long sequence, EventWrapper incoming) {
			eventWrapper.set(incoming);
		}
	};
	

	public void onData(EventWrapper incoming) {
		if (!isQuitting()) {
			/* Write event to log */
			if (logWriter != null) {
				try {
					logWriter.write(incoming.toString().getBytes());
					logWriter.write("\n".getBytes());
					logWriter.flush();
				} catch (IOException exception) {
					getLog().error(
							"Error writing event to log file:"
									+ incoming.getClass().getCanonicalName());
				}
			}

			/* Submit event for handling */
			ringBuffer.publishEvent(TRANSLATOR, incoming);
		}
	}
	

}
