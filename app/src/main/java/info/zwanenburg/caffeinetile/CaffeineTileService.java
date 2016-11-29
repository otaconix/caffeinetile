package info.zwanenburg.caffeinetile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class CaffeineTileService extends TileService {
    private static final String LOG_TAG = CaffeineTileService.class.getSimpleName();
    private Tile tile;
    private PowerManager.WakeLock wakeLock;
    private ScreenOffReceiver screenOffReceiver = new ScreenOffReceiver();
    private Icon activatedIcon;
    private Icon deactivatedIcon;

    @Override
    public void onCreate() {
        wakeLock = getSystemService(PowerManager.class).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getPackageName());
        wakeLock.setReferenceCounted(false);
        screenOffReceiver.init();
        loadIcons();
    }

    private void loadIcons() {
        activatedIcon = Icon.createWithResource(this, R.drawable.ic_coffee_on);
        deactivatedIcon = Icon.createWithResource(this, R.drawable.ic_coffee_off);
    }

    @Override
    public void onTileAdded() {
        startService(new Intent(getApplicationContext(), getClass()));
    }

    @Override
    public void onTileRemoved() {
        wakeLock.release();
        stopService(new Intent(getApplicationContext(), getClass()));
    }

    @Override
    public void onDestroy() {
        screenOffReceiver.destroy();
    }

    @Override
    public void onStartListening() {
        tile = getQsTile();
        updateTile();
    }

    private void updateTile() {
        Log.d(LOG_TAG, "Updating tile");

        if (wakeLock.isHeld()) {
            tile.setIcon(activatedIcon);
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            tile.setIcon(deactivatedIcon);
            tile.setState(Tile.STATE_INACTIVE);
        }

        tile.updateTile();
    }

    @Override
    public void onClick() {
        toggleActive();
    }

    private void toggleActive() {
        if (wakeLock.isHeld()) {
            Log.d(LOG_TAG, "Disabling caffeine");
            wakeLock.release();
        } else {
            Log.d(LOG_TAG, "Enabling caffeine");
            wakeLock.acquire();
        }

        updateTile();
    }

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
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CaffeineTileService.this);
                boolean keepActive = sharedPreferences.getBoolean("keep_active", getResources().getBoolean(R.bool.pref_keep_active_default));
                if (wakeLock.isHeld() && !keepActive) {
                    toggleActive();
                }
            }
        }

        public void destroy() {
            getApplicationContext().unregisterReceiver(this);
        }
    }
}
