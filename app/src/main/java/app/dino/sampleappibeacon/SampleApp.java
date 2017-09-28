package app.dino.sampleappibeacon;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.service.RangedBeacon;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * Created by DiNo on 28.09.2017.
 */

public class SampleApp extends Application implements BootstrapNotifier, BeaconConsumer, RangeNotifier {

    private static final String UUID = "699EBC80-E1F3-11E3-9A0F-0CF3EE3BC012";
    private static final String TAG = "BeaconBackground";
    private RegionBootstrap regionBootstrap;
    private BeaconManager beaconManager;

    @Override
    public void onCreate() {
        super.onCreate();
        initBeaconBackground();
        //Saving batteries
        BackgroundPowerSaver backgroundPowerSaver = new BackgroundPowerSaver(this);

        RangedBeacon.setSampleExpirationMilliseconds(5000);
    }

    private void initBeaconBackground() {
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // set scan period
        beaconManager.setForegroundScanPeriod(1000);
        beaconManager.setForegroundBetweenScanPeriod(1000);
        beaconManager.setBackgroundScanPeriod(1000);
        beaconManager.setBackgroundBetweenScanPeriod(1000);
        beaconManager.setBackgroundMode(true);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        Log.e(TAG, "onBeaconServiceConnect ");
        initRegion();
    }

    @Override
    public void didDetermineStateForRegion(int arg0, Region arg1) {
        try {
            if (arg1 != null && arg1.getId1() != null)
                Log.e(TAG, "didDetermineStateForRegion id=" + arg1.getId1().toString());
            beaconManager.stopRangingBeaconsInRegion(arg1);
            beaconManager.addRangeNotifier(this);
            try {
                beaconManager.startRangingBeaconsInRegion(arg1);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } catch (Exception ignore) {

        }
    }

    @Override
    public void didEnterRegion(Region region) {
        try {
            if (region.getId1() != null && region.getId1() != null) {
                Log.e(TAG, "didEnterRegion id=" + region.getId1().toString());
            } else {
                Log.e(TAG, "didEnterRegion");
            }

            beaconManager.addRangeNotifier(this);
            try {
                beaconManager.startRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } catch (Exception ignore) {

        }
    }

    @Override
    public void didExitRegion(Region region) {
        try {
            if (region != null) {
                if (region.getId1() != null && region.getId1() != null) {
                    Log.e(TAG, "didEnterRegion id=" + region.getId1().toString());
                } else {
                    Log.e(TAG, "didEnterRegion");
                }
                showNotification("iBeacon exit the region", region.getId1(), region.getId2(), region.getId3());
            }
        } catch (Exception ignore) {

        }
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        try {
            Log.e(TAG, "didRangeBeaconsInRegion=");
            if (beacons.iterator().next() != null) {
                Beacon beacon = beacons.iterator().next();
                if (beacon != null) {
                    showNotification("iBeacon detected", beacon.getId1(), beacon.getId2(), beacon.getId3());
                    beaconManager.stopRangingBeaconsInRegion(region);
                }
            }
        } catch (NoSuchElementException exp) {

        } catch (RemoteException e) {

        }
    }

    public void initRegion() {
        try {
            regionBootstrap = new RegionBootstrap(this, new Region("region" + UUID, Identifier.parse(UUID), null, null));
            if (beaconManager != null)
                beaconManager.bind(this);
        } catch (IllegalArgumentException exp) {
            exp.printStackTrace();
        }
    }

    private static final int NOTIFY_ID = 101;

    private void showNotification(String title, Identifier uuid, Identifier major, Identifier minor) {
        PowerManager pm = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if(!pm.isScreenOn()) {
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |PowerManager.ACQUIRE_CAUSES_WAKEUP |PowerManager.ON_AFTER_RELEASE,"MyLock");
            wl.acquire(5000);
            PowerManager.WakeLock wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyCpuLock");
            wl_cpu.acquire(5000);
        }
        PendingIntent activityPendingIntent = getActivityPendingIntent();

        NotificationCompat.Builder notificationbilder = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText("Накорми кота!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(activityPendingIntent)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setCategory(Notification.CATEGORY_STATUS);
        if (uuid != null && major != null && minor != null) {
            notificationbilder.setContentText("Major=" + major.toString() + " Minor=" + minor.toString() + " UUID=" + uuid.toString());
        } else {
            if (uuid != null) {
                notificationbilder.setContentText("UUID=" + uuid.toString());
            }
        }
        Notification notification = notificationbilder.build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFY_ID, notification);
    }
    private PendingIntent getActivityPendingIntent() {
        Intent activityIntent = new Intent(this, MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
