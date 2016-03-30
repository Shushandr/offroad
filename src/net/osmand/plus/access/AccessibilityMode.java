package net.osmand.plus.access;

import net.sourceforge.offroad.R;

public enum AccessibilityMode {

    ON(R.string.shared_string_on),
    OFF(R.string.shared_string_off),
    DEFAULT(R.string.accessibility_default);

    private final int key;

    AccessibilityMode(int key) {
        this.key = key;
    }

    public String toHumanString() {
        return ""+key;
    }

}
