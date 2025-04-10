import 'smdt_platform_interface.dart';

class Smdt {
  Future<String?> smdtGetCurrentNetType() {
    return SmdtPlatform.instance.smdtGetCurrentNetType();
  }

  Future<void> smdtSetStatusBar(bool enable) {
    return SmdtPlatform.instance.smdtSetStatusBar(enable);
  }

  Future<void> smdtSetNavigationBar(bool enable) {
    return SmdtPlatform.instance.smdtSetNavigationBar(enable);
  }

  Future<int?> smdtGetVolume() {
    return SmdtPlatform.instance.smdtGetVolume();
  }

  Future<void> smdtSetVolume(int volume) {
    return SmdtPlatform.instance.smdtSetVolume(volume);
  }

  Future<void> smdtReboot() {
    return SmdtPlatform.instance.smdtReboot();
  }

  Future<void> smdtShutdown() {
    return SmdtPlatform.instance.smdtShutdown();
  }

  Future<String?> getLogCat() {
    return SmdtPlatform.instance.getLogCat();
  }

  Future<void> smdtInstall(String apkPath) {
    return SmdtPlatform.instance.smdtInstall(apkPath);
  }

  Future<bool> sendLogCatToServer(String serverUrl, String accessToken) {
    return SmdtPlatform.instance.sendLogCatToServer(serverUrl, accessToken);
  }

  Future<Map<String, dynamic>> getMemoryStats() {
    return SmdtPlatform.instance.getMemoryStats();
  }

  static SmdtPlatform get instance => SmdtPlatform.instance;
}
