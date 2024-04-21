package com.mrboomdev.awery.extensions.support.js;

import static com.mrboomdev.awery.app.AweryLifecycle.getAnyActivity;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.mrboomdev.awery.app.CrashHandler;
import com.mrboomdev.awery.sdk.util.Callbacks;

public class JsTask {
	private static final String TAG = "JsTask";
	protected static final int LOAD_ALL_EXTENSIONS = 1;
	protected static final int LOAD_EXTENSION = 2;
	protected static final int POST_RUNNABLE = 3;
	private final int type;
	private final Object[] args;
	private final Callbacks.Callback1<Object> callback;

	protected JsTask(int type, Callbacks.Callback1<Object> callback, Object... args) {
		this.type = type;
		this.args = args;
		this.callback = callback;
	}

	protected JsTask(Runnable runnable) {
		this.type = POST_RUNNABLE;
		this.args = new Object[0];

		this.callback = o -> {
			if(o instanceof Throwable t) {
				CrashHandler.showErrorDialog(
						getAnyActivity(AppCompatActivity.class),
						t, false, null);

				Log.e(TAG, "Returned exception, ignoring the response.", t);
				return;
			}

			runnable.run();
		};
	}

	protected void resolve(Object o) {
		callback.run(o);
	}

	protected Object[] getArgs() {
		return args;
	}

	protected int getTaskType() {
		return type;
	}
}