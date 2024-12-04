package org.secuso.privacyfriendlybackup.ui.encryption;

/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;

import android.util.AttributeSet;

import org.openintents.openpgp.R;
import org.openintents.openpgp.util.OpenPgpApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Does not extend ListPreference, but is very similar to it!
 * http://grepcode.com/file_/repository.grepcode.com/java/ext/com.google.android/android/4.4_r1/android/preference/ListPreference.java/?v=source
 */
public class OpenPgpAppPreference extends DialogPreference {
    private static final String OPENKEYCHAIN_PACKAGE = "org.sufficientlysecure.keychain";
    private static final String MARKET_INTENT_URI_BASE = "market://details?id=%s";
    private static final Intent MARKET_INTENT = new Intent(Intent.ACTION_VIEW, Uri.parse(
            String.format(MARKET_INTENT_URI_BASE, OPENKEYCHAIN_PACKAGE)));

    private static final ArrayList<String> PROVIDER_BLACKLIST = new ArrayList<String>();

    static {
        // Unfortunately, the current released version of APG includes a broken version of the API
        PROVIDER_BLACKLIST.add("org.thialfihar.android.apg");
    }

    private ArrayList<OpenPgpProviderEntry> mLegacyList = new ArrayList<>();
    private ArrayList<OpenPgpProviderEntry> mList = new ArrayList<>();

    private String mSelectedPackage;

    public OpenPgpAppPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        populateAppList();
    }

    public OpenPgpAppPreference(Context context) {
        this(context, null);
    }

    /**
     * Public method to add new entries for legacy applications
     *
     * @param packageName
     * @param simpleName
     * @param icon
     */
    public void addLegacyProvider(int position, String packageName, String simpleName, Drawable icon) {
        mLegacyList.add(position, new OpenPgpProviderEntry(packageName, simpleName, icon));
    }

    void setAndPersist(String packageName) {
        if (!callChangeListener(packageName)) {
            // They don't want the value to be set
            return;
        }

        mSelectedPackage = packageName;

        // Save to persistent storage (this method will make sure this
        // preference should be persistent, along with other useful checks)
        persistString(mSelectedPackage);

        // Data has changed, notify so UI can be refreshed!
        notifyChanged();

        // also update summary with selected provider
        updateSummary(mSelectedPackage);
    }

    private void updateSummary(String packageName) {
        String summary = getEntryByValue(packageName);
        setSummary(summary);
    }

    @Override
    public CharSequence getSummary() {
        return getEntryByValue(mSelectedPackage);
    }

    int getIndexOfProviderList(String packageName) {
        for (OpenPgpProviderEntry app : mList) {
            if (app.packageName.equals(packageName)) {
                return mList.indexOf(app);
            }
        }

        // default is "none"
        return 0;
    }

    /**
     * Public API
     */
    public String getEntry() {
        return getEntryByValue(mSelectedPackage);
    }

    /**
     * Public API
     */
    public String getValue() {
        return mSelectedPackage;
    }

    /**
     * Public API
     */
    public void setValue(String packageName) {
        setAndPersist(packageName);
    }

    public @NonNull ArrayList<OpenPgpProviderEntry> getEntries() {
        return mList;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            // Restore state
            mSelectedPackage = getPersistedString(mSelectedPackage);
            updateSummary(mSelectedPackage);
        } else {
            String value = (String) defaultValue;
            setAndPersist(value);
            updateSummary(value);
        }
    }

    public String getEntryByValue(String packageName) {
        for (OpenPgpProviderEntry app : mList) {
            if (app.packageName.equals(packageName) && app.intent == null) {
                return app.simpleName;
            }
        }

        return getContext().getString(R.string.openpgp_list_preference_none);
    }

    void populateAppList() {
        mList.clear();

        // add "none"-entry
        mList.add(0, new OpenPgpProviderEntry("",
                getContext().getString(R.string.openpgp_list_preference_none),
                getContext().getResources().getDrawable(R.drawable.ic_action_cancel_launchersize)));

        // add all additional (legacy) providers
        mList.addAll(mLegacyList);

        // search for OpenPGP providers...
        ArrayList<OpenPgpProviderEntry> providerList = new ArrayList<>();
        Intent intent = new Intent(OpenPgpApi.SERVICE_INTENT_2);
        List<ResolveInfo> resInfo = getContext().getPackageManager().queryIntentServices(intent, 0);
        if (!resInfo.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfo) {
                if (resolveInfo.serviceInfo == null)
                    continue;

                String packageName = resolveInfo.serviceInfo.packageName;
                String simpleName = String.valueOf(resolveInfo.serviceInfo.loadLabel(getContext()
                        .getPackageManager()));
                Drawable icon = resolveInfo.serviceInfo.loadIcon(getContext().getPackageManager());

                if (!PROVIDER_BLACKLIST.contains(packageName)) {
                    providerList.add(new OpenPgpProviderEntry(packageName, simpleName, icon));
                }
            }
        }

        if (providerList.isEmpty()) {
            // add install links if provider list is empty
            resInfo = getContext().getPackageManager().queryIntentActivities
                    (MARKET_INTENT, 0);
            for (ResolveInfo resolveInfo : resInfo) {
                Intent marketIntent = new Intent(MARKET_INTENT);
                marketIntent.setPackage(resolveInfo.activityInfo.packageName);
                Drawable icon = resolveInfo.activityInfo.loadIcon(getContext().getPackageManager());
                String marketName = String.valueOf(resolveInfo.activityInfo.applicationInfo
                        .loadLabel(getContext().getPackageManager()));
                String simpleName = String.format(getContext().getString(R.string
                        .openpgp_install_openkeychain_via), marketName);
                mList.add(new OpenPgpProviderEntry(OPENKEYCHAIN_PACKAGE, simpleName,
                        icon, marketIntent));
            }
        } else {
            // add provider
            mList.addAll(providerList);
        }
    }

    static class OpenPgpProviderEntry {
        String packageName;
        String simpleName;
        Drawable icon;
        Intent intent;

        public OpenPgpProviderEntry(String packageName, String simpleName, Drawable icon) {
            this.packageName = packageName;
            this.simpleName = simpleName;
            this.icon = icon;
        }

        public OpenPgpProviderEntry(String packageName, String simpleName, Drawable icon, Intent intent) {
            this(packageName, simpleName, icon);
            this.intent = intent;
        }

        @Override
        public String toString() {
            return simpleName;
        }
    }
}
