import 'package:flutter/material.dart';

class MobileModelInput extends StatefulWidget {
  const MobileModelInput({
    super.key,
    required this.controller,
    required this.suggestions,
    required this.onChanged,
    this.onRemove,
  });

  final TextEditingController controller;
  final List<String> Function(String query) suggestions;
  final ValueChanged<String> onChanged;
  final VoidCallback? onRemove;

  @override
  State<MobileModelInput> createState() => _MobileModelInputState();
}

class _MobileModelInputState extends State<MobileModelInput> {
  late final FocusNode _focusNode = FocusNode();

  @override
  void dispose() {
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: RawAutocomplete<String>(
            textEditingController: widget.controller,
            focusNode: _focusNode,
            optionsBuilder: (value) => widget.suggestions(value.text),
            onSelected: (selection) {
              widget.controller
                ..text = selection
                ..selection =
                    TextSelection.collapsed(offset: selection.length);
              widget.onChanged(selection);
            },
            fieldViewBuilder: (context, controller, focusNode, onSubmit) {
              return TextFormField(
                controller: controller,
                focusNode: focusNode,
                onChanged: widget.onChanged,
                textCapitalization: TextCapitalization.words,
                decoration: InputDecoration(
                  hintText: 'Mobile model',
                  filled: true,
                  fillColor: Colors.white,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 12,
                  ),
                ),
                validator: (v) {
                  if (v == null || v.trim().isEmpty) {
                    return 'Mobile model cannot be empty';
                  }
                  return null;
                },
              );
            },
            optionsViewBuilder: (context, onSelected, options) {
              final list = options.toList();
              if (list.isEmpty) return const SizedBox.shrink();
              return Align(
                alignment: Alignment.topLeft,
                child: Material(
                  elevation: 4,
                  borderRadius: BorderRadius.circular(12),
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(
                      maxHeight: 220,
                      maxWidth: 420,
                    ),
                    child: ListView.builder(
                      padding: EdgeInsets.zero,
                      shrinkWrap: true,
                      itemCount: list.length,
                      itemBuilder: (context, index) {
                        final option = list[index];
                        return ListTile(
                          dense: true,
                          title: Text(option),
                          onTap: () => onSelected(option),
                        );
                      },
                    ),
                  ),
                ),
              );
            },
          ),
        ),
        if (widget.onRemove != null)
          IconButton(
            onPressed: widget.onRemove,
            icon: const Icon(Icons.close_rounded),
            tooltip: 'Remove',
          ),
      ],
    );
  }
}
