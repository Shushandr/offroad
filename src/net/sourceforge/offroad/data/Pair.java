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

package net.sourceforge.offroad.data;

import java.util.Map.Entry;

/**
 * @author foltin
 * @date 31.05.2016
 */
public class Pair<L, R> implements Entry<L,R>{

	private final L left;
	private R right;

	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public L getLeft() {
		return left;
	}

	public R getRight() {
		return right;
	}

	@Override
	public int hashCode() {
		return left.hashCode() ^ right.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Pair<?, ?>))
			return false;
		Pair<?, ?> pairo = (Pair<?, ?>)o;
		return this.left.equals(pairo.getLeft()) && this.right.equals(pairo.getRight());
	}

	@Override
	public L getKey() {
		return left;
	}

	@Override
	public R getValue() {
		return right;
	}

	@Override
	public R setValue(R pValue) {
		right = pValue;
		return right;
	}

}
