package net.sourceforge.offroad.actions;

import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.sourceforge.offroad.OsmWindow;

public class AddFavoriteAction extends OffRoadAction {
	public AddFavoriteAction(OsmWindow pContext, String pName, Icon pIcon) {
		super(pContext, pName, pIcon);
	}

	@Override
	public void actionPerformed(ActionEvent pE) {
		String result = JOptionPane.showInputDialog("Enter favorite's name");
		if(result != null){
			LatLon pos = mContext.getCursorPosition();
			mContext.getFavorites().addEmptyCategory("CategoryBla", 0x2288AA44);
			mContext.getFavorites().addFavourite(new FavouritePoint(pos.getLatitude(), pos.getLongitude(), result, "CategoryBla"));
		}
	}

	@Override
	public void save() {
	}
}