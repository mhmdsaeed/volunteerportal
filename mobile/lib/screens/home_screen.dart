import 'package:flutter/material.dart';

import '../l10n/app_localizations.dart';
import 'events_screen.dart';
import 'history_screen.dart';
import 'notifications_screen.dart';
import 'profile_screen.dart';
import 'scan_screen.dart';

/// The logged-in app: Events, Check in (scan), History and Notifications tabs.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key, this.scannerBuilder = cameraScanner});

  final ScannerBuilder scannerBuilder;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _tab = 0;

  // Bumped after a check-in so the Events and History tabs reload with the new status
  int _refresh = 0;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final tabs = [
      EventsScreen(key: ValueKey('events-$_refresh')),
      ScanScreen(scannerBuilder: widget.scannerBuilder, onCheckedIn: () => setState(() => _refresh++)),
      HistoryScreen(key: ValueKey('history-$_refresh')),
      const NotificationsScreen(),
    ];
    final titles = [t.tabEvents, t.tabScan, t.tabHistory, t.tabNotifications];

    return Scaffold(
      appBar: AppBar(
        title: Text(titles[_tab]),
        actions: [
          IconButton(
            key: const Key('profileButton'),
            tooltip: t.profile,
            icon: const Icon(Icons.account_circle),
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const ProfileScreen())),
          ),
        ],
      ),
      // Only the visible tab is built, so the camera runs only on the Check in tab
      body: tabs[_tab],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (index) => setState(() => _tab = index),
        destinations: [
          NavigationDestination(icon: const Icon(Icons.event), label: t.tabEvents),
          NavigationDestination(key: const Key('scanTab'), icon: const Icon(Icons.qr_code_scanner), label: t.tabScan),
          NavigationDestination(icon: const Icon(Icons.history), label: t.tabHistory),
          NavigationDestination(icon: const Icon(Icons.notifications), label: t.tabNotifications),
        ],
      ),
    );
  }
}
