package edu.rit.se.wifibuddy;

import android.net.wifi.p2p.WifiP2pManager;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * Base representation of an ActionListenerTask
 *
 * @see ActionListenerTaskQueue
 *
 * Created by mike on 9/8/17.
 */
public abstract class ActionListenerTask {

    private String taskName;

    private int taskId;

    private static AtomicInteger idInteger = new AtomicInteger();

    private volatile boolean timedOut = false;

    private int timeout = 10000;

    /**
     * Create a new task.
     *
     * @param taskName Task name used for logging purposes.
     */
    public ActionListenerTask(String taskName) {
        this.taskName = taskName;
        taskId = idInteger.getAndIncrement();
    }

    public ActionListenerTask() {
        this("Unnamed ActionListenerTask");
    }

    /**
     * Override this method to make the async call you want to make and use the provided listener
     * as the ActionListener. If you want to run synchronous code you can always simply call
     * listener.onSuccess() .
     *
     * @param listener
     */
    public abstract void run(WifiP2pManager.ActionListener listener);

    /**
     * Called when the task has succeeded as per ActionListener.onSuccess .
     */
    public void onSuccess() {

    }

    /**
     * Called when the task has failed as per ActionListener.onFailure .
     *
     * @param reason
     */
    public void onFailure(int reason) {

    }

    /**
     * Called when the task has timed out. If the task times out onSuccess and onFailure will not
     * be called.
     */
    public void onTimeout() {

    }

    /**
     * The name of the task - used for logging purposes.
     *
     * @return A string with the task id and name as provided e.g. "#4 - clear local services"
     */
    public String getName() {
        return "#" + taskId + ":" + taskName;
    }

    /**
     * Used to determine if the task has timed out or not
     * @return
     */
    public boolean isTimedOut() {
        return timedOut;
    }

    /**
     * Sets whether or not this task has timed out. This should be used *ONLY* by the TaskQueue.
     * @param timedOut
     */
    void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    /**
     * Gets the time (in ms) that this task is allowed to run for until it will timeout.
     *
     * @return Time in ms the task is allowed to run for (default 10,000 - e.g. 10 seconds)
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets the amount of time this task is allowed to run for
     * @param timeout Time in ms the task will be allowed to run for
     * @return this
     */
    public ActionListenerTask setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
}
