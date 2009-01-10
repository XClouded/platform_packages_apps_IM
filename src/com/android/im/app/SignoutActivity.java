/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.im.app;

import android.app.Activity;
import android.provider.Im;
import android.os.Handler;
import android.os.Bundle;
import android.os.RemoteException;
import android.content.Intent;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import android.database.Cursor;
import com.android.im.IImConnection;


public class SignoutActivity extends Activity {

    private String[] ACCOUNT_SELECTION = new String[] {
            Im.Account.PROVIDER,
    };

    private ImApp mApp;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data == null) {
            Log.e(ImApp.LOG_TAG, "Need account data to sign in");
            finish();
            return;
        }

        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(data,
                ACCOUNT_SELECTION,
                null /* selection */,
                null /* selection args */,
                null /* sort order */);
        final long providerId;

        try {
            if (!c.moveToFirst()) {
                Log.w(ImApp.LOG_TAG, "[SignoutActivity] No data for " + data);
                finish();
                return;
            }

            providerId = c.getLong(c.getColumnIndexOrThrow(Im.Account.PROVIDER));
        } finally {
            c.close();
        }

        mApp = ImApp.getApplication(this);
        mApp.callWhenServiceConnected(mHandler, new Runnable() {
            public void run() {
                signOut(providerId);
            }
        });
    }

    private void signOut(long providerId) {
        try {
            IImConnection conn = mApp.getConnection(providerId);
            if (conn != null) {
                conn.logout();
            }
        } catch (RemoteException ex) {
            Log.e(ImApp.LOG_TAG, "signout: caught ", ex);
        } finally {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // always call finish here, because we don't want to be in the backlist ever, and
        // we don't handle onRestart()
        finish();
    }


    static void log(String msg) {
        Log.d(ImApp.LOG_TAG, "[Signout] " + msg);
    }
}