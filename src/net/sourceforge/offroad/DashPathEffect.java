package net.sourceforge.offroad;

public class DashPathEffect {

	private float[] mFs;
	private float mI;

	public DashPathEffect(float[] pFs, float pI) {
		mFs = pFs;
		mI = pI;
	}

	public float[] getDashes() {
		return mFs;
	}

	public float getDashPhase() {
		return mI;
	}

}
