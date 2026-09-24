import 'package:uuid/uuid.dart';

import '../models/mobile_model.dart';
import '../models/protector.dart';
import '../services/local_storage_service.dart';

class ProtectorRepository {
  ProtectorRepository(this._storage);

  final LocalStorageService _storage;
  final _uuid = const Uuid();

  List<Protector> _protectors = [];
  List<MobileModel> _mobileModels = [];

  List<Protector> get protectors => List.unmodifiable(_protectors);
  List<MobileModel> get mobileModels => List.unmodifiable(_mobileModels);

  Future<void> loadAll() async {
    await _storage.init();
    _protectors = await _storage.loadProtectors();
    _mobileModels = await _storage.loadMobileModels();
  }

  Future<void> _persistProtectors() => _storage.saveProtectors(_protectors);

  Future<void> _persistMobileModels() =>
      _storage.saveMobileModels(_mobileModels);

  Future<String> ensureMobileModel(String rawName) async {
    final name = rawName.trim();
    if (name.isEmpty) {
      throw ArgumentError('Mobile model name cannot be empty');
    }

    for (final m in _mobileModels) {
      if (m.name.toLowerCase() == name.toLowerCase()) {
        return m.name;
      }
    }

    _mobileModels = [
      ..._mobileModels,
      MobileModel(id: _uuid.v4(), name: name),
    ]..sort((a, b) => a.name.toLowerCase().compareTo(b.name.toLowerCase()));
    await _persistMobileModels();
    return name;
  }

  List<String> suggestMobileModels(String query) {
    final q = query.trim().toLowerCase();
    if (q.isEmpty) {
      return _mobileModels.map((m) => m.name).take(20).toList();
    }
    return _mobileModels
        .where((m) => m.name.toLowerCase().contains(q))
        .map((m) => m.name)
        .toList();
  }

  List<Protector> search(String query) {
    final q = query.trim().toLowerCase();
    if (q.isEmpty) return protectors;
    return _protectors.where((p) {
      if (p.protectorName.toLowerCase().contains(q)) return true;
      return p.mobileModels.any((m) => m.toLowerCase().contains(q));
    }).toList();
  }

  Future<List<String>> _resolveModels(List<String> mobileModels) async {
    final cleaned = <String>[];
    final seen = <String>{};
    for (final raw in mobileModels) {
      final name = raw.trim();
      if (name.isEmpty) continue;
      final key = name.toLowerCase();
      if (seen.contains(key)) {
        throw ArgumentError('Duplicate mobile model: $name');
      }
      seen.add(key);
      cleaned.add(name);
    }
    if (cleaned.isEmpty) {
      throw ArgumentError('At least one mobile model is required');
    }

    final resolved = <String>[];
    for (final model in cleaned) {
      resolved.add(await ensureMobileModel(model));
    }
    return resolved;
  }

  Future<Protector> createProtector({
    required String protectorName,
    required List<String> mobileModels,
  }) async {
    final name = protectorName.trim();
    if (name.isEmpty) {
      throw ArgumentError('Protector name is required');
    }
    final models = await _resolveModels(mobileModels);
    final protector = Protector(
      id: _uuid.v4(),
      protectorName: name,
      mobileModels: models,
    );
    _protectors = [..._protectors, protector];
    await _persistProtectors();
    return protector;
  }

  Future<Protector> updateProtector({
    required String id,
    required String protectorName,
    required List<String> mobileModels,
  }) async {
    final index = _protectors.indexWhere((p) => p.id == id);
    if (index < 0) throw StateError('Protector not found');

    final name = protectorName.trim();
    if (name.isEmpty) {
      throw ArgumentError('Protector name is required');
    }
    final models = await _resolveModels(mobileModels);
    final updated = Protector(
      id: id,
      protectorName: name,
      mobileModels: models,
    );
    _protectors = [..._protectors]..[index] = updated;
    await _persistProtectors();
    return updated;
  }

  Future<void> deleteProtector(String id) async {
    _protectors = _protectors.where((p) => p.id != id).toList();
    await _persistProtectors();
  }
}
