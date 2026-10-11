import 'package:flutter/material.dart';
import 'screens/home_screen.dart';

/// Flutter 入口：先建立可運作的 UI 骨架，後續逐頁對照 Kotlin 移植。
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const SmartCoasterApp());
}

class SmartCoasterApp extends StatelessWidget {
  const SmartCoasterApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '多喝水沒事',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(colorSchemeSeed: Colors.cyan, useMaterial3: true),
      home: const HomeScreen(),
    );
  }
}
