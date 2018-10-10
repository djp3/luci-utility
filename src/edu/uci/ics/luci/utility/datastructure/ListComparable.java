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

package edu.uci.ics.luci.utility.datastructure;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ListComparable<T extends Comparable<? super T>> implements List<T>, Comparable<ListComparable<T>>,Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -723364735661202960L;
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(ListComparable.class);
		}
		return log;
	}
	
	private AbstractList<T> list;

	/** Consider whether you should be using a SortedList **/
	public ListComparable(AbstractList<T> list) {
		super();
		if(list == null){
			throw new NullPointerException("Can't initialize with null pointer");
		}
		else{
			this.list = list;
		}
	}
	
	public int compareTo(ListComparable<T> otherList){
		return compareTo((List<T>) otherList);
	}

	public int compareTo(List<T> otherList) {
		if(otherList == null){
			return -1;
		}
		else{
			if(list.size() != otherList.size()){
				return(otherList.size() -list.size());
			}
			else{
				for(T x: list){
					if(!otherList.contains(x)){
						return -1;
					}
					else{
						//Equal so far, so continue
					}
				}
			}
		}
		return 0;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + list.hashCode();
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		ListComparable<T> other;
		if (obj instanceof ListComparable) {
			other = (ListComparable<T>) obj;
		}
		else{
			return false;
		}
		
		return(this.compareTo(other) == 0);
	}
	


	public boolean add(T arg0) {
		return list.add(arg0);
	}

	public void add(int arg0, T arg1) {
		list.add(arg0,arg1);
	}

	public boolean addAll(Collection<? extends T> arg0) {
		return list.addAll(arg0);
	}

	public boolean addAll(int arg0, Collection<? extends T> arg1) {
		return list.addAll(arg0,arg1);
	}

	public void clear() {
		list.clear();
	}

	public boolean contains(Object arg0) {
		return list.contains(arg0);
	}

	public boolean containsAll(Collection<?> arg0) {
		return list.containsAll(arg0);
	}

	public T get(int arg0) {
		return list.get(arg0);
	}

	public int indexOf(Object arg0) {
		return list.indexOf(arg0);
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public Iterator<T> iterator() {
		return list.iterator();
	}

	public int lastIndexOf(Object arg0) {
		return list.lastIndexOf(arg0);
	}

	public ListIterator<T> listIterator() {
		return list.listIterator();
	}

	public ListIterator<T> listIterator(int arg0) {
		return list.listIterator(arg0);
	}

	public boolean remove(Object arg0) {
		return list.remove(arg0);
	}

	public T remove(int arg0) {
		return list.remove(arg0);
	}

	public boolean removeAll(Collection<?> arg0) {
		return list.removeAll(arg0);
	}

	public boolean retainAll(Collection<?> arg0) {
		return list.retainAll(arg0);
	}

	public T set(int arg0, T arg1) {
		return list.set(arg0,arg1);
	}

	public int size() {
		return list.size();
	}

	public List<T> subList(int arg0, int arg1) {
		return list.subList(arg0,arg1);
	}

	public Object[] toArray() {
		return list.toArray();
	}

	@SuppressWarnings("hiding")
	public <T> T[] toArray(T[] arg0) {
		return list.toArray(arg0);
	}
	
	public String toString(){
		StringBuffer ret = new StringBuffer("[");
		for(T e:this){
			ret.append("\"");
			ret.append(e.toString());
			ret.append("\",");
		}
		ret.delete(ret.length()-1, ret.length());
		ret.append("]");
		return(ret.toString());
	}

}
