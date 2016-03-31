package net.sourceforge.offroad;

import java.awt.BasicStroke;

public enum Cap {
	BUTT("BUTT", BasicStroke.CAP_BUTT), ROUND("ROUND", BasicStroke.CAP_ROUND), SQUARE("SQUARE", BasicStroke.CAP_SQUARE);

	private String mTitle;
	private int mVal;

	private Cap(String pTitle, int pVal) {
		mTitle = pTitle;
		mVal = pVal;
	}

	public int getVal() {
		return mVal;
	}

}
