/** 
   OffRoad
   Copyright (C) 2016 Christian Foltin

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/
package net.sourceforge.offroad.ui;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Used to calculate unzoomed pictures in a suitable order for better zoom out effect.
 * 
 * @author foltin
 * @date 30.04.2016
 */
public class BinaryOrder {
	
	TreeSet<Integer> mAllNumbers = new TreeSet<>();
	Vector<Integer> mOrder = new Vector<>();
	
	public BinaryOrder() {
	}

	public synchronized void init(int pStart, int pEnd) {
		mOrder.clear();
		mAllNumbers.clear();
		if(pEnd <= pStart){
			return;
		}
		addNumbersOfDepth(pStart, pEnd, 1);
	}
	
	public synchronized boolean hasNext() {
		return !mOrder.isEmpty();
	}
	
	
	public synchronized int getNext() {
		if(!hasNext()){
			throw new IllegalArgumentException("getNext on empty list called!");
		}
		Integer ret = mOrder.firstElement();
		mOrder.remove(0);
		return ret;
	}
	private void addNumbersOfDepth(int pStart, int pEnd, int pDepth) {
		for(float i = pStart; i <= pEnd; i += (0f+pEnd-pStart)/pDepth){
			addNumber((int) i);
		}
		if(mAllNumbers.size() <= pEnd-pStart){
			addNumbersOfDepth(pStart, pEnd, pDepth*2);
		}
	}

	private boolean addNumber(int pNumber) {
		if(mAllNumbers.add(pNumber)){
			mOrder.add(pNumber);
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		BinaryOrder order = new BinaryOrder();
		order.init(1, 17);
		while(order.hasNext()){
			int number = order.getNext();
			for(int j = 1; j < number; j++) {
				System.out.print(" ");
			}
			System.out.println("x " + number);
		}
	}

	public synchronized void alreadyDone(int pZoom) {
		for (Iterator<Integer> it = mOrder.iterator(); it.hasNext();) {
			Integer zoom = (Integer) it.next();
			if(zoom == pZoom){
				it.remove();
				return;
			}
		}
	}

}
