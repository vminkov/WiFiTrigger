package com.intuitics.wifitrigger;

import android.content.ContextWrapper;

import android.net.wifi.WifiManager;
import java.lang.reflect.Method;
import android.R;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class TriggerService extends IntentService {
	private static final String TAG = "WiFiTrigger";
	public static final int NOTIFICATION_ID = 1;

	private NotificationManager mNotificationManager;
	NotificationCompat.Builder builder;

	public TriggerService() {
		super("GcmIntentService");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	public void onDestroy() {
		Toast.makeText(this, "WiFiTrigger Stopped", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onDestroy");
	}

	@Override
	public void onStart(Intent intent, int startid) {
		Intent intents = new Intent(getBaseContext(), MainActivity.class);
		intents.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intents);
		Toast.makeText(this, "WiFiTrigger Started", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onStart");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		// The getMessageType() intent parameter must be the intent you received
		// in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) { // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that
			 * GCM will be extended in the future with new message types, just
			 * ignore any message types you're not interested in, or that you
			 * don't recognize.
			 */
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
					.equals(messageType)) {
				sendNotification("Send error: " + extras.toString());
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
					.equals(messageType)) {
				sendNotification("Deleted messages on server: "
						+ extras.toString());
				// If it's a regular GCM message, do some work.
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
					.equals(messageType)) {
				// This loop represents the service doing some work.

				boolean on = switchTethering();

				Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
				// Post notification of received message.
				sendNotification("Received: " + extras.toString()
						+ " and the tethering is now " + ((on) ? "on" : "off"));
				Log.i(TAG, "Received: " + extras.toString());
			}
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GCNBroadcastReceiver.completeWakefulIntent(intent);
	}

	private boolean switchTethering() {
		// turn on/off the tethering

		ContextWrapper context = this;
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(context.WIFI_SERVICE);

		Method[] methods = wifiManager.getClass().getDeclaredMethods();
		boolean enabled = false;
		for (Method method : methods) {
			if (method.getName().equals("isWifiApEnabled")) {
				try {
					enabled = (Boolean) method.invoke(wifiManager);
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
				}
				break;
			}
		}
		for (Method method : methods) {
			if (method.getName().equals("setWifiApEnabled")) {
				try {
					method.invoke(wifiManager, null, !enabled);
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
				}
				break;
			}
		}

		return enabled;
	}

	// Put the message into a notification and post it.
	// This is just one simple example of what you might choose to do with
	// a GCM message.
	private void sendNotification(String msg) {
		mNotificationManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, MainActivity.class), 0);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.btn_default)
				.setContentTitle("GCM Notification")
				.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
				.setContentText(msg);

		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

}