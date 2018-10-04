/*
	Copyright 2007-2018
		Donald J. Patterson 
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

package edu.uci.ics.luci.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Shutdown implements Runnable {
	
	/* Class logger */
	private static transient volatile Logger log = null;
	public static Logger getLog() {
		if (log == null) {
			log = LogManager.getLogger(Shutdown.class);
		}
		return log;
	}
	
	/* Class variables */
	private Object firstShutdown = new Object();
	private List<Quittable> quittables = null;

	/**
	 * Constructor
	 * @param q, a list of quittables that should be shutdown, possibly empty at start
	 */
	public Shutdown(List<Quittable> q) {
		super();
		this.quittables = Collections.synchronizedList(new ArrayList<Quittable>());
		/* Doing this rather than addAll to make sure no nulls are added to the list */
		for(Quittable qq: q){
			this.add(qq);
		}
	}
	
	/**
	 * Constructor that makes a shutdown object that is initialized with nothing to shut
	 * down
	 */
	public Shutdown() {
		this(new ArrayList<Quittable>());
	}

	/**
	 * Add something to the list of things that should be shutdown
	 * @param q
	 */
	public void add(Quittable q) {
		if(q != null){
			this.quittables.add(q);
		}
	}

	/**
	 * A method for runnable such that, when executed, everything that should be
	 * shutdown is giving the opportunity to do so.
	 * Shutdown order is arbitrary
	 */
	public synchronized void run() {
		if(firstShutdown != null){
			synchronized(firstShutdown) {
				getLog().debug("MyShutdown shutting down");
				synchronized(quittables) {
					for (Quittable q : quittables) {
						synchronized (q) {
							if (!q.isQuitting()) {
								q.setQuitting(true);
								q.notifyAll();
							}
						}
					}
				}	
			}
			getLog().debug("Done shutting down");
			firstShutdown = null;
		}
	}
}
