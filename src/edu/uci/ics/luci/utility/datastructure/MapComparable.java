/*
	Copyright 2007-2013
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

package edu.uci.ics.luci.utility.datastructure;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

public class MapComparable<K extends Comparable<? super K>,V extends Comparable<? super V>> implements Map<K, V>, Comparable<MapComparable<K,V>>,Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3411359654904301481L;
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = Logger.getLogger(MapComparable.class);
		}
		return log;
	}
	
	private AbstractMap<K, V> map;

	public MapComparable(AbstractMap<K,V> map){
		super();
		if(map == null){
			throw new NullPointerException("Can't initialize with null pointer");
		}
		else{
			this.map = map;
		}
	}

	public int compareTo(MapComparable<K, V> otherMap) {
		return compareTo((Map<K,V>)otherMap);
	}


	public int compareTo(Map<K, V> otherMap) {
		if(otherMap == null){
			return -1;
		}
		else{
			if (map.size() != otherMap.size()) {
				return (otherMap.size() - map.size());
			}
			else{
				for(java.util.Map.Entry<K, V> mapEntry: map.entrySet()){
					K mapKey = mapEntry.getKey();
					if(!otherMap.keySet().contains(mapKey)){
						return -1;
					}
					else{
						V mapValue = mapEntry.getValue();
						V otherMapValue = otherMap.get(mapKey);
						if((mapValue == null) && (otherMapValue == null)){
							//Equal so far, so continue
						}
						else if((mapValue == null) || (otherMapValue == null)){
							if(mapValue == null){
								return -1;
							}
							else{
								return 1;
							}
						}
						else{
							if (!mapValue.equals(otherMapValue)) {
								return mapValue.compareTo(otherMapValue);
							}
							else{
								//Equal so far, so continue
							}
						}
					}
				}
			}
			return (0);
		}
	}

	public boolean equals(Object otherMap) {
		if(otherMap == null){
			return false;
		}
		else{
			if(otherMap instanceof Map){
				@SuppressWarnings("unchecked")
				Map<K,V> b = (Map<K,V>) otherMap;
				return(this.compareTo(b) == 0);
			}
			else{
				return(false);
			}
		}
	}


	public void clear() {
		map.clear();
	}
	
	public Set<java.util.AbstractMap.Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	public boolean containsKey(Object arg0) {
		return map.containsKey(arg0);
	}

	public boolean containsValue(Object arg0) {
		return map.containsValue(arg0);
	}

	public V get(Object arg0) {
		return map.get(arg0);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Set<K> keySet() {
		return map.keySet();
	}

	public V put(K arg0, V arg1) {
		return map.put(arg0, arg1);
	}
	
	public V put(Pair<K,V> p){
		return map.put(p.getFirst(), p.getSecond());
	}

	public void putAll(Map<? extends K, ? extends V> arg0) {
		map.putAll(arg0);
		
	}

	public V remove(Object arg0) {
		return map.remove(arg0);
	}

	public int size() {
		return map.size();
	}

	public Collection<V> values() {
		return map.values();
	}
	
	public String toString(){
		boolean hascontent = false;
		StringBuffer ret = new StringBuffer();
		ret.append("{");
		for(java.util.AbstractMap.Entry<K, V> e:this.entrySet()){
			hascontent=true;
			ret.append("\"");
			K k = e.getKey();
			if(k != null){
				ret.append(k.toString());
			}
			else{
				ret.append("null");
			}
			ret.append("\":\"");
			V v = e.getValue();
			if(v!= null){
				ret.append(v.toString());
			}
			else{
				ret.append("null");
			}
			ret.append("\",");
		}
		if(hascontent){
			ret.delete(ret.length()-1, ret.length());
		}
		ret.append("}");
		return(ret.toString());
	}
}
