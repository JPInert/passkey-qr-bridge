package io.github.jpinert.passkeyqr;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.List;

/** Who handles what on this phone. Shared by the search shim and the setup screen. */
final class Routes {
    /** The query the setup screen sends to make Android ask which app handles web searches. */
    static final String SETUP_QUERY = "io.github.jpinert.passkeyqr:setup";

    private Routes() { }

    /** True for a passkey sign-in code from a QR (FIDO Alliance hybrid / caBLE v2 URI). */
    static boolean isPasskey(String q) {
        return q != null && q.trim().regionMatches(true, 0, "FIDO:/", 0, 6);
    }

    /** The intent that hands a passkey code to the system passkey handler. */
    static Intent passkeyIntent(String q) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(q.trim()));
    }

    /** Whether anything on the phone (normally Google Play Services) accepts passkey codes. */
    static boolean hasPasskeyHandler(Context c) {
        return !c.getPackageManager().queryIntentActivities(passkeyIntent("FIDO:/0"), 0).isEmpty();
    }

    /** The package of the default browser, or "" when none is set. */
    static String defaultBrowser(Context c) {
        ResolveInfo r = c.getPackageManager().resolveActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
                        .addCategory(Intent.CATEGORY_BROWSABLE),
                PackageManager.MATCH_DEFAULT_ONLY);
        return r == null || "android".equals(r.activityInfo.packageName) ? "" : r.activityInfo.packageName;
    }

    /** The web-search activity ordinary searches go on to: the default browser's, else any other. */
    static ComponentName searchTarget(Context c) {
        List<ResolveInfo> all = c.getPackageManager()
                .queryIntentActivities(new Intent(Intent.ACTION_WEB_SEARCH), 0);
        String browser = defaultBrowser(c);
        ComponentName any = null;
        for (ResolveInfo r : all) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(c.getPackageName())) continue;
            ComponentName cn = new ComponentName(pkg, r.activityInfo.name);
            if (pkg.equals(browser)) return cn;
            if (any == null && !pkg.startsWith("com.google.android.setupwizard")) any = cn;
        }
        return any;
    }

    /** The package Android currently sends web searches to: ours, another app's, or "" (asks). */
    static String searchDefault(Context c) {
        ResolveInfo r = c.getPackageManager().resolveActivity(
                new Intent(Intent.ACTION_WEB_SEARCH), PackageManager.MATCH_DEFAULT_ONLY);
        return r == null || "android".equals(r.activityInfo.packageName) ? "" : r.activityInfo.packageName;
    }

    /** A user-facing name for a package. */
    static CharSequence label(Context c, String pkg) {
        try {
            PackageManager pm = c.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }
}
