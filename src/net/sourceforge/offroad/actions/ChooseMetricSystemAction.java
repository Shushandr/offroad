/**
   OffRoad
   Copyright (C) 2020 Reimar Döffinger

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

package net.sourceforge.offroad.actions;

import java.awt.event.ActionEvent;

import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.R;
import net.sourceforge.offroad.actions.OffRoadAction.SelectableAction;

public class ChooseMetricSystemAction extends OffRoadAction implements SelectableAction {

	private MetricsConstants mMetricSystem;

	static private String getMetricSystemString(OsmWindow ctx, MetricsConstants sys)
	{
		if (sys == MetricsConstants.KILOMETERS_AND_METERS) return ctx.getString(R.string.si_km_m);
		if (sys == MetricsConstants.MILES_AND_FOOTS) return ctx.getString(R.string.si_mi_foots);
		if (sys == MetricsConstants.NAUTICAL_MILES) return ctx.getString(R.string.si_nm);
		if (sys == MetricsConstants.MILES_AND_YARDS) return ctx.getString(R.string.si_mi_yard);
		return sys.toHumanString();
	}

	public ChooseMetricSystemAction(OsmWindow pContext, MetricsConstants pMetricSystem) {
		super(pContext, getMetricSystemString(pContext, pMetricSystem), null);
		mMetricSystem = pMetricSystem;
	}

	@Override
	public void actionPerformed(ActionEvent pE) {
		mContext.getSettings().METRIC_SYSTEM.set(mMetricSystem);
		mContext.getDrawPanel().flushCacheAndDrawLater();
	}


	@Override
	public boolean isSelected() {
		return mMetricSystem == mContext.getSettings().METRIC_SYSTEM.get();
	}
}
