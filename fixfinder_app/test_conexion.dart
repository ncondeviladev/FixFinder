import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

void main() async {
  const String host =
      '127.0.0.1'; // Cambiar a 10.0.2.2 si se corre desde emulador
  const int port = 5000;

  print('--- TEST DE CONEXIÓN FIXFINDER ---');

  try {
    print('Conectando a $host:$port...');
    final socket =
        await Socket.connect(host, port, timeout: const Duration(seconds: 5));
    print('¡Conectado!');

    // Escuchar respuestas
    socket.listen((Uint8List data) {
      if (data.length >= 2) {
        final int length = (data[0] << 8) | data[1];
        final String jsonStr = utf8.decode(data.sublist(2, 2 + length));
        print('Respuesta recibida: $jsonStr');
      }
    },
        onError: (e) => print('Error en socket: $e'),
        onDone: () => print('Conexión cerrada.'));

    // Enviar un LOGIN de prueba
    final Map<String, dynamic> loginRequest = {
      'accion': 'LOGIN',
      'datos': {'email': 'test@fixfinder.com', 'password': '123'}
    };

    final String jsonLogin = jsonEncode(loginRequest);
    final List<int> jsonBytes = utf8.encode(jsonLogin);

    // Preparar cabecera readUTF
    final int len = jsonBytes.length;
    final Uint8List header = Uint8List(2);
    header[0] = (len >> 8) & 0xFF;
    header[1] = len & 0xFF;

    final BytesBuilder builder = BytesBuilder();
    builder.add(header);
    builder.add(jsonBytes);

    print('Enviando LOGIN: $jsonLogin');
    socket.add(builder.toBytes());
    await socket.flush();

    // Esperar un poco para recibir respuesta antes de cerrar
    await Future.delayed(const Duration(seconds: 2));
    socket.destroy();
    print('Test finalizado.');
  } catch (e) {
    print('Error fatal en el test: $e');
  }
}
