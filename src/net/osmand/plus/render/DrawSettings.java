package net.osmand.plus.render;

public class DrawSettings {
    private final boolean nightMode;
    private final boolean updateVectorRendering;

    public DrawSettings(boolean nightMode) {
            this(nightMode, false);
    }

    public DrawSettings(boolean nightMode, boolean updateVectorRendering) {
            this.nightMode = nightMode;
            this.updateVectorRendering = updateVectorRendering;
    }

    public boolean isUpdateVectorRendering() {
            return updateVectorRendering;
    }

    public boolean isNightMode() {
            return nightMode;
    }

}
