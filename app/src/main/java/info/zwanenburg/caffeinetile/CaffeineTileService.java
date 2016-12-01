package info.zwanenburg.caffeinetile;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.util.Optional;

public class CaffeineTileService extends TileService {
    private static final String LOG_TAG = CaffeineTileService.class.getSimpleName();
    private Tile tile;
    private Icon activatedIcon;
    private Icon deactivatedIcon;
    private Optional<WakelockService> wakelockService = Optional.empty();
    private ServiceConnection serviceConnection;

    @Override
    public void onCreate() {
        loadIcons();
    }

    private void loadIcons() {
        activatedIcon = Icon.createWithResource(this, R.drawable.ic_coffee_on);
        deactivatedIcon = Icon.createWithResource(this, R.drawable.ic_coffee_off);
    }

    @Override
    public void onTileAdded() {
        startService(new Intent(getApplicationContext(), WakelockService.class));
    }

    @Override
    public void onTileRemoved() {
        stopService(new Intent(getApplicationContext(), WakelockService.class));
    }

    @Override
    public void onStartListening() {
        tile = getQsTile();
        updateTile();
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                wakelockService = Optional.of(((WakelockService.Binder) iBinder).getService());
                updateTile();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                wakelockService = Optional.empty();
                updateTile();
            }
        };
        bindService(new Intent(getApplicationContext(), WakelockService.class),
                serviceConnection, 0);
    }

    @Override
    public void onStopListening() {
        unbindService(serviceConnection);
    }

    private void updateTile() {
        Log.d(LOG_TAG, "Updating tile");

        tile.setIcon(deactivatedIcon);
        tile.setState(Tile.STATE_UNAVAILABLE);

        wakelockService.ifPresent(s -> {
            if (s.isActive()) {
                tile.setIcon(activatedIcon);
                tile.setState(Tile.STATE_ACTIVE);
            } else {
                tile.setIcon(deactivatedIcon);
                tile.setState(Tile.STATE_INACTIVE);
            }
        });

        tile.updateTile();
    }

    @Override
    public void onClick() {
        wakelockService.ifPresent(WakelockService::toggle);
        updateTile();
    }
}
