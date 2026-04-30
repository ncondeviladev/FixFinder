# Plan General de la Presentación: FixFinder

## 🎯 Enfoque Estratégico de la Presentación
El objetivo principal frente al tribunal **no es solo mostrar una aplicación bonita que funciona**, sino **demostrar un profundo conocimiento técnico e ingeniería**. El mensaje central debe ser que has rechazado el "camino fácil" de usar frameworks mágicos (como Spring Boot, APIs REST estándar o herramientas ORM) para pelear directamente con la concurrencia, los sockets a muy bajo nivel y el diseño de protocolos. Es una defensa de la **ingeniería de software tradicional y pura**.

---

## 🗺️ Estructura del Guion y Diapositivas

Para que el tribunal lo entienda perfectamente, vamos a dividir la presentación en dos grandes bloques. Primero demostramos **qué hace y cómo se ve** (Funcional), y una vez que los tengamos impresionados con el resultado visual, les enseñamos **el monstruo técnico que hay debajo** (Técnico).

> 💡 **Nota sobre el diseño de las diapositivas:** No necesitas hacer varias diapositivas casi iguales. En programas como PowerPoint, Google Slides o Canva, usaremos **"Animaciones de entrada"**. Es decir, estás en una sola diapositiva y, cada vez que haces clic, aparece un elemento nuevo (una flecha, un dibujo, un texto).

---

### BLOQUE 1: Funcionalidad y Modelo de Negocio (El "Qué")

#### Diapositiva 1: Portada
*   **Concepto:** FixFinder - Sistema de Gestión de Incidencias y Reparaciones. Ingeniería concurrente y multiplataforma desde cero.
*   **Visual:** Logo de FixFinder destacando la identidad visual (naranja/oscuro) y tus datos.

#### Diapositiva 2: El Problema vs. La Solución (Modelo Invertido)
*   **Mecánica de la diapositiva:** Usaremos una sola diapositiva dividida por la mitad, con apariciones animadas (por clics).
*   **Clic 1 (La Vía Tradicional):** Aparece un cliente estresado (icono). Salen flechas desde él hacia 4 empresas distintas. *Explicación:* El cliente pierde tiempo buscando en Google, llamando, pidiendo presupuestos uno a uno.
*   **Clic 2 (El Ecosistema FixFinder):** Desaparece lo anterior o aparece en la otra mitad. El cliente hace 1 solo clic publicando su avería en la nube. Desde la nube salen flechas hacia las 4 empresas. Las empresas le devuelven presupuestos directamente a su móvil.
*   **Mensaje clave:** Usabilidad extrema. El cliente no busca, las empresas pujan.

#### Diapositivas 3 y 4: El Ciclo de Vida en Vivo (Secuencia de GIFs)
*   **Mecánica de las diapositivas:** Para evitar fallos del directo y agilizar, usaremos una secuencia de **vídeos cortos transformados en GIFs** que se reproducen solos. Mostraremos el flujo completo de una incidencia.
*   **GIFs - Parte 1 (Creación y Gestión):**
    1.  *Cliente:* Crea incidencia y sube foto en la App.
    2.  *Gerente:* Recibe la alerta en el Dashboard en tiempo real y emite un presupuesto.
    3.  *Cliente:* Recibe el presupuesto y pulsa "Aceptar".
*   **GIFs - Parte 2 (Ejecución y Cierre):**
    4.  *Gerente:* Asigna la incidencia aceptada a un Operario concreto.
    5.  *Operario:* Recibe su tarea en la App, va al sitio y la marca como finalizada.
    6.  *Cliente:* Recibe la notificación de cierre y pone una valoración de 5 estrellas.
    7.  *Gerente:* En el Dashboard ve cómo la incidencia pasa a "Finalizado" y revisa la valoración.
*   **Mensaje clave:** El ecosistema completo está vivo, sincronizado en las tres plataformas y es 100% funcional.

---

### BLOQUE 2: La Ingeniería bajo el capó (El "Cómo")

*A partir de aquí, habiendo demostrado que el producto es increíble y funciona, damos el salto técnico para explicar por qué ha sido tan difícil y meritorio construirlo.*

#### Diapositiva 5: La Decisión Arquitectónica (Huyendo del "Camino Fácil")
*   **Visual de la diapositiva:** ¡Cero texto o casi nulo! Usaremos una **metáfora visual** muy clara para que tú aportes la narrativa de voz:
    *   *Opcíon A (El estándar):* Una imagen de una **"Caja Negra"** mágica con los logos de Spring Boot y REST. (Representa los frameworks: hacen el trabajo por ti, pero no sabes cómo funcionan por dentro).
    *   *Opción B (Lo que hemos hecho):* Una imagen de un **"Motor Abierto"** con engranajes, un icono de un cerebro "haciendo pesas" y los logos de Java Sockets y Threads. (Representa el trabajo manual, duro, pero transparente).
*   **Punto a mostrar (tu guion):** Aquí explicarás que el objetivo era **escalar y profundizar tus conocimientos**. Hacerlo todo de forma manual te ha obligado a entender cómo se comunican las máquinas byte a byte y cómo dominar los hilos concurrentes, algo que un framework te hubiera ocultado. Ya que este trabajo de fin de curso no se trata de hacer algo comercial para vender sino demostrar el aprendizaje del curso, yo he decidido demostrarlo y evolucionarlo aun mas.

