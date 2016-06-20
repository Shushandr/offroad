package net.sourceforge.offroad.actions;

import java.awt.event.ActionEvent;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.sourceforge.offroad.OsmWindow;

public class ShowFavoriteAction extends OffRoadAction {
	private FavouritePoint mFavouritePoint;

	public ShowFavoriteAction(OsmWindow pContext, FavouritePoint pFp) {
		super(pContext, pFp.getName(), null);
		mFavouritePoint = pFp;
	}

	@Override
	public void actionPerformed(ActionEvent pE) {
		mContext.move(new LatLon(mFavouritePoint.getLatitude(), mFavouritePoint.getLongitude()), null);
	}

	@Override
	public void save() {
		
	}
}