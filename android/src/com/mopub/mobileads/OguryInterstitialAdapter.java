/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2017, FrostWire(R). All rights reserved.
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

package com.mopub.mobileads;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.offers.Offers;
import com.frostwire.util.Logger;

import java.util.Map;
import java.util.Random;

import io.presage.IADHandler;
import io.presage.Presage;

/**
 * @author aldenml
 * @author gubatron
 * @author marcelinkaaa
 *         Created on 4/10/2017 - ogury 2.0.5
 *         Updated 04/26/2017 - ogury 2.1.1
 */
public final class OguryInterstitialAdapter extends CustomEventInterstitial {

    private static final Logger LOG = Logger.getLogger(OguryInterstitialAdapter.class);

    private static boolean OGURY_STARTED = false;
    private static boolean OGURY_ENABLED = false;

    private CustomEventInterstitialListener interstitialListener;

    public OguryInterstitialAdapter() {
        // this class should be created only once by the mopub framework
        // both OGURY_STARTED and OGURY_ENABLED are static to minimize the
        // risks in case of getting in a multithreaded environment
        OGURY_ENABLED = UIUtils.diceRollPassesThreshold(ConfigurationManager.instance(), Constants.PREF_KEY_GUI_OGURY_THRESHOLD);
        LOG.info("OGURY_ENABLED=" + OGURY_ENABLED);
    }

    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> map, Map<String, String> map1) {
        if (customEventInterstitialListener == null) {
            // this should not happen, but just in case
            LOG.error("loadInterstitial() aborted. CustomEventInterstitialListener was null.");
            return;
        }

        if (!OGURY_ENABLED) {
            LOG.info("loadInterstitial() aborted, ogury not enabled.");
            return;
        }

        if (Offers.disabledAds()) {
            LOG.info("loadInterstitial() aborted, ogury not enabled. PlayStore reports no ads");
            return;
        }

        interstitialListener = customEventInterstitialListener;
        LOG.info("loadInterstitial() starting ogury");
        startOgury(context); // starts only once
        presage().load(new OguryIADHandler(interstitialListener));
    }

    @Override
    protected void showInterstitial() {
        if (!OGURY_ENABLED) {
            LOG.info("showInterstitial() aborted, ogury disabled.");
            return;
        }

        if (interstitialListener == null) {
            // this should not happen at this point, but just in case
            LOG.error("showInterstitial() aborted. CustomEventInterstitialListener was null.");
            return;
        }

        if (presage().canShow()) {
            LOG.info("showInterstitial() Showing Ogury-Mopub interstitial");
            presage().show(new OguryIADHandler(interstitialListener));
        } else {
            LOG.info("showInterstitial() Ogury-Mopub canShow()=false, ad not loaded yet");
            interstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected void onInvalidate() {
    }

    private static Presage presage() {
        return Presage.getInstance();
    }

    private static void startOgury(Context context) {
        if (!OGURY_ENABLED || OGURY_STARTED) {
            return;
        }
        try {
            OGURY_STARTED = true;
            // presage internally picks the application context
            presage().setContext(context);
            presage().start();
            LOG.info("startOgury: Ogury started from Mopub-Ogury adapter");
        } catch (Throwable e) {
            OGURY_STARTED = false;
            LOG.error("startOgury: Could not start Ogury from Mopub-Ogury adapter", e);
        }
    }

    private static final class OguryIADHandler implements IADHandler {

        private final CustomEventInterstitialListener mopubListener;

        private OguryIADHandler(CustomEventInterstitialListener mopubListener) {
            this.mopubListener = mopubListener;
        }

        @Override
        public void onAdAvailable() {
        }

        @Override
        public void onAdNotAvailable() {
            mopubListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }

        @Override
        public void onAdLoaded() {
            mopubListener.onInterstitialLoaded();
        }

        @Override
        public void onAdClosed() {
            mopubListener.onInterstitialDismissed();
        }

        @Override
        public void onAdError(int code) {
            mopubListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
        }

        @Override
        public void onAdDisplayed() {
            mopubListener.onInterstitialShown();
        }
    }
}
