import 'package:flutter/foundation.dart';

import '../models/mobile_model.dart';
import '../models/protector.dart';
import '../repositories/protector_repository.dart';

class ProtectorProvider extends ChangeNotifier {
  ProtectorProvider(this._repository);

  final ProtectorRepository _repository;

  bool _loading = true;
  bool get isLoading => _loading;

  String _searchQuery = '';
  String get searchQuery => _searchQuery;

  List<Protector> get protectors => _repository.protectors;
  List<Protector> get filteredProtectors => _repository.search(_searchQuery);
  List<MobileModel> get mobileModels => _repository.mobileModels;

  Future<void> load() async {
    _loading = true;
    notifyListeners();
    try {
      await _repository.loadAll();
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  void setSearchQuery(String query) {
    _searchQuery = query;
    notifyListeners();
  }

  List<String> suggestMobileModels(String query) =>
      _repository.suggestMobileModels(query);

  Future<void> createProtector({
    required String protectorName,
    required List<String> mobileModels,
  }) async {
    await _repository.createProtector(
      protectorName: protectorName,
      mobileModels: mobileModels,
    );
    notifyListeners();
  }

  Future<void> updateProtector({
    required String id,
    required String protectorName,
    required List<String> mobileModels,
  }) async {
    await _repository.updateProtector(
      id: id,
      protectorName: protectorName,
      mobileModels: mobileModels,
    );
    notifyListeners();
  }

  Future<void> deleteProtector(String id) async {
    await _repository.deleteProtector(id);
    notifyListeners();
  }
}
