package com.github.ReactSextant.mupdfmini;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.LinkedBlockingQueue;
import com.facebook.react.bridge.UiThreadUtil;

public class Worker implements Runnable
{
	public static class Task implements Runnable {
		public void work() {} /* The 'work' method will be executed on the background thread. */
		public void run() {} /* The 'run' method will be executed on the UI thread. */
	}

	protected Context context;
	protected LinkedBlockingQueue<Task> queue;
	protected boolean alive;

	public Worker(Context ctx) {
		context = ctx;
		queue = new LinkedBlockingQueue<Task>();
	}

	public void start() {
		alive = true;
		new Thread(this).start();
	}

	public void stop() {
		alive = false;
	}

	public void add(Task task) {
		try {
			queue.put(task);
		} catch (InterruptedException x) {
			Log.e("MuPDF Worker", x.getMessage());
		}
	}

	public void run() {
		while (alive) {
			try {
				Task task = queue.take();
				task.work();
				UiThreadUtil.runOnUiThread(task);
			} catch (final Throwable x) {
				Log.e("MuPDF Worker", x.getMessage());
				UiThreadUtil.runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(context, x.getMessage(), Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
	}
}
