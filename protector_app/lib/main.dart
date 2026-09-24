import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'providers/protector_provider.dart';
import 'repositories/protector_repository.dart';
import 'screens/protector_list_screen.dart';
import 'services/local_storage_service.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final storage = LocalStorageService();
  await storage.init();
  final repository = ProtectorRepository(storage);
  final provider = ProtectorProvider(repository);
  await provider.load();

  runApp(ProtectorApp(provider: provider));
}

class ProtectorApp extends StatelessWidget {
  const ProtectorApp({super.key, required this.provider});

  final ProtectorProvider provider;

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider.value(
      value: provider,
      child: MaterialApp(
        title: 'Protector Management',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          useMaterial3: true,
          colorScheme: ColorScheme.fromSeed(
            seedColor: const Color(0xFF0F766E),
            brightness: Brightness.light,
          ),
          scaffoldBackgroundColor: const Color(0xFFF4F7FB),
          appBarTheme: const AppBarTheme(
            centerTitle: false,
            scrolledUnderElevation: 0,
          ),
        ),
        home: const ProtectorListScreen(),
      ),
    );
  }
}
