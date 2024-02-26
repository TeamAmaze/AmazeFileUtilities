package com.amaze.fileutilities;
import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.amaze.fileutilities.home_page.MainActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

@RequiresApi(api = Build.VERSION_CODES.M)
public class PermissionsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private Logger log = LoggerFactory.getLogger(PermissionsActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void invokePermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && !checkStoragePermission()) {
                requestStoragePermission(onPermissionGranted, true);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestAllFilesAccess(onPermissionGranted);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !checkNotificationPermission()) {
                requestNotificationPermission(onPermissionGranted, true);
            }
        }
    }

    private boolean checkNotificationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean haveStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkStoragePermission();
        }
        return true;
    }

    public static final int PERMISSION_LENGTH = 5;
    public static final int STORAGE_PERMISSION = 0;
    public static final int ALL_FILES_PERMISSION = 1;
    public static final int LOCATION_PERMISSION = 2;
    public static final int NOTIFICATION_PERMISSION = 3;

    public final PermissionsActivity.OnPermissionGranted[] permissionCallbacks = new PermissionsActivity.OnPermissionGranted[PERMISSION_LENGTH];

    public final PermissionsActivity.OnPermissionGranted onPermissionGranted = isGranted -> {
        if (isGranted) {
            Intent action = new Intent(PermissionsActivity.this, MainActivity.class);
            action.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(action);
            finish();
        } else {
            Toast.makeText(PermissionsActivity.this, R.string.grantfailed, Toast.LENGTH_SHORT).show();
            requestStoragePermission(permissionCallbacks[STORAGE_PERMISSION], false);
        }
        permissionCallbacks[STORAGE_PERMISSION] = null;
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION) {
            permissionCallbacks[STORAGE_PERMISSION].onPermissionGranted(isGranted(grantResults));
        } else if (requestCode == LOCATION_PERMISSION) {
            if (isGranted(grantResults)) {
                permissionCallbacks[LOCATION_PERMISSION].onPermissionGranted(true);
                permissionCallbacks[LOCATION_PERMISSION] = null;
            } else if (requestCode == NOTIFICATION_PERMISSION &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!isGranted(grantResults)) {
                    Toast.makeText(this, R.string.grantfailed, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.grant_location_failed, Toast.LENGTH_SHORT).show();
                requestStoragePermission(permissionCallbacks[LOCATION_PERMISSION], false);
                permissionCallbacks[LOCATION_PERMISSION].onPermissionGranted(false);
                permissionCallbacks[LOCATION_PERMISSION] = null;
            }
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return (ActivityCompat.checkSelfPermission(this, Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    == PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(this, Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    == PackageManager.PERMISSION_GRANTED) || Environment.isExternalStorageManager();
        }
        return false;
    }

    public void isLocationEnabled(PermissionsActivity.OnPermissionGranted onPermissionGranted) {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps(onPermissionGranted);
            onPermissionGranted.onPermissionGranted(false);
        } else {
            onPermissionGranted.onPermissionGranted(true);
        }
    }

    private void buildAlertMessageNoGps(PermissionsActivity.OnPermissionGranted onPermissionGranted) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Custom_Dialog_Dark);
        builder.setMessage(getResources().getString(R.string.gps_disabled))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    dialog.cancel();
                })
                .setNegativeButton(getResources().getString(R.string.no), (dialog, which) -> {
                    onPermissionGranted.onPermissionGranted(false);
                    dialog.cancel();
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void initLocationResources(PermissionsActivity.OnPermissionGranted onPermissionGranted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkLocationPermission()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Custom_Dialog_Dark);
            builder.setMessage(R.string.grant_location_permission)
                    .setTitle(R.string.grant_permission)
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    }).setCancelable(false);
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION, builder, onPermissionGranted, true);
            onPermissionGranted.onPermissionGranted(false);
        } else {
            onPermissionGranted.onPermissionGranted(true);
        }
    }

    private boolean checkLocationPermission() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission(PermissionsActivity.OnPermissionGranted onPermissionGranted, boolean isInitialStart) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Custom_Dialog_Dark);
        builder.setMessage(R.string.grant_storage_read_permission)
                .setTitle(R.string.grant_permission)
                .setNegativeButton(R.string.cancel, (dialog, which) -> finish()).setCancelable(false);
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION, builder, onPermissionGranted, isInitialStart);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestNotificationPermission(PermissionsActivity.OnPermissionGranted onPermissionGranted, boolean isInitialStart) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Custom_Dialog_Dark);
        builder.setMessage(R.string.grant_notification_permission)
                .setTitle(R.string.grant_permission)
                .setNegativeButton(R.string.cancel, (dialog, which) -> finish()).setCancelable(false);
        requestPermission(Manifest.permission.POST_NOTIFICATIONS, NOTIFICATION_PERMISSION, builder, onPermissionGranted, isInitialStart);
    }

    private void requestPermission(String permission, int code, AlertDialog.Builder rationale, PermissionsActivity.OnPermissionGranted onPermissionGranted, boolean isInitialStart) {
        permissionCallbacks[code] = onPermissionGranted;
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            rationale.setPositiveButton(R.string.grant, (dialog, which) -> {
                ActivityCompat.requestPermissions(PermissionsActivity.this, new String[]{permission}, code);
                dialog.cancel();
            });
            rationale.show();
        } else if (isInitialStart) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, code);
        } else {
            showToastInCenter(getString(R.string.grantfailed));
            finish();
        }
    }

    private void requestAllFilesAccess(PermissionsActivity.OnPermissionGranted onPermissionGranted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Custom_Dialog_Dark);
            builder.setMessage(R.string.grant_all_files_permission)
                    .setTitle(R.string.grant_permission)
                    .setNegativeButton(R.string.cancel, (dialog, which) -> finish())
                    .setPositiveButton(R.string.grant, (dialog, which) -> {
                        permissionCallbacks[ALL_FILES_PERMISSION] = onPermissionGranted;
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    .setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        } catch (ActivityNotFoundException anf) {
                            log.warn("Failed to find activity for all files access", anf);
                            try {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                        .setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            } catch (Exception e) {
                                log.error("Failed to initial activity to grant all files access", e);
                                showToastInCenter(getString(R.string.grantfailed));
                            }
                        } catch (Exception e) {
                            log.error("Failed to grant all files access", e);
                            showToastInCenter(getString(R.string.grantfailed));
                        }
                        dialog.cancel();
                    }).setCancelable(false).show();
        }
    }

    private boolean isGranted(int[] grantResults) {
        return grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    public interface OnPermissionGranted {
        void onPermissionGranted(boolean isGranted);
    }

/*
    public static void showToastInCenter(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }*/

    public void showToastInCenter(String message) {
        Toast.makeText(PermissionsActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}

