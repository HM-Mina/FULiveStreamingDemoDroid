package com.sensetime.stmobile.STEffect;

import android.graphics.Bitmap;

/**
 * Created by sensetime on 17-6-8.
 */

public class StickerItem {
    public String name;
    public Bitmap icon;
    public String path;

    public StickerItem(String name, Bitmap icon, String path) {
        this.name = name;
        this.icon = icon;
        this.path = path;
    }
}
