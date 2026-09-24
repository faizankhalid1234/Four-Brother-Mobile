class MobileModel {
  final String id;
  final String name;

  const MobileModel({
    required this.id,
    required this.name,
  });

  factory MobileModel.fromJson(Map<String, dynamic> json) {
    return MobileModel(
      id: json['id'] as String,
      name: (json['name'] as String?)?.trim() ?? '',
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
      };
}
