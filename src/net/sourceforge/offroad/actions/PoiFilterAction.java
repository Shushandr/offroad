package net.sourceforge.offroad.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.sourceforge.offroad.OsmWindow;

public class PoiFilterAction implements ActionListener {
	private String mFilterId;
	private OsmWindow mContext;

	public PoiFilterAction(OsmWindow pContext, String pFilterId) {
		mContext = pContext;
		mFilterId = pFilterId;
	}
	
	@Override
	public void actionPerformed(ActionEvent pE) {
		mContext.getSettings().SELECTED_POI_FILTER_FOR_MAP.set(mFilterId);
		mContext.getDrawPanel().refreshMap();
	}
}