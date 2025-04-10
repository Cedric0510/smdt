import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:smdt/smdt_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelSmdt platform = MethodChannelSmdt();
  const MethodChannel channel = MethodChannel('smdt');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('smdtGetCurrentNetType', () async {
    expect(await platform.smdtGetCurrentNetType(), 'wifi');
  });
}
