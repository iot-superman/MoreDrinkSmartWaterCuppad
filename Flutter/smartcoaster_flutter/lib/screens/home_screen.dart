import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';
import 'package:mqtt_client/mqtt_client.dart';
import 'package:mqtt_client/mqtt_server_client.dart';

/// Flutter V0.2 第一階段：真實 MQTT 連線、BLE 掃描、手動飲水紀錄。
/// ESP32 的命令 Topic 與 Payload 尚須與原版韌體核對，避免送錯指令。
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});
  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int tab = 0;
  final uid = TextEditingController(text: '311');
  final broker = TextEditingController(text: 'broker.mqttgo.io');
  final topic = TextEditingController(text: 'esp32/recode');
  final amount = TextEditingController(text: '250');
  final records = <_DrinkRecord>[];
  final found = <ScanResult>[];
  MqttServerClient? client;
  StreamSubscription<List<ScanResult>>? scanSub;
  String mqttStatus = '尚未連線';
  String bleStatus = '尚未掃描';
  String latestMessage = '尚未收到資料';
  bool scanning = false;
  int goal = 2000;
  int step = 0;

  int get total {
    final now = DateTime.now();
    return records.where((r) => r.time.year == now.year &&
        r.time.month == now.month && r.time.day == now.day)
        .fold(0, (sum, r) => sum + r.ml);
  }

  @override
  void dispose() {
    scanSub?.cancel();
    client?.disconnect();
    uid.dispose();
    broker.dispose();
    topic.dispose();
    amount.dispose();
    super.dispose();
  }

  void notice(String message) {
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)));
  }

  Future<void> connectMqtt() async {
    final host = broker.text.trim();
    final channel = topic.text.trim();
    if (host.isEmpty || channel.isEmpty) {
      notice('請輸入 Broker 與 Topic');
      return;
    }
    client?.disconnect();
    final c = MqttServerClient.withPort(
      host, 'smartcoaster_flutter_${DateTime.now().millisecondsSinceEpoch}', 1883);
    c.logging(on: false);
    c.keepAlivePeriod = 20;
    c.autoReconnect = true;
    c.connectionMessage = MqttConnectMessage().withClientIdentifier(
      'smartcoaster_flutter_${DateTime.now().millisecondsSinceEpoch}')
      .startClean().withWillQos(MqttQos.atMostOnce);
    setState(() => mqttStatus = '連線中…');
    try {
      await c.connect().timeout(const Duration(seconds: 12));
      if (c.connectionStatus?.state != MqttConnectionState.connected) {
        throw StateError('Broker 拒絕連線：${c.connectionStatus?.state}');
      }
      client = c;
      c.subscribe(channel, MqttQos.atLeastOnce);
      c.updates?.listen((messages) {
        if (!mounted || messages.isEmpty) return;
        final m = messages.first.payload as MqttPublishMessage;
        final raw = MqttPublishPayload.bytesToStringAsString(m.payload.message);
        setState(() => latestMessage = raw);
        // 只顯示原始回傳；在確認韌體格式前不將未知訊息當成喝水紀錄。
      });
      if (mounted) setState(() => mqttStatus = '已連線 · $host');
    } catch (e) {
      c.disconnect();
      if (mounted) setState(() => mqttStatus = '連線失敗');
      notice('MQTT：$e');
    }
  }

  Future<void> scanBle() async {
    if (scanning) return;
    try {
      if (!await FlutterBluePlus.isSupported) {
        notice('目前平台不支援 BLE 掃描');
        return;
      }
      setState(() { scanning = true; found.clear(); bleStatus = '掃描中…'; });
      await scanSub?.cancel();
      scanSub = FlutterBluePlus.scanResults.listen((results) {
        if (mounted) setState(() {
          found..clear()..addAll(results);
        });
      });
      await FlutterBluePlus.startScan(timeout: const Duration(seconds: 8));
      await Future.delayed(const Duration(seconds: 9));
      if (mounted) setState(() => bleStatus = '掃描完成 · ${found.length} 個裝置');
    } catch (e) {
      if (mounted) setState(() => bleStatus = '掃描失敗');
      notice('BLE：$e。請確認系統藍牙權限。');
    } finally {
      if (mounted) setState(() => scanning = false);
    }
  }

  void addDrink() {
    final ml = int.tryParse(amount.text.trim());
    if (ml == null || ml <= 0 || ml > 5000) {
      notice('請輸入 1～5000 ml 的有效飲水量');
      return;
    }
    setState(() { records.insert(0, _DrinkRecord(DateTime.now(), ml)); step = 0; });
    notice('已新增 $ml ml 手動紀錄（目前僅保留在本次執行期間）');
  }

  Widget panel(String title, Widget child) => Card(
    margin: const EdgeInsets.symmetric(vertical: 8),
    child: Padding(padding: const EdgeInsets.all(16),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(title, style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 12), child,
      ])));

  Widget waterPage() => ListView(padding: const EdgeInsets.all(16), children: [
    panel('今日喝水', Column(children: [
      SizedBox(height: 140, width: 140, child: Stack(alignment: Alignment.center, children: [
        SizedBox.expand(child: CircularProgressIndicator(
          value: (total / goal).clamp(0.0, 1.0), strokeWidth: 12,
          backgroundColor: Colors.blueGrey.shade100)),
        Column(mainAxisAlignment: MainAxisAlignment.center, children: [
          Text('$total', style: Theme.of(context).textTheme.headlineMedium),
          Text('/ $goal ml'),
        ]),
      ])),
      const SizedBox(height: 12),
      Text('今日 ${records.length} 筆紀錄'),
    ])),
    panel('喝水三步驟', Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      Text('目前步驟：${['準備杯子', '拿起杯子喝水', '放回杯子'][step]}'),
      const SizedBox(height: 8),
      FilledButton(onPressed: () {
        if (step < 2) {
          setState(() => step++);
        } else {
          notice('尚未核對 ESP32 MQTT 指令格式，不能假裝已完成自動量測。可先使用手動紀錄。');
        }
      }, child: Text(step == 2 ? '等待裝置量測結果' : '下一步')),
      const Text('目前為操作流程預覽；尚未發送 ESP32 控制指令。'),
    ])),
    panel('手動新增飲水紀錄', Column(children: [
      TextField(controller: amount, keyboardType: TextInputType.number,
        decoration: const InputDecoration(labelText: '飲水量（ml）', border: OutlineInputBorder())),
      const SizedBox(height: 10),
      FilledButton.icon(onPressed: addDrink, icon: const Icon(Icons.add),
        label: const Text('新增紀錄')),
    ])),
    panel('MQTT 狀態', Text(mqttStatus)),
  ]);

  Widget historyPage() => ListView(padding: const EdgeInsets.all(16), children: [
    panel('飲水紀錄', records.isEmpty
      ? const Text('尚無紀錄。請先在「喝水」頁面新增。')
      : Column(children: records.map((r) => ListTile(
          leading: const Icon(Icons.water_drop, color: Colors.blue),
          title: Text('${r.ml} ml'),
          subtitle: Text('${r.time.year}/${r.time.month}/${r.time.day}  '
              '${r.time.hour.toString().padLeft(2, '0')}:${r.time.minute.toString().padLeft(2, '0')}'),
        )).toList())),
    const Text('注意：目前紀錄尚未寫入本機資料庫或 Django。'),
  ]);

  Widget mapPage() => ListView(padding: const EdgeInsets.all(16), children: [
    panel('找水喝', const Column(children: [
      Icon(Icons.map_outlined, size: 64, color: Colors.teal),
      SizedBox(height: 12),
      Text('原版 LBS 地圖、雷達動畫及水滴標記尚待移植。'),
      Text('不以靜態假地圖冒充可用的定位功能。'),
    ])),
  ]);

  Widget devicePage() => ListView(padding: const EdgeInsets.all(16), children: [
    panel('MQTT 設定', Column(children: [
      TextField(controller: broker, decoration: const InputDecoration(labelText: 'Broker 主機')),
      TextField(controller: topic, decoration: const InputDecoration(labelText: '訂閱 Topic')),
      TextField(controller: uid, decoration: const InputDecoration(labelText: 'UID')),
      const SizedBox(height: 12),
      Text(mqttStatus),
      const SizedBox(height: 8),
      FilledButton.icon(onPressed: connectMqtt, icon: const Icon(Icons.cloud),
        label: const Text('連線並訂閱')),
      const SizedBox(height: 12),
      SelectableText('最後收到訊息：$latestMessage'),
    ])),
    panel('藍牙 BLE 掃描', Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      Text(bleStatus),
      FilledButton.icon(onPressed: scanning ? null : scanBle,
        icon: const Icon(Icons.bluetooth_searching), label: const Text('搜尋附近裝置')),
      ...found.map((r) => ListTile(
        leading: const Icon(Icons.bluetooth),
        title: Text(r.device.platformName.isEmpty ? '未命名裝置' : r.device.platformName),
        subtitle: Text(r.device.remoteId.str),
        trailing: Text('${r.rssi} dBm'),
      )),
      const Text('BLE 綁定、GATT 寫入與 SYNC 尚待比對原版 UUID 與協定。'),
    ])),
  ]);

  @override
  Widget build(BuildContext context) {
    final pages = [waterPage(), mapPage(), historyPage(), devicePage()];
    return Scaffold(
      appBar: AppBar(title: const Text('多喝水沒事 · Flutter V0.2')),
      body: pages[tab],
      bottomNavigationBar: NavigationBar(
        selectedIndex: tab,
        onDestinationSelected: (i) => setState(() => tab = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.water_drop), label: '喝水'),
          NavigationDestination(icon: Icon(Icons.map_outlined), label: '找水喝'),
          NavigationDestination(icon: Icon(Icons.history), label: '紀錄'),
          NavigationDestination(icon: Icon(Icons.bluetooth), label: '裝置'),
        ],
      ),
    );
  }
}

class _DrinkRecord {
  final DateTime time;
  final int ml;
  const _DrinkRecord(this.time, this.ml);
}
