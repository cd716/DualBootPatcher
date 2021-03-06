/*
 * Copyright (C) 2014-2016  Andrew Gunnerson <andrewgunnerson@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.chenxiaolong.dualbootpatcher.appsharing;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.github.chenxiaolong.dualbootpatcher.R;
import com.github.chenxiaolong.dualbootpatcher.RomConfig;
import com.github.chenxiaolong.dualbootpatcher.RomConfig.SharedItems;
import com.github.chenxiaolong.dualbootpatcher.RomUtils;
import com.github.chenxiaolong.dualbootpatcher.RomUtils.RomInformation;
import com.github.chenxiaolong.dualbootpatcher.socket.MbtoolConnection;
import com.github.chenxiaolong.dualbootpatcher.socket.interfaces.MbtoolInterface;

import org.apache.commons.io.IOUtils;

import java.util.HashMap;

public class AppSharingService extends IntentService {
    private static final String TAG = AppSharingService.class.getSimpleName();

    public static final String ACTION = "action";
    public static final String ACTION_PACKAGE_REMOVED = "package_removed";

    public static final String EXTRA_PACKAGE = "package";

    public AppSharingService() {
        super(TAG);
    }

    private void onPackageRemoved(final String pkg) {
        RomInformation info;

        MbtoolConnection conn = null;

        try {
            conn = new MbtoolConnection(this);
            MbtoolInterface iface = conn.getInterface();

            info = RomUtils.getCurrentRom(this, iface);
        } catch (Exception e) {
            Log.e(TAG, "Failed to determine current ROM. App sharing status was NOT updated", e);
            return;
        } finally {
            IOUtils.closeQuietly(conn);
        }

        if (info == null) {
            Log.e(TAG, "Failed to determine current ROM. App sharing status was NOT updated");
            return;
        }

        // Unshare package if explicitly removed. This only exists to keep the config file clean.
        // Mbtool will not touch any app that's not listed in the package database.
        RomConfig config = RomConfig.getConfig(info.getConfigPath());
        HashMap<String, SharedItems> sharedPkgs = config.getIndivAppSharingPackages();
        if (sharedPkgs.containsKey(pkg)) {
            sharedPkgs.remove(pkg);
            config.setIndivAppSharingPackages(sharedPkgs);
            config.apply();

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    String message = getString(R.string.indiv_app_sharing_app_no_longer_shared);
                    Toast.makeText(AppSharingService.this, String.format(message, pkg),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getStringExtra(ACTION);

        if (ACTION_PACKAGE_REMOVED.equals(action)) {
            onPackageRemoved(intent.getStringExtra(EXTRA_PACKAGE));
        }
    }
}