#### Diapositiva 6: El Ecosistema y Despliegue en la Nube
*   **Mecánica de la diapositiva:** Usaremos una transición visual dentro de la misma diapositiva, pasando de un concepto sencillo a uno más técnico.
*   **Clic 1 (Visión General):** Aparecen imágenes genéricas e intuitivas: Un PC (Dashboard), dos móviles (App cliente/operario) y una Nube en el centro que los conecta. Sirve para explicar a grandes rasgos las tres "patas" del proyecto.
*   **Clic 2 (Transformación a AWS):** Esas imágenes genéricas desaparecen y entra a pantalla completa el **Diagrama de Despliegue AWS (Producción)**. 
*   **Punto a mostrar:** Aquí confirmas que el proyecto no corre en "localhost". Mencionas el uso de AWS EC2 para el servidor de sockets, RDS para asegurar y aislar la base de datos MySQL, y Firebase Storage para aligerar la carga de la red con el contenido multimedia.

#### Diapositivas 7, 8 y 9: Las Tres Arquitecturas (El Descenso Técnico)
*   **Mecánica Visual Recomendada (Layout 30/70):** Como lo vas a hacer a mano, te recomiendo encarecidamente no poner el diagrama solo. Divide la diapositiva: a la derecha (70% del espacio) pones el diagrama de Mermaid. A la izquierda (30%) pones 3 iconos grandes con 3 palabras clave para que el tribunal sepa de qué hablas sin tener que leer el diagrama entero.

*   **Diapositiva 7 (El Motor - Servidor Central):** 
    *   *Izquierda (Conceptos):* 📥 Dispatcher | ⚙️ Pool de Workers | 💾 Capa DAO.
    *   *Derecha (Visual):* Diagrama de Arquitectura del Backend.
    *   *Tu guion:* Explicar cómo el Dispatcher recibe las conexiones TCP y las pasa al Pool de Workers para no bloquearse.

*   **Diapositiva 8 (El Centro de Mando - Dashboard):** 
    *   *Izquierda (Conceptos):* 🖥️ UI JavaFX | 🕸️ Capa de Red | ⚡ Callbacks.
    *   *Derecha (Visual):* Diagrama de Componentes del Dashboard.
    *   *Tu guion:* Explicas la separación entre la interfaz visual (UI Thread) y la red para evitar que el programa "no responda".

*   **Diapositiva 9 (El Trabajo de Campo - App Móvil):** 
    *   *Izquierda (Conceptos):* 📱 Vistas | 🧠 Providers (Estado) | 🔌 Servicios.
    *   *Derecha (Visual):* Diagrama de Componentes de la App Flutter.
    *   *Tu guion:* Explicas la organización por capas y cómo la app delega la lógica pesada al servidor.

---

### BLOQUE 3: Los Retos Técnicos (La "Cúspide" de la Presentación)

*Ya les has enseñado las piezas. Ahora les vas a enseñar cómo resolviste los problemas de unir todas esas piezas.*

#### Diapositiva 10: Reto Técnico 1 - La Red y "El Síndrome del Embudo TCP"
*   **Punto a mostrar:** Explicar la fragmentación de paquetes TCP (cuando la red corta un JSON grande por la mitad).
*   **La solución:** Creación de un protocolo propio. Cada envío lleva una **Cabecera de 4 bytes (Big-Endian)** que le dice a la app exactamente cuánto pesa el mensaje.
*   **Visual:** Un esquema visual grande y sencillo del bloque de datos: `[CABECERA 4-BYTES] + [PAYLOAD JSON]`.

#### Diapositiva 11: Reto Técnico 2 - Concurrencia e Interfaces (El Lector Avaro)
*   **Diagrama:** **Diagrama de Secuencia y Flujo de Hilos**.
*   **Punto a mostrar:** Cómo evitar que el Dashboard (JavaFX) se congele y marque "No Responde" al recibir ráfagas de datos masivas. Explicarás tu arquitectura de 3 hilos separada (Lector Avaro, Procesador, y `Platform.runLater`).

#### Diapositiva 12: Reto Técnico 3 - Tiempo Real y Aislamiento (Broadcaster)
*   **Diagrama:** **Diagrama de Clases de Red (Servidor)**.
*   **Tu guion:** 
    *   *El Broadcaster:* ¿Cómo avisamos a un cliente de un presupuesto al instante sin que recargue la app? Patrón Observer para emitir eventos Push desde el servidor.
    *   *El Aislamiento:* Uso de `ThreadLocal` para darle a cada hilo su propia conexión privada a MySQL, evitando colisiones y corrupción de datos entre clientes concurrentes.

#### Diapositiva 13: Conclusiones y Defensa Final
*   **Punto a mostrar:** El aprendizaje real y el cumplimiento de plazos.
*   **Diagrama:** **Diagrama de Gantt** para demostrar la planificación temporal, y cierre con la frase elegida.

---

## 📝 Pasos Siguientes Recomendados
1.  **Revisión del Planteamiento:** Revisa si este flujo (Problema -> Por qué hacerlo a mano -> Explicación de cada reto técnico con su diagrama -> Resultado visual -> Conclusión) te convence.
2.  **Redacción de los *Speech*:** Una vez aprobado, podemos empezar a redactar el "guion del orador", es decir, qué decir exactamente en cada diapositiva para que suene profesional, seguro y técnico pero sin llegar a ser aburrido.
3.  **Diseño de Diapositivas:** Finalmente, plasmar esto en tu programa de presentaciones usando los diagramas que ya generamos.
