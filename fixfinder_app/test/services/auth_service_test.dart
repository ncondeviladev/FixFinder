import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:fixfinder_app/services/auth_service.dart';
import 'dart:convert';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('AuthService Tests', () {
    late AuthService authService;

    setUp(() {
      SharedPreferences.setMockInitialValues({});
      authService = AuthService();
    });

    test('tryAutoLogin devuelve false si no hay datos persistidos', () async {
      final resultado = await authService.tryAutoLogin();
      expect(resultado, isFalse);
      expect(authService.usuarioActual, isNull);
    });

    test(
        'tryAutoLogin recupera el usuario si los datos en SharedPreferences son válidos',
        () async {
      final mockUser = {
        'id': 1,
        'email': 'test@test.com',
        'nombre_completo': 'Test User',
        'rol': 'CLIENTE',
        'token': 'mock_token',
        'dni': '12345678A',
        'urgencia': 1,
        'urlsFotos': [],
      };

      SharedPreferences.setMockInitialValues({
        'userData': jsonEncode(mockUser),
      });

      final resultado = await authService.tryAutoLogin();

      expect(resultado, isTrue);
      expect(authService.usuarioActual, isNotNull);
      expect(authService.usuarioActual!.email, 'test@test.com');
      expect(authService.usuarioActual!.token, 'mock_token');
    });

    test('logout limpia los datos de SharedPreferences', () async {
      SharedPreferences.setMockInitialValues({
        'userData': 'some_data',
      });

      await authService.logout();

      final prefs = await SharedPreferences.getInstance();
      expect(prefs.getString('userData'), isNull);
      expect(authService.usuarioActual, isNull);
    });
  });
}
