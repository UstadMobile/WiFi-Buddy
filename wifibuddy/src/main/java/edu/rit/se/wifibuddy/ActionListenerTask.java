package edu.rit.se.wifibuddy;

import android.net.wifi.p2p.WifiP2pManager;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mike on 9/8/17.
 */

public abstract class ActionListenerTask {

    private String taskName;

    private int taskId;

    private static AtomicInteger idInteger = new AtomicInteger();

    private volatile boolean timedOut = false;

    private int timeout = 10000;

    public ActionListenerTask(String taskName) {
        this.taskName = taskName;
        taskId = idInteger.getAndIncrement();
    }

    public ActionListenerTask() {
        this("Unnamed ActionListenerTask");
    }

    public abstract void run(WifiP2pManager.ActionListener listener);

    public void onSuccess() {

    }

    public void onFailure(int reason) {

    }

    public void onTimeout() {

    }

    public String getName() {
        return "#" + taskId + ":" + taskName;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public int getTimeout() {
        return timeout;
    }

    public ActionListenerTask setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
}
