package net.osmand.router;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import net.osmand.binary.RouteDataObject;

public class BinaryRoutePlanner {
	
	public interface RouteSegmentVisitor {
		
		public void visitSegment(RouteSegment segment, int segmentEnd, boolean poll);
	}
	
	public static class RouteSegmentPoint extends RouteSegment {
		public RouteSegmentPoint(RouteDataObject road, int segmentStart, double distSquare) {
			super(road, segmentStart);
			this.distSquare = distSquare;
		}

		public double distSquare;
		public int preciseX;
		public int preciseY;
		public List<RouteSegmentPoint> others;
	}
	
	public static class RouteSegment {
		final short segStart;
		final RouteDataObject road;
		// needed to store intersection of routes
		RouteSegment next = null;
		RouteSegment oppositeDirection = null;
		
		// search context (needed for searching route)
		// Initially it should be null (!) because it checks was it segment visited before
		RouteSegment parentRoute = null;
		short parentSegmentEnd = 0;
		// 1 - positive , -1 - negative, 0 not assigned
		byte directionAssgn = 0;
		
		// distance measured in time (seconds)
		float distanceFromStart = 0;
		float distanceToEnd = 0;
		
		public RouteSegment(RouteDataObject road, int segmentStart) {
			this.road = road;
			this.segStart = (short) segmentStart;
		}
		
		public RouteSegment initRouteSegment(boolean positiveDirection) {
			if(segStart == 0 && !positiveDirection) {
				return null;
			}
			if(segStart == road.getPointsLength() - 1 && positiveDirection) {
				return null;
			}
			RouteSegment rs = this;
			if(directionAssgn == 0) {
				rs.directionAssgn = (byte) (positiveDirection ? 1 : -1);
			} else {
				if(positiveDirection != (directionAssgn == 1)) {
					if(oppositeDirection == null) {
						oppositeDirection = new RouteSegment(road, segStart);
						oppositeDirection.directionAssgn = (byte) (positiveDirection ? 1 : -1);
					}
					if ((oppositeDirection.directionAssgn == 1) != positiveDirection) {
						throw new IllegalStateException();
					}
					rs = oppositeDirection;
				}
			}
			return rs;
		}
		
		public byte getDirectionAssigned(){
			return directionAssgn;
		}
		
		public RouteSegment getParentRoute() {
			return parentRoute;
		}
		
		public boolean isPositive() {
			return directionAssgn == 1;
		}
		
		public void setParentRoute(RouteSegment parentRoute) {
			this.parentRoute = parentRoute;
		}
		
		public void assignDirection(byte b) {
			directionAssgn = b;
		}
		
		public void setParentSegmentEnd(int parentSegmentEnd) {
			this.parentSegmentEnd = (short) parentSegmentEnd;
		}
		
		public int getParentSegmentEnd() {
			return parentSegmentEnd;
		}
		
		public RouteSegment getNext() {
			return next;
		}
		
		public short getSegmentStart() {
			return segStart;
		}
		
		public float getDistanceFromStart() {
			return distanceFromStart;
		}
		
		public void setDistanceFromStart(float distanceFromStart) {
			this.distanceFromStart = distanceFromStart;
		}
		
		public RouteDataObject getRoad() {
			return road;
		}
		
		public String getTestName(){
			return MessageFormat.format("s{0,number,#.##} e{1,number,#.##}", ((float)distanceFromStart), ((float)distanceToEnd));
		}
		
		
		public Iterator<RouteSegment> getIterator() {
			return new Iterator<BinaryRoutePlanner.RouteSegment>() {
				RouteSegment next = RouteSegment.this;
				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
				
				@Override
				public RouteSegment next() {
					RouteSegment c = next;
					if(next != null) {
						next = next.next;
					}
					return c;
				}
				
				@Override
				public boolean hasNext() {
					return next != null;
				}
			};
		}
	}
	
	static class FinalRouteSegment extends RouteSegment {
		
		boolean reverseWaySearch;
		RouteSegment opposite;

		public FinalRouteSegment(RouteDataObject road, int segmentStart) {
			super(road, segmentStart);
		}
		
	}
	
}
