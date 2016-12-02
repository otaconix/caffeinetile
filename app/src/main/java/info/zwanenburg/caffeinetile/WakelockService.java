package info.zwanenburg.caffeinetile;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Service that holds a wakelock which can be toggled between "held" and "released" states.
 */
public class WakelockService extends Service {
    private static final String LOG_TAG = WakelockService.class.getSimpleName();
    private PowerManager.WakeLock wakeLock;
    private ScreenOffReceiver screenOffReceiver = new ScreenOffReceiver();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        wakeLock = getSystemService(PowerManager.class).newWakeLock(PowerManager
                .SCREEN_DIM_WAKE_LOCK, "Caffeine tile");
        wakeLock.setReferenceCounted(false);
        screenOffReceiver.init();
    }

    @Override
    public void onDestroy() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        screenOffReceiver.destroy();
    }

    /**
     * @return an instance of {@link WakelockService.Binder}, through it's possible to access
     * this service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    /**
     * @return whether the wakelock is currently held.
     */
    public boolean isActive() {
        return wakeLock.isHeld();
    }

    /**
     * Toggle the wakelock state (held -> released, or released -> held)
     */
    public void toggle() {
        if (wakeLock.isHeld()) {
            Log.d(LOG_TAG, "Disabling caffeine");
            wakeLock.release();
        } else {
            Log.d(LOG_TAG, "Enabling caffeine");
            wakeLock.acquire();
        }
    }

    /**
     * Binder through which it's possible to access this service.
     */
    public class Binder extends android.os.Binder {
        public WakelockService getService() {
            return WakelockService.this;
        }
    }

    /**
     * BroadcastReceiver that handles {@link Intent#ACTION_SCREEN_OFF} actions, by releasing the
     * wakelock (depending on current preferences).
     */
    private class ScreenOffReceiver extends BroadcastReceiver {
        public void init() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            getApplicationContext().registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(WakelockService.this);
                boolean keepActive = sharedPreferences.getBoolean("keep_active", getResources()
                        .getBoolean(R.bool.pref_keep_active_default));
                if (wakeLock.isHeld() && !keepActive) {
                    toggle();
                }
            }
        }

        public void destroy() {
            getApplicationContext().unregisterReceiver(this);
        }
    }
}
