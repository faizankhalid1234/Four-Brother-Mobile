class Protector {
  final String id;
  final String protectorName;
  final List<String> mobileModels;

  const Protector({
    required this.id,
    required this.protectorName,
    required this.mobileModels,
  });

  factory Protector.fromJson(Map<String, dynamic> json) {
    return Protector(
      id: json['id'] as String,
      protectorName: (json['protectorName'] as String?)?.trim() ?? '',
      mobileModels: List<String>.from(json['mobileModels'] ?? const [])
          .map((e) => e.trim())
          .where((e) => e.isNotEmpty)
          .toList(),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'protectorName': protectorName,
        'mobileModels': mobileModels,
      };
}
