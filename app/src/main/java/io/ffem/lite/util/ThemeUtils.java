/*
 * Copyright (C) 2018 Shobhit Agarwal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.ffem.lite.util;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;

import io.ffem.lite.R;


public final class ThemeUtils {

    private final Context context;

    public ThemeUtils(Context context) {
        this.context = context;
    }

    @StyleRes
    public int getAppTheme() {
        return R.style.Theme_Collect;
    }

//    @DrawableRes
//    public int getDivider() {
//        return isDarkTheme() ? android.R.drawable.divider_horizontal_dark : android.R.drawable.divider_horizontal_bright;
//    }

    private int getAttributeValue(@AttrRes int resId) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(resId, outValue, true);
        return outValue.data;
    }

    public boolean isSystemTheme() {
        return true;
    }

//    public boolean isDarkTheme() {
//        if (isSystemTheme()) {
//            int uiMode = context.getResources().getConfiguration().uiMode;
//            return (uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES;
//        } else {
//            String theme = getPrefsTheme();
//            return theme.equals(context.getString(R.string.app_theme_dark));
//        }
//    }
//
//    private String getPrefsTheme() {
//        return context.getString(R.string.app_theme_light);
//    }

    /**
     * @return Text color for the current {@link android.content.res.Resources.Theme}
     */
    @ColorInt
    public int getColorOnSurface() {
        return getAttributeValue(R.attr.colorOnSurface);
    }


    @ColorInt
    public int getColorPrimary() {
        return getAttributeValue(R.attr.colorPrimary);
    }

    @ColorInt
    public int getColorOnPrimary() {
        return getAttributeValue(R.attr.colorOnPrimary);
    }

}
