package cz.gradle.android.webp;

/**
 * @author haozhou
 */

public class WebPAndroidExtension {
    private boolean autoConvert = false;
    private int quality = 75;

    public boolean isAutoConvert() {
        return autoConvert;
    }

    public void setAutoConvert(boolean autoConvert) {
        this.autoConvert = autoConvert;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }
}
