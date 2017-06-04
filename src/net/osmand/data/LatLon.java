package net.osmand.data;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import net.osmand.util.MapUtils;

@XmlRootElement
public class LatLon implements Serializable {
	public void setLongitude(double pLongitude) {
		longitude = pLongitude;
	}

	public void setLatitude(double pLatitude) {
		latitude = pLatitude;
	}
	private double longitude;
	private double latitude;

	public LatLon() {
	}
	
	public LatLon(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		int temp;
		temp = (int)Math.floor(latitude * 10000);
		result = prime * result + temp;
		temp = (int)Math.floor(longitude * 10000);
		result = prime * result + temp;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		LatLon other = (LatLon) obj;
		return Math.abs(latitude - other.latitude) < 0.00001
				&& Math.abs(longitude - other.longitude) < 0.00001;
	}

	@Override
	public String toString() {
		return "Lat " + ((float)latitude) + " Lon " + ((float)longitude); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public int get31TileNumberX() {
		return MapUtils.get31TileNumberX(longitude);
	}
	public int get31TileNumberY() {
		return MapUtils.get31TileNumberY(latitude);
	}

}