package org.yaxim.androidclient.service;

import java.util.HashMap;
import java.util.Map;

import org.yaxim.androidclient.chat.ChatWindow;
import org.yaxim.androidclient.data.YaximConfiguration;
import org.yaxim.androidclient.util.LogConstants;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.yaxim.androidclient.MainWindow;
import org.yaxim.androidclient.R;

public abstract class GenericService extends Service {

	private static final String TAG = "Service";
	private static final String APP_NAME = "yaxim";

	private NotificationManager mNotificationMGR;
	private Notification mNotification;
	private Vibrator mVibrator;
	private Intent mNotificationIntent;
	//private int mNotificationCounter = 0;
	private final int appNotificationId = -1;
	
	private Map<String, Integer> notificationCount = new HashMap<String, Integer>(2);
	private Map<String, Integer> notificationId = new HashMap<String, Integer>(2);
	private int lastNotificationId = 0;

	protected YaximConfiguration mConfig;

	@Override
	public IBinder onBind(Intent arg0) {
		Log.i(TAG, "called onBind()");
		return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "called onUnbind()");
		return super.onUnbind(intent);
	}

	@Override
	public void onRebind(Intent intent) {
		Log.i(TAG, "called onRebind()");
		super.onRebind(intent);
	}

	@Override
	public void onCreate() {
		Log.i(TAG, "called onCreate()");
		super.onCreate();
		mConfig = new YaximConfiguration(PreferenceManager
				.getDefaultSharedPreferences(this));
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		addNotificationMGR();
		createAppNotification();
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "called onDestroy()");
		super.onDestroy();
		destroyAppNotification();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.i(TAG, "called onStart()");
		super.onStart(intent, startId);
	}

	private void addNotificationMGR() {
		mNotificationMGR = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationIntent = new Intent(this, ChatWindow.class);
	}

	protected void notifyClient(String fromJid, String fromUserName, String message) {
		setNotification(fromJid, fromUserName, message);
		setLEDNotifivation();
		
		int notifyId = 0;
		if (notificationId.containsKey(fromJid)) {
			notifyId = notificationId.get(fromJid);
		} else {
			lastNotificationId++;
			notifyId = lastNotificationId;
			notificationId.put(fromJid, Integer.valueOf(notifyId));
		}
		mNotificationMGR.notify(notifyId, mNotification);
		
		vibraNotififaction();
	}
	
	private void setNotification(String fromJid, String fromUserId, String message) {
		
		int mNotificationCounter = 0;
		if (notificationCount.containsKey(fromJid)) {
			mNotificationCounter = notificationCount.get(fromJid);
		}
		mNotificationCounter++;
		notificationCount.put(fromJid, mNotificationCounter);

		String author;
		if (null == fromUserId || fromUserId.length() == 0 || fromJid.equals(fromUserId)) {
			author = fromJid;
		} else {
			author = fromUserId + " (" + fromJid + ")";
		}
		
		String title = getString(R.string.notification_message, author);
		mNotification = new Notification(R.drawable.icon, title,
				System.currentTimeMillis());

		Uri userNameUri = Uri.parse(fromJid);
		mNotificationIntent.setData(userNameUri);
		mNotificationIntent.putExtra(ChatWindow.INTENT_EXTRA_USERNAME, fromUserId);
		
		//need to set flag FLAG_UPDATE_CURRENT to get extras transferred
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		mNotification.setLatestEventInfo(this, title, message, pendingIntent);
		mNotification.number = mNotificationCounter;
		mNotification.flags = Notification.FLAG_AUTO_CANCEL;
	}

	private void setLEDNotifivation() {
		if (mConfig.isLEDNotify) {
			mNotification.flags |= Notification.DEFAULT_LIGHTS;
			mNotification.ledARGB = Color.MAGENTA;
			mNotification.ledOnMS = 300;
			mNotification.ledOffMS = 1000;
			mNotification.flags |= Notification.FLAG_SHOW_LIGHTS;
		}
	}

	private void vibraNotififaction() {
		//TODO instead of vibrating here, we should let the notification do that. There seems to be a bug in there though
		if (mConfig.isVibraNotify) {
			AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
			boolean vibrate = am.shouldVibrate(AudioManager.VIBRATE_TYPE_NOTIFICATION);
			if (vibrate) {
				mVibrator.vibrate(400);
			}
		}
	}

	protected void shortToastNotify(String msg) {
		Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	public void resetNotificationCounter(String userJid) {
		notificationCount.remove(userJid);
	}

	protected void logError(String data) {
		if (LogConstants.LOG_ERROR) {
			Log.e(TAG, data);
		}
	}

	protected void logInfo(String data) {
		if (LogConstants.LOG_ERROR) {
			Log.e(TAG, data);
		}
	}

	private void createAppNotification() {
		if (mConfig.isAppNotify) {
			
			Notification appNotification = new Notification(R.drawable.icon, getString(R.string.notification_text), System.currentTimeMillis());
			Intent mainWindowIntent = new Intent(this, MainWindow.class);
			
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
					mainWindowIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			appNotification.setLatestEventInfo(this, getString(R.string.notification_text), "", pendingIntent);
			appNotification.flags = Notification.FLAG_NO_CLEAR
				| Notification.FLAG_ONGOING_EVENT;
			mNotificationMGR.notify(appNotificationId, appNotification);
		}		
	}
	
	private void destroyAppNotification() {
		if (mConfig.isAppNotify) {
			Log.i(TAG, "Destroying app config");
			mNotificationMGR.cancel(appNotificationId);
		}
	}
}