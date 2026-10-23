package io.github.jpinert.passkeyqr;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Handles "search the web" requests. Many phone makers' camera apps read QR codes, but send any
 * code they don't recognise to a web search, including passkey sign-in codes ("FIDO:/..."). Those
 * go to the system passkey handler instead, the same place Google's own QR scanner sends them.
 * Every other search goes on unchanged to the default browser. Never shown on screen.
 */
public class SearchShim extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent in = getIntent();
        String q = in.getStringExtra(SearchManager.QUERY);
        try {
            if (Routes.SETUP_QUERY.equals(q)) {
                // the setup screen's own test search: Android just made us the default
                startActivity(new Intent(this, SetupActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            } else if (Routes.isPasskey(q) && Routes.hasPasskeyHandler(this)) {
                startActivity(Routes.passkeyIntent(q).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                if (Routes.isPasskey(q)) {
                    Toast.makeText(this, R.string.no_passkey_handler, Toast.LENGTH_LONG).show();
                }
                forward(in, q);
            }
        } catch (ActivityNotFoundException | SecurityException ignored) {
            // nothing can take it: drop it rather than loop back here
        }
        finish();
    }

    private void forward(Intent in, String q) {
        ComponentName target = Routes.searchTarget(this);
        if (target != null) {
            // A fresh intent: copying the caller's would carry its flags (e.g. exclude-from-recents)
            // into the browser's task, and some browsers then never show the page.
            Intent out = new Intent(Intent.ACTION_WEB_SEARCH).setComponent(target)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (in.getExtras() != null) out.putExtras(in.getExtras());
            startActivity(out);
        } else if (q != null) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://duckduckgo.com/?q=" + Uri.encode(q)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
