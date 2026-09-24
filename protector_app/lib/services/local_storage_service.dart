import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../models/mobile_model.dart';
import '../models/protector.dart';

class LocalStorageService {
  static const protectorsKey = 'protectors';
  static const mobileModelsKey = 'mobile_models';

  SharedPreferences? _prefs;

  Future<SharedPreferences> _ensurePrefs() async {
    return _prefs ??= await SharedPreferences.getInstance();
  }

  Future<void> init() async {
    await _ensurePrefs();
  }

  Future<List<Protector>> loadProtectors() async {
    try {
      final prefs = await _ensurePrefs();
      final raw = prefs.getString(protectorsKey);
      if (raw == null || raw.isEmpty) return [];
      final list = jsonDecode(raw) as List<dynamic>;
      return list
          .map((e) => Protector.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } catch (_) {
      return [];
    }
  }

  Future<void> saveProtectors(List<Protector> protectors) async {
    final prefs = await _ensurePrefs();
    await prefs.setString(
      protectorsKey,
      jsonEncode(protectors.map((e) => e.toJson()).toList()),
    );
  }

  Future<List<MobileModel>> loadMobileModels() async {
    try {
      final prefs = await _ensurePrefs();
      final raw = prefs.getString(mobileModelsKey);
      if (raw == null || raw.isEmpty) return [];
      final list = jsonDecode(raw) as List<dynamic>;
      return list
          .map((e) =>
              MobileModel.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } catch (_) {
      return [];
    }
  }

  Future<void> saveMobileModels(List<MobileModel> models) async {
    final prefs = await _ensurePrefs();
    await prefs.setString(
      mobileModelsKey,
      jsonEncode(models.map((e) => e.toJson()).toList()),
    );
  }
}
