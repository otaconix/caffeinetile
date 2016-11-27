package info.zwanenburg.caffeinetile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.PowerManager;
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
        super.onCreate();
        getApplicationContext().startService(new Intent(getApplicationContext(), getClass()));
        Log.i(LOG_TAG, "TileService created");
        wakeLock = getApplicationContext().getSystemService(PowerManager.class).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Caffeine");
        wakeLock.setReferenceCounted(false);
        screenOffReceiver.init();
        loadIcons();
    }

    private void loadIcons() {
        activatedIcon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_coffee_on);
        deactivatedIcon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_coffee_off);
    }

    @Override
    public void onTileRemoved() {
        wakeLock.release();
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "TileService destroyed");
        screenOffReceiver.destroy();
    }

    @Override
    public void onStartListening() {
        Log.i(LOG_TAG, "Start listening");
        tile = getQsTile();
        updateTile();
    }

    private void updateTile() {
        Log.i(LOG_TAG, "Updating tile");

        if (wakeLock.isHeld()) {
            Log.i(LOG_TAG, "Caffeine is active, updating tile accordingly");
            tile.setIcon(activatedIcon);
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            Log.i(LOG_TAG, "Caffeine is inactive, updating tile accordingly");
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
        Log.i(LOG_TAG, "Toggling active state");

        if (wakeLock.isHeld()) {
            Log.i(LOG_TAG, "Disabling caffeine");
            wakeLock.release();
        } else {
            Log.i(LOG_TAG, "Enabling caffeine");
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
                if (wakeLock.isHeld()) {
                    toggleActive();
                }
            }
        }

        public void destroy() {
            getApplicationContext().unregisterReceiver(this);
        }
    }
}
