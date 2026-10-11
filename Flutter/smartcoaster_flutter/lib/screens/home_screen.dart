import 'package:flutter/material.dart';

/// V0.1 導覽骨架；尚未宣稱完成原生版畫面或 BLE/MQTT 功能。
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int selectedIndex = 0;
  static const titles = ['喝水紀錄', '找水喝', '裝置設定'];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('多喝水沒事')),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.water_drop_outlined, size: 72),
            const SizedBox(height: 16),
            Text(titles[selectedIndex],
                style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 8),
            const Text('Flutter V0.1：功能移植中'),
          ],
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: selectedIndex,
        onDestinationSelected: (index) => setState(() => selectedIndex = index),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.water_drop), label: '喝水'),
          NavigationDestination(icon: Icon(Icons.map_outlined), label: '找水喝'),
          NavigationDestination(icon: Icon(Icons.bluetooth), label: '裝置'),
        ],
      ),
    );
  }
}
