package edu.rit.se.wifibuddy;

import android.net.wifi.p2p.WifiP2pManager;

/**
 * Created by mike on 7/11/17.
 */

public class RetryActionListener implements WifiP2pManager.ActionListener {

    private Runnable runnable;

    private WifiP2pManager.ActionListener actionListener;

    public RetryActionListener(Runnable runnable, WifiP2pManager.ActionListener actionListener) {

    }


    @Override
    public void onSuccess() {

    }

    @Override
    public void onFailure(int i) {

    }
}
