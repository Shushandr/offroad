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

package net.sourceforge.offroad.actions;

import java.awt.event.ActionEvent;

import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.render.RenderingRuleProperty;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.actions.OffRoadAction.SelectableAction;

/**
 * @author foltin
 * @date 08.06.2016
 */
public class SetRenderingRule extends OffRoadAction implements SelectableAction {

	private RenderingRuleProperty mCustomProp;
	private String mValue;
	private boolean mIsDefault;

	public SetRenderingRule(OsmWindow pWin, RenderingRuleProperty pCustomProp) {
		super(pWin, pWin.getString("rendering_attr_" + pCustomProp.getAttrName() + "_name"), null);
		mCustomProp = pCustomProp;
		mValue = null;
		mIsDefault = false;
	}

	private static String valueToName(OsmWindow pWin, String value)
	{
		// If the string is a int value there is no translation
		// string and we just return it unchanged
		try {
			Integer.parseInt(value);
			return value;
		} catch (NumberFormatException e) {}
		return pWin.getString("rendering_value_" + value + "_name");
	}

	public SetRenderingRule(OsmWindow pWin, RenderingRuleProperty pCustomProp, String value, boolean isDefault) {
		super(pWin, valueToName(pWin, value), null);
		mCustomProp = pCustomProp;
		mValue = value;
		mIsDefault = isDefault;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		if (mValue != null) {
			CommonPreference<String> pref = getStringPreference();
			// resetToDefault seems not entirely reliable, so always call set first
			pref.set(mValue);
			if (mIsDefault) pref.resetToDefault();
		} else {
			CommonPreference<Boolean> pref = getPreference();
			pref.set(!pref.get());
		}
		mContext.getDrawPanel().flushCacheAndDrawLater();
	}

	private CommonPreference<Boolean> getPreference() {
		return mContext.getSettings()
				.getCustomRenderBooleanProperty(mCustomProp.getAttrName());
	}

	private CommonPreference<String> getStringPreference() {
		return mContext.getSettings()
				.getCustomRenderProperty(mCustomProp.getAttrName());
	}

	@Override
	public boolean isSelected() {
		if (mValue != null) {
			return mValue.equals(getStringPreference().get());
		}
		CommonPreference<Boolean> pref = getPreference();
		return pref.get();
	}

}
