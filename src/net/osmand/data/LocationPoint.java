package net.osmand.data;

import net.sourceforge.offroad.OsmWindow;

/**
 */
public interface LocationPoint {

	public double getLatitude();

	public double getLongitude();

	public int getColor();
	
	public boolean isVisible();
	
	public PointDescription getPointDescription(OsmWindow ctx);

//	public String getSpeakableName();
	
	//public void prepareCommandPlayer(CommandBuilder cmd, String names);
	
}
