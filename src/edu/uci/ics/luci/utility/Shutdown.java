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

package edu.uci.ics.luci.utility;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Shutdown implements Runnable {

	private boolean firstShutdown = true;
	private List<Quittable> quittables = null;

	private static transient volatile Logger log = null;

	public static Logger getLog() {
		if (log == null) {
			log = LogManager.getLogger(Shutdown.class);
		}
		return log;
	}

	public Shutdown(List<Quittable> q) {
		super();
		this.quittables = new ArrayList<Quittable>();
		this.quittables.addAll(q);
	}

	public synchronized void add(Quittable q) {
		if(this.quittables == null){
			this.quittables = new ArrayList<Quittable>();
		}
		if(q != null){
			this.quittables.add(q);
		}
	}

	public synchronized void run() {
		if(firstShutdown){
			getLog().debug("MyShutdown shutting down");
		}
		if (quittables != null) {
			try {
				for (Quittable q : quittables) {
					if (q != null) {
						synchronized (q) {
							if (!q.isQuitting()) {
								q.setQuitting(true);
								q.notifyAll();
							}
						}
					}
				}
			} finally {
				quittables = null;
			}
		}
		if(firstShutdown){
			getLog().debug("Done shutting down");
		}
		firstShutdown = false;
	}
}
