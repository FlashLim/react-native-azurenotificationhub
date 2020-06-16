package com.azure.reactnative.notificationhub;

import android.content.Context;
import android.content.Intent;

import androidx.core.app.JobIntentService;

import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.microsoft.windowsazure.messaging.NotificationHub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReactNativeFCMRegisterIntentService extends JobIntentService {

    public static final String TAG = "ReactNativeRegistration";

    private static final int JOB_ID = 1000;

    private final ExecutorService mPool = Executors.newFixedThreadPool(1);

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, ReactNativeFCMRegisterIntentService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(Intent intent) {
        final Intent event = ReactNativeNotificationHubUtil.IntentFactory.createIntent(TAG);
        final ReactNativeNotificationHubUtil notificationHubUtil = ReactNativeNotificationHubUtil.getInstance();
        final String storedToken = notificationHubUtil.getFCMToken(this);

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(
                mPool, new OnSuccessListener<InstanceIdResult>() {
                    @Override
                    public void onSuccess(InstanceIdResult instanceIdResult) {
                        try {
                            String token = instanceIdResult.getToken();
                            Log.d(TAG, "FCM Registration Token: " + token);

                            // Storing the registration ID indicates whether the generated token has been
                            // sent to your server. If it is not stored, send the token to your server.
                            // Also check if the token has been compromised and needs refreshing.
                            if (storedToken != token) {
                                notificationHubUtil.setFCMToken(ReactNativeFCMRegisterIntentService.this, token);
                            }
                            event.putExtra(
                                    ReactNativeConstants.KEY_INTENT_EVENT_NAME,
                                    ReactNativeConstants.EVENT_FCM_TOKEN_RECEIVED);
                            event.putExtra(
                                    ReactNativeConstants.KEY_INTENT_EVENT_TYPE,
                                    ReactNativeConstants.INTENT_EVENT_TYPE_STRING);
                            event.putExtra(
                                    ReactNativeConstants.KEY_INTENT_EVENT_STRING_DATA, token);
                            ReactNativeNotificationsHandler.sendBroadcast(
                                    ReactNativeFCMRegisterIntentService.this, event, 0);

                            // Create notification handler
                            ReactNativeFirebaseMessagingService.createNotificationChannel(
                                    ReactNativeFCMRegisterIntentService.this);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to complete token refresh", e);

                            event.putExtra(
                                    ReactNativeConstants.KEY_INTENT_EVENT_NAME,
                                    ReactNativeConstants.EVENT_FCM_TOKEN_ERROR);
                            event.putExtra(
                                    ReactNativeConstants.KEY_INTENT_EVENT_TYPE,
                                    ReactNativeConstants.INTENT_EVENT_TYPE_STRING);
                            event.putExtra(ReactNativeConstants.KEY_INTENT_EVENT_STRING_DATA, e.getMessage());
                            ReactNativeNotificationsHandler.sendBroadcast(
                                    ReactNativeFCMRegisterIntentService.this, event, 0);
                        }
                    }
                });
    }
}
