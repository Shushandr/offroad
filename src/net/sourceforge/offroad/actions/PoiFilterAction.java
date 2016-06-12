package net.sourceforge.offroad.actions;

import java.awt.event.ActionEvent;

import net.osmand.AndroidUtils;
import net.osmand.plus.poi.PoiUIFilter;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.actions.OffRoadAction.SelectableAction;

public class PoiFilterAction extends OffRoadAction implements SelectableAction {
	private PoiUIFilter mFilter;
	private String mFilterId;
	private boolean mUseTextFilter;

	public PoiFilterAction(OsmWindow pContext, PoiUIFilter pFilter, boolean pUseTextFilter) {
		super(pContext, pFilter != null?pFilter.getName(): pContext.getOffRoadString("offroad.poifilteroff"), null);
		mFilter = pFilter;
		mUseTextFilter = pUseTextFilter;
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
		mContext.setPoiFilter(mFilter, (mUseTextFilter)?mContext.getSearchTextField().getText():null);
	}


	@Override
	public void save() {
	}
}