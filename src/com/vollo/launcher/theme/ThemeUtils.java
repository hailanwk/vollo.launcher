package com.vollo.launcher.theme;

import android.net.Uri;


public class ThemeUtils {

	public final static String THEME_MANAGER_PACKAGE = "com.huaqin.thememgr";
	public final static String THEME_PACKAGE_TOKEN = "com.vollo.launcher.theme.";
	public final static String DEFAULT_THEME_PACKAGENAME = "theme_defalt";
	
	public static final String AUTHORITY        = "com.huaqin.thememgr.Settings";
	public static final Uri    CONTENT_URI      = Uri.parse("content://"+ AUTHORITY + "/theme");
	public static final String PACKAGE_NAME     = "package_name";
	
	public static final String SLECTION_LAUNCHER = "category='launcherIcon'";
	public static final String SLECTION_WALLPAPER = "category='wallpaper'";
	
	public final static int APP_ICON_BG_MAX_COUNT = 10;
}