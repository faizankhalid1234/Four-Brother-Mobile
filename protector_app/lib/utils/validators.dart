class Validators {
  static String? protectorName(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Protector name is required';
    }
    return null;
  }

  static String? mobileModelList(List<String> models) {
    final cleaned =
        models.map((e) => e.trim()).where((e) => e.isNotEmpty).toList();
    if (cleaned.isEmpty) {
      return 'Add at least one mobile model';
    }
    final seen = <String>{};
    for (final name in cleaned) {
      final key = name.toLowerCase();
      if (seen.contains(key)) {
        return 'Duplicate mobile model: $name';
      }
      seen.add(key);
    }
    return null;
  }
}
