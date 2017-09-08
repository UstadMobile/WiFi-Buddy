package edu.rit.se.wifibuddy;

import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * The WiFi p2p api lends itself to callback hell. e.g. we want to clear local services, and then,
 * if successful, add our local service. There are also actions that take place after wifi is
 * enabled and disabled. If those callbacks take longer than the time in between wifi being enabled
 * and disabled a cross over can occur.
 *
 * ActionListenerTaskQueue is designed to simplify things. Instead of this:
 *
     wifiP2pManager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
         @Override
         public void onSuccess() {
             Log.i(TAG, "Adding local service: " + serviceName + " : adding");
             // Add the local service
             wifiP2pManager.addLocalService(channel, wifiP2pServiceInfo, new WifiP2pManager.ActionListener() {
                 @Override
                 public void onSuccess() {

 *
 * You can now do this:
 *
    taskQueue.queueTask(new ActionListenerTask("addLocalService - clearLocalServices") {
        @Override
        public void run(WifiP2pManager.ActionListener listener) {
            wifiP2pManager.clearLocalServices(channel, listener);
        }
    })
    .queueTask(new ActionListenerTask("addLocalService - addLocalService") {
        @Override
        public void run(WifiP2pManager.ActionListener listener) {
            Log.i(TAG, "Adding local service: " + serviceName + " : adding");
            wifiP2pManager.addLocalService(channel, serviceInfo, listener);
        }
    })
});

 * Each task will execute after the previous one has failed or succeeded. Make a run method which calls
 * the required method (e.g. clearLocalServices, addLocalService, etc) and pass it the listener
 * received in the argument. You can also override the onSuccess and onFailure method of the
 * ActionListenerTask. Those (optional) methods will be executed when the task succeeds or fails.
 *
 * A task can also timeout. The default timeout is 10seconds. If the task times out the onTimeout
 * method will be executed, and the next task will be run (the onSuccess and onFailure methods
 * of that ActionListenerTask will not run).
 *
 * As Wifi P2P on Android can exhibit strange behaviours this class performs extensive logging.
 */
public class ActionListenerTaskQueue  {

    private Vector<ActionListenerTask> tasks;

    private Timer timeoutTimer;

    private volatile ActionListenerTask currentTask;

    private String queueName;

    private class TimeoutTimerTask extends TimerTask{

        private ActionListenerTask task;

        public TimeoutTimerTask(ActionListenerTask task) {
            this.task = task;
        }

        @Override
        public void run() {
            synchronized (ActionListenerTaskQueue.this) {
                synchronized (ActionListenerTaskQueue.this) {
                    task.setTimedOut(true);
                    task.onTimeout();
                    nextTask();
                }
            }
        }
    };

    public ActionListenerTaskQueue() {
        this("");
    }

    /**
     * Main constructor
     *
     * @param queueName The queue name - used for logging purposes
     */
    public ActionListenerTaskQueue(String queueName) {
        tasks = new Vector<>();
        this.queueName = queueName;
        timeoutTimer = new Timer("ActionListenerQueue Timeout Timer - " + queueName);
    }

    /**
     * Queue a task to run. If nothing is running now, it will run immediately. Otherwise it will
     * be added to the queue.
     *
     * @param task Task to execute
     * @return this
     */
    public ActionListenerTaskQueue queueTask(ActionListenerTask task) {
        synchronized (this) {
            Log.i(WifiDirectHandler.TAG, makeLogLineStart(task) + " queue");
            tasks.add(task);
        }

        nextTask();
        return this;
    }

    private void nextTask() {
        synchronized (this) {
            if(currentTask == null && tasks.size() > 0) {
                currentTask = tasks.elementAt(0);
                Log.i(WifiDirectHandler.TAG, makeLogLineStart(currentTask)
                        + " start task");
                final long startTime = Calendar.getInstance().getTimeInMillis();
                final ActionListenerTask task = currentTask;
                final TimerTask timeoutTimerTask = new TimeoutTimerTask(task);

                timeoutTimer.schedule(timeoutTimerTask, task.getTimeout());
                currentTask.run(new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        timeoutTimerTask.cancel();
                        Log.i(WifiDirectHandler.TAG, makeLogLineStart(currentTask)
                                + " success: " + formatTaskTime(startTime));
                        if(!task.isTimedOut()) {
                            task.onSuccess();
                            synchronized (ActionListenerTaskQueue.this) {
                                tasks.remove(currentTask);
                                currentTask = null;
                            }

                            nextTask();
                        }else {
                            Log.e(WifiDirectHandler.TAG, makeLogLineStart(currentTask)
                                    + " timed out - not running onSuccess handler");
                        }
                    }

                    @Override
                    public void onFailure(int i) {
                        timeoutTimerTask.cancel();
                        Log.e(WifiDirectHandler.TAG, makeLogLineStart(currentTask)
                                + " failure: reason" + FailureReason.fromInteger(i)
                                + " " + formatTaskTime(startTime));
                        if(!task.isTimedOut()) {
                            task.onFailure(i);
                            synchronized (ActionListenerTaskQueue.this) {
                                tasks.remove(currentTask);
                                currentTask = null;
                            }
                            nextTask();
                        }else {
                            Log.e(WifiDirectHandler.TAG, makeLogLineStart(currentTask)
                                    + " timed out - not running onFailure handler");
                        }
                    }
                });
            }
        }
    }

    private String formatTaskTime(long startTime) {
        return "took " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms";
    }


    private String makeLogLineStart(ActionListenerTask task) {
        return "ActionListenerTaskQueue (" + queueName + ") task: " +
                task.getName() + ": ";
    }

}
