package com.qf.sxy.day42_rx.cache;

import android.graphics.Bitmap;

/**
 * Created by BlueSky on 16/11/4.
 */

public interface Cache {
     Bitmap getBitmap(String s);
     void putBitmap(String s, Bitmap bitmap);
}
