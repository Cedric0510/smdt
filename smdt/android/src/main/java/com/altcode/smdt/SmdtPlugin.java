package com.altcode.smdt;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import android.os.Message;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;
import android.os.Bundle;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import android.app.smdt.SmdtManager;
import android.view.View;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/** SmdtPlugin */
public class SmdtPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native
  /// Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine
  /// and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private SmdtManager smdt;
  private FlutterPluginBinding flutterPluginBinding;
  private Activity activity;
  private static final String TAG = "SmdtPlugin";

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "smdt");
    channel.setMethodCallHandler(this);

    /// Création du plugin SmdtManager
    smdt = SmdtManager.create(flutterPluginBinding.getApplicationContext());
    this.flutterPluginBinding = flutterPluginBinding;
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    this.activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    this.activity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    this.activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivity() {
    this.activity = null;
  }

  private double getAndroidVersion() {
    return Float.parseFloat(smdt.getAndroidVersion());
  }

  private String getFullLogcatContent() {
    try {
        Process process = Runtime.getRuntime().exec("logcat -d");
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        
        StringBuilder log = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            log.append(line).append("\n");
        }
        
        Log.d(TAG, "Récupération de " + log.length() + " octets de logs");
        return log.toString();
    }
    catch (IOException e) {
        Log.e(TAG, "Erreur lors de la récupération du logcat", e);
        return "";
    }
  }

  private void sendLogCatToServer(MethodCall call, Result result) {
    try {
        String serverUrl = call.argument("serverUrl");
        String accessToken = call.argument("accessToken");
        
        if (serverUrl == null || accessToken == null) {
            result.error("INVALID_ARGUMENTS", "serverUrl and accessToken required", null);
            return;
        }
        
        // Cette partie sera exécutée en arrière-plan pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                final String logcatContent = getFullLogcatContent();
                
                URL url = new URL(serverUrl + "/api/totem/app.logcat");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/octet-stream");
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setDoOutput(true);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = logcatContent.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = connection.getResponseCode();
                
                // Ramener le résultat sur le thread principal
                final int finalResponseCode = responseCode;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (finalResponseCode == 200) {
                        result.success(true);
                    } else {
                        result.error("HTTP_ERROR", "Erreur HTTP: " + finalResponseCode, null);
                    }
                });
                
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    result.error("EXCEPTION", e.getMessage(), null);
                });
            }
        }).start();
    } catch (Exception e) {
        result.error("EXCEPTION", e.getMessage(), null);
    }
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    try {
      switch (call.method) {
        /// (void) smdtSetStatusBar(boolean)
        case "smdtSetStatusBar":
          final Boolean enable = call.argument("enable");
          smdt.smdtSetStatusBar(flutterPluginBinding.getApplicationContext(), enable);
          if (getAndroidVersion() > 10.0) {
            smdt.setGestureBar(enable);
          }
          result.success(null);
          break;

        /// (string) smdtGetCurrentNetType
        case "smdtGetCurrentNetType":
          result.success(smdt.getCurrentNetType());
          break;

        /// (int) smdtGetVolume
        case "smdtGetVolume":
          result.success(smdt.smdtGetVolume(flutterPluginBinding.getApplicationContext()));
          break;

        /// (void) smdtSetVolume(int)
        case "smdtSetVolume":
          final int volume = call.argument("volume");
          smdt.smdtSetVolume(flutterPluginBinding.getApplicationContext(), volume);
          result.success(null);
          break;

        /// (void) smdtReboot
        case "smdtReboot":
          smdt.smdtReboot();
          result.success(null);
          break;

        /// (void) smdtShutdown
        case "smdtShutdown":
          smdt.shutDown();
          result.success(null);
          break;

        /// (void) smdtInstall(string)
        case "smdtInstall":
          final String apkPath = call.argument("apkPath");
          /// Démarre un thread pour l'installation silencieuse
          Thread t = new Thread() {
            @Override
            public void run() {
              if (!apkPath.equals("")) {

                smdt.smdtSilentInstall(apkPath, flutterPluginBinding.getApplicationContext());
                Message msg = new Message();
                msg.what = 0x123;
              }
            }
          };
          t.start();
          result.success(null);
          break;

        /// (string) getLogCat
        case "getLogCat":
        ProcessBuilder processBuilder = new ProcessBuilder("logcat", "-d", "-D");
        Process process = processBuilder.start();
          BufferedReader bufferedReader = new BufferedReader(
              new InputStreamReader(process.getInputStream()));

          StringBuilder log = new StringBuilder();
          String line = "";
          while ((line = bufferedReader.readLine()) != null) {
            log.append(line);
            log.append("\n");
          }

          result.success(log.toString());
          break;

        /// (void) smdtSetNavigationBar(boolean)
        case "smdtSetNavigationBar":
          final Boolean enableNavBar = call.argument("enable");
          
          // Obtenir la version Android
          double androidVersion = getAndroidVersion();
          
          // Pour Android 10+, utiliser setGestureBar
          if (androidVersion >= 10.0) {
            smdt.setGestureBar(!enableNavBar);
          }
          
          // Pour Android 7-9, utiliser une approche spécifique
          if (androidVersion < 8.0) {
            hideNavigationBarAndroid7(enableNavBar);
          } else {
            // Pour Android 8-9 utiliser l'approche standard
            if (activity != null) {
              activity.runOnUiThread(() -> {
                View decorView = activity.getWindow().getDecorView();
                int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                       | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                       | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

                if (!enableNavBar) {
                  flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                }
                
                decorView.setSystemUiVisibility(flags);
              });
            }
          }
          
          result.success(null);
          break;

        case "isSmdtAvailable":
          checkSmdtAvailable(result);
          break;

        case "sendLogCatToServer":
          sendLogCatToServer(call, result);
          break;

        case "getMemoryStats":
          getMemoryStats(result);
          break;

        case "getAndroidVersion":
          try {
            // Essayer d'utiliser le SmdtManager en premier
            result.success(smdt.getAndroidVersion());
          } catch (Exception e) {
            // Fallback sur la version système en cas d'erreur
            result.success(android.os.Build.VERSION.RELEASE);
          }
          break;

        default:
          result.notImplemented();
      }
    } catch (Exception e) {
      result.error("Exception", e.getMessage(), null);
    }
  }

  private void checkSmdtAvailable(@NonNull Result result) {
    try {
      Class.forName("android.app.smdt.SmdtManager");
      result.success(true);
    } catch (Exception e) {
      result.success(false);
    }
  }

  private void getMemoryStats(@NonNull Result result) {
    try {
      Map<String, Object> memoryInfo = new HashMap<>();
      ActivityManager activityManager = (ActivityManager) flutterPluginBinding.getApplicationContext()
          .getSystemService(Context.ACTIVITY_SERVICE);
      ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
      activityManager.getMemoryInfo(memInfo);
      
      // Convertir en MB pour lisibilité
      long totalMemInMb = memInfo.totalMem / (1024 * 1024);
      long availMemInMb = memInfo.availMem / (1024 * 1024);
      long usedMemInMb = totalMemInMb - availMemInMb;
      
      // Obtenir la mémoire utilisée par l'application
      Debug.MemoryInfo appMemInfo = new Debug.MemoryInfo();
      Debug.getMemoryInfo(appMemInfo);
      int appMemInMb = appMemInfo.getTotalPss() / 1024; // Pss en KB -> MB
      
      memoryInfo.put("totalRam", totalMemInMb);
      memoryInfo.put("availableRam", availMemInMb);
      memoryInfo.put("usedRam", usedMemInMb);
      memoryInfo.put("appMemory", appMemInMb);
      
      // Tenter d'obtenir des infos GPU si possible
      try {
        // Cette approche est expérimentale et peut ne pas fonctionner sur tous les appareils
        Process process = Runtime.getRuntime().exec("dumpsys gfxinfo " + flutterPluginBinding.getApplicationContext().getPackageName());
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.contains("Total GPU memory")) {
            String[] parts = line.split(":");
            if (parts.length > 1) {
              String gpuMemStr = parts[1].trim().split(" ")[0];
              try {
                float gpuMemMb = Float.parseFloat(gpuMemStr);
                memoryInfo.put("gpuMemory", gpuMemMb);
              } catch (NumberFormatException e) {
                // Ignorer si pas un nombre
              }
            }
            break;
          }
        }
      } catch (Exception e) {
        Log.e(TAG, "Erreur lors de la récupération des infos GPU", e);
      }
      
      result.success(memoryInfo);
    } catch (Exception e) {
      Log.e(TAG, "Erreur lors de la récupération des stats mémoire", e);
      result.error("MEMORY_ERROR", e.getMessage(), null);
    }
  }

  private void hideNavigationBarAndroid7(Boolean enable) {
    if (activity == null) return;
    
    activity.runOnUiThread(() -> {
      try {
        View decorView = activity.getWindow().getDecorView();
        
        if (!enable) {
          // Solution agressive pour Android 7 qui combine plusieurs approches
          // 1. Flags système standard mais avec IMMERSIVE au lieu de IMMERSIVE_STICKY
          int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
              | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_FULLSCREEN
              | View.SYSTEM_UI_FLAG_IMMERSIVE;
          
          decorView.setSystemUiVisibility(flags);
          
          // 2. Utiliser SMDT manager pour désactiver la barre si disponible
          try {
            smdt.smdtSetStatusBar(activity, false);
          } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la désactivation supplémentaire pour Android 7: " + e.getMessage());
          }
          
          // 3. Configuration périodique pour éviter la réapparition
          new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (activity != null && !activity.isFinishing()) {
              decorView.setSystemUiVisibility(flags);
            }
          }, 1000);
        } else {
          // Réactiver les barres système
          decorView.setSystemUiVisibility(0);
        }
      } catch (Exception e) {
        Log.e(TAG, "Erreur dans hideNavigationBarAndroid7: " + e.getMessage());
      }
    });
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }
}
