package net.sourceforge.offroad.actions;

import java.awt.event.ActionEvent;

import net.osmand.AndroidUtils;
import net.osmand.plus.poi.PoiUIFilter;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.actions.OffRoadAction.SelectableAction;

public class PoiFilterAction extends OffRoadAction implements SelectableAction {
	private PoiUIFilter mFilter;
	private String mFilterId;

	public PoiFilterAction(OsmWindow pContext, PoiUIFilter pFilter) {
		super(pContext, pFilter != null?pFilter.getName(): pContext.getOffRoadString("offroad.poifilteroff"), null);
		mFilter = pFilter;
		mFilterId = null;
		if(mFilter != null){
			mFilterId = mFilter.getFilterId();
		}
		
	}

	@Override
	public boolean isSelected() {
		return AndroidUtils.safeEquals(mFilterId, mContext.getSettings().SELECTED_POI_FILTER_FOR_MAP.get());
	}
	
	@Override
	public void actionPerformed(ActionEvent pE) {
		mContext.getSettings().SELECTED_POI_FILTER_FOR_MAP.set(mFilterId);
		mContext.getDrawPanel().refreshMap();
	}


	@Override
	public void save() {
	}
}