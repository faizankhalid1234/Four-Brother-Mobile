import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/protector.dart';
import '../providers/protector_provider.dart';
import '../utils/validators.dart';
import '../widgets/mobile_model_input.dart';

class ProtectorFormScreen extends StatefulWidget {
  const ProtectorFormScreen({super.key, this.protector});

  final Protector? protector;
  bool get isEdit => protector != null;

  @override
  State<ProtectorFormScreen> createState() => _ProtectorFormScreenState();
}

class _ProtectorFormScreenState extends State<ProtectorFormScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameController;
  late List<TextEditingController> _modelControllers;
  var _saving = false;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(
      text: widget.protector?.protectorName ?? '',
    );
    final models = widget.protector?.mobileModels ?? [''];
    _modelControllers =
        models.map((m) => TextEditingController(text: m)).toList();
    if (_modelControllers.isEmpty) {
      _modelControllers = [TextEditingController()];
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    for (final c in _modelControllers) {
      c.dispose();
    }
    super.dispose();
  }

  void _addModelField() {
    setState(() => _modelControllers.add(TextEditingController()));
  }

  void _removeModelField(int index) {
    if (_modelControllers.length <= 1) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('At least one mobile model is required')),
      );
      return;
    }
    setState(() {
      _modelControllers[index].dispose();
      _modelControllers.removeAt(index);
    });
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    final models = _modelControllers.map((c) => c.text).toList();
    final listError = Validators.mobileModelList(models);
    if (listError != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(listError)),
      );
      return;
    }

    setState(() => _saving = true);
    final provider = context.read<ProtectorProvider>();
    try {
      if (widget.isEdit) {
        await provider.updateProtector(
          id: widget.protector!.id,
          protectorName: _nameController.text,
          mobileModels: models,
        );
      } else {
        await provider.createProtector(
          protectorName: _nameController.text,
          mobileModels: models,
        );
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(widget.isEdit ? 'Protector updated' : 'Protector saved'),
        ),
      );
      Navigator.of(context).pop(true);
    } catch (e) {
      if (!mounted) return;
      final message = e.toString().replaceFirst(RegExp(r'^[^:]+:\s*'), '');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(message)),
      );
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<ProtectorProvider>();

    return Scaffold(
      backgroundColor: const Color(0xFFF4F7FB),
      appBar: AppBar(
        title: Text(widget.isEdit ? 'Edit Protector' : 'Add Protector'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
          children: [
            Text(
              'Protector Name',
              style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _nameController,
              textCapitalization: TextCapitalization.words,
              decoration: InputDecoration(
                hintText: 'e.g. ABC Protector',
                filled: true,
                fillColor: Colors.white,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              validator: Validators.protectorName,
            ),
            const SizedBox(height: 24),
            Text(
              'Mobile Models',
              style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
            ),
            const SizedBox(height: 4),
            Text(
              'Type to search saved models, or enter a new one.',
              style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
            ),
            const SizedBox(height: 12),
            ...List.generate(_modelControllers.length, (index) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: MobileModelInput(
                  controller: _modelControllers[index],
                  suggestions: provider.suggestMobileModels,
                  onChanged: (_) {},
                  onRemove: () => _removeModelField(index),
                ),
              );
            }),
            OutlinedButton.icon(
              onPressed: _addModelField,
              icon: const Icon(Icons.add_rounded),
              label: const Text('Add Mobile Model'),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
            const SizedBox(height: 28),
            FilledButton(
              onPressed: _saving ? null : _save,
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(50),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              child: Text(_saving ? 'Saving…' : 'Save'),
            ),
          ],
        ),
      ),
    );
  }
}
