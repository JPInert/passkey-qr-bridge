package io.github.jpinert.passkeyqr;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** One screen: what the app does, whether it is set up, and the one button that sets it up. */
public class SetupActivity extends Activity {
    private TextView status, passkey;
    private Button action;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = dp(20);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(pad, pad, pad, pad);

        TextView title = text(getString(R.string.app_name), 24);
        TextView about = text(getString(R.string.about), 16);
        status = text("", 16);
        passkey = text("", 14);
        action = new Button(this);
        TextView how = text(getString(R.string.how), 14);

        col.addView(title);
        col.addView(about, gap());
        col.addView(status, gap());
        col.addView(action, gap());
        col.addView(passkey, gap());
        col.addView(how, gap());

        ScrollView sv = new ScrollView(this);
        sv.setFitsSystemWindows(true);
        sv.addView(col);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        String def = Routes.searchDefault(this);
        if (def.equals(getPackageName())) {
            status.setText(R.string.status_active);
            action.setVisibility(View.GONE);
        } else if (def.isEmpty()) {
            status.setText(R.string.status_ask);
            action.setText(R.string.button_setup);
            action.setVisibility(View.VISIBLE);
            action.setOnClickListener(v -> askForDefault());
        } else {
            CharSequence other = Routes.label(this, def);
            status.setText(getString(R.string.status_other, other));
            action.setText(getString(R.string.button_clear, other));
            action.setVisibility(View.VISIBLE);
            action.setOnClickListener(v -> openAppSettings(def));
        }
        passkey.setText(Routes.hasPasskeyHandler(this) ? R.string.passkey_ok : R.string.passkey_missing);
    }

    /** A test search: Android shows its "Open with" sheet, where the user picks us and "Always". */
    private void askForDefault() {
        try {
            startActivity(new Intent(Intent.ACTION_WEB_SEARCH)
                    .putExtra(SearchManager.QUERY, Routes.SETUP_QUERY));
        } catch (ActivityNotFoundException ignored) {
        }
    }

    /** The other app's info page, where "Open by default" → "Clear defaults" lives. */
    private void openAppSettings(String pkg) {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", pkg, null)));
    }

    private TextView text(String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setGravity(Gravity.START);
        return t;
    }

    private LinearLayout.LayoutParams gap() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(16);
        return lp;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
