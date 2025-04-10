import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

import 'smdt_platform_interface.dart';

/// An implementation of [SmdtPlatform] that uses method channels.
class MethodChannelSmdt extends SmdtPlatform {
  /// The method channel used to interact with the native platform.

  final methodChannel = const MethodChannel('smdt');

  @override
  Future<String?> smdtGetCurrentNetType() async {
    final version =
        await methodChannel.invokeMethod<String>('smdtGetCurrentNetType');
    return version;
  }

  @override
  Future<void> smdtSetStatusBar(bool enable) async {
    await methodChannel
        .invokeMethod<void>('smdtSetStatusBar', <String, dynamic>{
      'enable': enable,
    });
  }

  @override
  Future<int?> smdtGetVolume() async {
    final volume = await methodChannel.invokeMethod<int>('smdtGetVolume');
    return volume;
  }

  @override
  Future<void> smdtSetVolume(int volume) async {
    await methodChannel.invokeMethod<void>('smdtSetVolume', <String, dynamic>{
      'volume': volume,
    });
  }

  @override
  Future<void> smdtReboot() async {
    await methodChannel.invokeMethod<void>('smdtReboot');
  }

  @override
  Future<void> smdtShutdown() async {
    await methodChannel.invokeMethod<void>('smdtShutdown');
  }

  @override
  Future<String?> getLogCat() async {
    final logcat = await methodChannel.invokeMethod<String>('getLogCat');
    return logcat;
  }

  @override
  Future<void> smdtInstall(String apkPath) async {
    await methodChannel.invokeMethod<void>('smdtInstall', <String, dynamic>{
      'apkPath': apkPath,
    });
  }

  @override
  Future<void> smdtSetNavigationBar(bool enable) async {
    await methodChannel
        .invokeMethod<void>('smdtSetNavigationBar', <String, dynamic>{
      'enable': enable,
    });
  }

  @override
  Future<bool> sendLogCatToServer(String serverUrl, String accessToken) async {
    try {
      // D'abord récupérer les logs via la méthode existante
      final String? logs = await getLogCat();

      if (logs == null || logs.isEmpty) {
        debugPrint('❌ [SMDT] Aucun log à envoyer');
        return false;
      }

      // Utiliser http pour l'envoyer au format octet-stream
      final response = await http.post(
        Uri.parse('$serverUrl/api/totem/app.logcat'),
        headers: {
          'Content-Type': 'application/octet-stream',
          'Authorization': 'Bearer $accessToken',
        },
        body: utf8.encode(logs), // Convertir en bytes pour octet-stream
      );

      if (response.statusCode == 200) {
        debugPrint('✅ [SMDT] Logs envoyés avec succès');
        return true;
      } else {
        debugPrint('❌ [SMDT] Échec d\'envoi des logs: ${response.statusCode}');
        return false;
      }
    } catch (e) {
      debugPrint('❌ [SMDT] Erreur lors de l\'envoi des logs: $e');
      return false;
    }
  }

  @override
  Future<Map<String, dynamic>> getMemoryStats() async {
    try {
      final result = await methodChannel
          .invokeMethod<Map<Object?, Object?>>('getMemoryStats');
      if (result != null) {
        return result.map((key, value) => MapEntry(key.toString(), value));
      }
      return {
        'totalRam': 'N/A',
        'availableRam': 'N/A',
        'usedRam': 'N/A',
        'appMemory': 'N/A'
      };
    } catch (e) {
      debugPrint(
          "❌ [SMDT] Erreur lors de la récupération des stats mémoire: $e");
      return {
        'totalRam': 'N/A',
        'availableRam': 'N/A',
        'usedRam': 'N/A',
        'appMemory': 'N/A'
      };
    }
  }
}
