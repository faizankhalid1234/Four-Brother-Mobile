import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/protector.dart';
import '../providers/protector_provider.dart';
import '../widgets/protector_card.dart';
import '../widgets/search_bar.dart';
import 'protector_form_screen.dart';

class ProtectorListScreen extends StatefulWidget {
  const ProtectorListScreen({super.key});

  @override
  State<ProtectorListScreen> createState() => _ProtectorListScreenState();
}

class _ProtectorListScreenState extends State<ProtectorListScreen> {
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _openForm({Protector? protector}) async {
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => ProtectorFormScreen(protector: protector),
      ),
    );
  }

  Future<void> _confirmDelete(String id, String name) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete Protector?'),
        content: Text(
          'Delete "$name"? Mobile models will stay in master data.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: FilledButton.styleFrom(backgroundColor: Colors.red.shade700),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
    if (ok == true && mounted) {
      try {
        await context.read<ProtectorProvider>().deleteProtector(id);
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Protector deleted')),
        );
      } catch (_) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Could not delete protector')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<ProtectorProvider>();
    final items = provider.filteredProtectors;

    return Scaffold(
      backgroundColor: const Color(0xFFF4F7FB),
      appBar: AppBar(
        title: const Text('Protector Management'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _openForm(),
        icon: const Icon(Icons.add_rounded),
        label: const Text('Add Protector'),
      ),
      body: provider.isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
                  child: AppSearchBar(
                    controller: _searchController,
                    onChanged: provider.setSearchQuery,
                  ),
                ),
                Expanded(
                  child: items.isEmpty
                      ? _EmptyState(
                          hasQuery: provider.searchQuery.trim().isNotEmpty,
                          onAdd: () => _openForm(),
                        )
                      : ListView.separated(
                          padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
                          itemCount: items.length,
                          separatorBuilder: (_, __) =>
                              const SizedBox(height: 10),
                          itemBuilder: (context, index) {
                            final protector = items[index];
                            return ProtectorCard(
                              protector: protector,
                              onEdit: () => _openForm(protector: protector),
                              onDelete: () => _confirmDelete(
                                protector.id,
                                protector.protectorName,
                              ),
                            );
                          },
                        ),
                ),
              ],
            ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.hasQuery, required this.onAdd});

  final bool hasQuery;
  final VoidCallback onAdd;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              hasQuery ? Icons.search_off_rounded : Icons.shield_outlined,
              size: 56,
              color: Colors.grey.shade400,
            ),
            const SizedBox(height: 16),
            Text(
              hasQuery ? 'No matches found' : 'No Protectors Found',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
            ),
            const SizedBox(height: 8),
            Text(
              hasQuery
                  ? 'Try another protector or mobile model name.'
                  : 'Tap + Add Protector to create your first Protector.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey.shade600),
            ),
            if (!hasQuery) ...[
              const SizedBox(height: 20),
              FilledButton.icon(
                onPressed: onAdd,
                icon: const Icon(Icons.add_rounded),
                label: const Text('Add Protector'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
