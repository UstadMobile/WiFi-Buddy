package edu.rit.se.wifibuddy;

import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * Created by mike on 9/8/17.
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

    public ActionListenerTaskQueue(String queueName) {
        tasks = new Vector<>();
        this.queueName = queueName;
        timeoutTimer = new Timer("ActionListenerQueue Timeout Timer - " + queueName);
    }

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
