import 'package:flutter/material.dart';

/// The Volunteer Portal look, shared with the website (src/main/resources/static/css/app.css):
/// workwear navy, cool light grey, and safety yellow reserved for "now / you" (the selected tab,
/// the next event, being checked in, focus). Use these instead of hard-coded colours.
abstract final class VpColors {
  static const ink = Color(0xFF17324D);
  static const inkDeep = Color(0xFF0F2438);
  static const inkTint = Color(0xFFE1E8EF);
  static const paper = Color(0xFFF5F7F8);
  static const surface = Color(0xFFFFFFFF);
  static const line = Color(0xFFD5DDE3);
  static const muted = Color(0xFF50606E);
  static const vest = Color(0xFFFFC72C);
  static const vestTint = Color(0xFFFFF1C7);
  static const leaf = Color(0xFF2F7D4F);
  static const leafInk = Color(0xFF22603B);
  static const leafTint = Color(0xFFE2F1E7);
  static const brick = Color(0xFFB3412F);
  static const brickInk = Color(0xFF8E3123);
  static const brickTint = Color(0xFFF8E3DF);
  static const slateTint = Color(0xFFE8EDF1);
}

/// Readex Pro (SIL Open Font License, assets/fonts/OFL.txt): one family for Latin and Arabic.
const vpFontFamily = 'ReadexPro';

ThemeData buildVpTheme() {
  const scheme = ColorScheme(
    brightness: Brightness.light,
    primary: VpColors.ink,
    onPrimary: Colors.white,
    primaryContainer: VpColors.inkTint,
    onPrimaryContainer: VpColors.ink,
    secondary: VpColors.vest,
    onSecondary: VpColors.ink,
    secondaryContainer: VpColors.vestTint,
    onSecondaryContainer: VpColors.ink,
    tertiary: VpColors.leaf,
    onTertiary: Colors.white,
    error: VpColors.brick,
    onError: Colors.white,
    errorContainer: VpColors.brickTint,
    onErrorContainer: VpColors.brickInk,
    surface: VpColors.surface,
    onSurface: VpColors.ink,
    onSurfaceVariant: VpColors.muted,
    surfaceContainerLowest: VpColors.surface,
    surfaceContainerLow: VpColors.paper,
    surfaceContainer: VpColors.paper,
    surfaceContainerHigh: VpColors.slateTint,
    surfaceContainerHighest: VpColors.slateTint,
    outline: VpColors.muted,
    outlineVariant: VpColors.line,
  );

  final base = ThemeData(useMaterial3: true, colorScheme: scheme, fontFamily: vpFontFamily);
  final text = base.textTheme.apply(bodyColor: VpColors.ink, displayColor: VpColors.ink).copyWith(
        headlineMedium: base.textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w600, height: 1.15),
        headlineSmall: base.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w600, height: 1.2),
        titleLarge: base.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w600),
        titleMedium: base.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w500),
        labelLarge: base.textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w500),
      );
  final rounded = RoundedRectangleBorder(borderRadius: BorderRadius.circular(8));

  return base.copyWith(
    scaffoldBackgroundColor: VpColors.paper,
    textTheme: text,
    // Navy bars top and bottom, like the website's sidebar
    appBarTheme: AppBarTheme(
      backgroundColor: VpColors.ink,
      foregroundColor: Colors.white,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      titleTextStyle: text.titleLarge?.copyWith(color: Colors.white),
    ),
    // The selected tab wears the yellow lanyard
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: VpColors.ink,
      surfaceTintColor: Colors.transparent,
      indicatorColor: VpColors.vest,
      iconTheme: WidgetStateProperty.resolveWith((states) => IconThemeData(
            color: states.contains(WidgetState.selected) ? VpColors.ink : Colors.white.withValues(alpha: 0.8),
          )),
      // Long labels ("Notifications", تسجيل الحضور) must stay on one line on 360-wide phones: no letter
      // spacing (Material's 0.5 adds ~6px, and spacing Arabic letters apart is wrong anyway) and the same
      // weight selected or not (bolder is wider). The yellow indicator and full white mark the selected tab.
      labelTextStyle: WidgetStateProperty.resolveWith((states) => text.labelMedium?.copyWith(
            color: Colors.white.withValues(alpha: states.contains(WidgetState.selected) ? 1 : 0.8),
            fontWeight: FontWeight.w500,
            letterSpacing: 0,
          )),
    ),
    cardTheme: CardThemeData(
      color: VpColors.surface,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(10),
        side: const BorderSide(color: VpColors.line),
      ),
    ),
    dividerTheme: const DividerThemeData(color: VpColors.line, space: 1, thickness: 1),
    listTileTheme: const ListTileThemeData(iconColor: VpColors.muted),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: VpColors.surface,
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(6), borderSide: const BorderSide(color: VpColors.line)),
      enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(6), borderSide: const BorderSide(color: VpColors.line)),
      focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(6), borderSide: const BorderSide(color: VpColors.ink, width: 2)),
      errorBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(6), borderSide: const BorderSide(color: VpColors.brick)),
      focusedErrorBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(6), borderSide: const BorderSide(color: VpColors.brick, width: 2)),
      labelStyle: const TextStyle(color: VpColors.muted),
      floatingLabelStyle: const TextStyle(color: VpColors.ink, fontWeight: FontWeight.w500),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: VpColors.ink,
        foregroundColor: Colors.white,
        shape: rounded,
        textStyle: text.labelLarge,
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: VpColors.ink,
        backgroundColor: VpColors.surface,
        side: const BorderSide(color: VpColors.line),
        shape: rounded,
        textStyle: text.labelLarge,
      ),
    ),
    textButtonTheme: TextButtonThemeData(style: TextButton.styleFrom(foregroundColor: VpColors.ink)),
    chipTheme: ChipThemeData(
      side: BorderSide.none,
      shape: const StadiumBorder(),
      labelStyle: text.labelMedium?.copyWith(fontWeight: FontWeight.w500),
    ),
    progressIndicatorTheme: const ProgressIndicatorThemeData(color: VpColors.ink),
    snackBarTheme: const SnackBarThemeData(backgroundColor: VpColors.ink, contentTextStyle: TextStyle(color: Colors.white)),
  );
}

/// A status pill: `fill` background with `ink` text (e.g. yellow for checked in).
class StatusPill extends StatelessWidget {
  const StatusPill({super.key, required this.label, required this.fill, required this.ink, this.bold = false});

  final String label;
  final Color fill;
  final Color ink;
  final bool bold;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: ShapeDecoration(color: fill, shape: const StadiumBorder()),
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelMedium?.copyWith(
              color: ink,
              fontWeight: bold ? FontWeight.w600 : FontWeight.w500,
            ),
      ),
    );
  }
}

/// The yellow lanyard band with a punched slot, across the top of the next-event pass and the
/// login header.
class LanyardStrip extends StatelessWidget {
  const LanyardStrip({super.key, this.slotColor = VpColors.paper});

  final Color slotColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 14,
      color: VpColors.vest,
      alignment: Alignment.center,
      child: Container(
        width: 56,
        height: 6,
        decoration: BoxDecoration(color: slotColor, borderRadius: BorderRadius.circular(3)),
      ),
    );
  }
}
