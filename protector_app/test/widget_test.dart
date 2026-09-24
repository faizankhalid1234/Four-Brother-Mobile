import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:protector_app/main.dart';
import 'package:protector_app/providers/protector_provider.dart';
import 'package:protector_app/repositories/protector_repository.dart';
import 'package:protector_app/services/local_storage_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Empty state is shown on launch', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final storage = LocalStorageService();
    await storage.init();
    final repository = ProtectorRepository(storage);
    final provider = ProtectorProvider(repository);
    await provider.load();

    await tester.pumpWidget(ProtectorApp(provider: provider));
    await tester.pumpAndSettle();

    expect(find.text('No Protectors Found'), findsOneWidget);
  });
}
