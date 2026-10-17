# Guion de verificación — Spec 025

Sin dispositivo no se comprueba el aspecto de un tema. Esto es lo que hay que mirar, en este orden.

## 1. Que nadie vea cambios (lo primero, y lo más importante)

Con la app **tal como estaba antes de instalarla** —con `dynamic_colors` apagado si lo tenías apagado—:

1. Abre la app. Debe verse **igual que antes**: el morado, no Material You.
2. Abre el editor de código. Debe verse **VS Code Dark Modern**, como siempre.
3. Abre una nota que tenga un bloque de código. Los acentos deben ser los de siempre
   (azul `#3B82F6`, verde `#16A34A`, gris `#8B93A3`).
4. Si tenías el interruptor **encendido** (o no lo habías tocado nunca), la app se ve con Material You y el
   editor en VS Code Dark. Igual que antes.

## 2. El selector

5. Ajustes → Aspecto. Debe haber **dos** filas: «Tema» y «Modo oscuro». **No** hay interruptor de colores dinámicos.
6. «Tema» muestra el nombre del tema actual. Al abrirlo, seis filas con dos muestras de color cada una.
7. La fila del tema activo está en negrita y con un ✓.

## 3. Cada tema, en claro y en oscuro

8. Elige **Predeterminado**: los morados de siempre, en las dos filas y en la hoja.
9. Elige **VS Code Dark Modern**: superficies casi blancas en claro y muy oscuras en oscuro, acentos azules.
10. Elige **One Dark**: superficie con matiz azulado-violáceo, acento azul claro.
11. Elige **Monokai**: acento rosa fuerte y superficies con matiz cálido.
12. Elige **GitHub Light**: superficies muy claras y acentos rojos/verdes de GitHub.
13. En cada uno: mira una nota larga, el chat, Ajustes y el buscador. Busca texto **pegado** a un botón y texto
    sobre una tarjeta: tiene que leerse.
14. Cambia el modo (Sistema / Encendido / Apagado) con un tema puesto: el tema **no** cambia de color, solo de
    lado. Un tema nunca cambia el modo.

## 4. Que el tema llegue a todas partes

15. Con **Monokai** puesto, abre el editor de código: debe verse en Monokai.
16. Con **Monokai**, abre una nota con un bloque de código: las palabras clave en rosa, las cadenas en amarillo.
17. Con **Monokai**, activa la burbuja flotante: se repinta.
18. Abre un PDF desde la app: se repinta.
19. Abre la configuración del widget: se repinta.
20. Provoca un fallo (la pantalla del informe de fallos): se repinta.

## 5. Que se recuerde

21. Elige **One Dark**, cierra la app del todo (no solo laActivity) y vuelve a abrirla. Sigue en One Dark.
22. Con la burbuja y el widget activos, repite el cierre y la reapertura. Siguen en One Dark.

## 6. Cambiar con las cosas abiertas

23. Abre el editor con un archivo, vuelve atrás, cambia el tema a **GitHub Light** y vuelve a entrar al editor: se
    ve en GitHub Light sin cerrar el archivo.
24. Abre una nota con un bloque de código, cambia de tema y vuelve a la nota: el bloque cambia de color **sin**
    teclear nada. Si se queda con los colores viejos, la paleta no está en la clave del `remember`.

## 7. Robustez

25. Elige un tema y reinicia el dispositivo. Sigue igual.
26. En un dispositivo con Material You, comprueba que el tema Dinámico sigue tomando los colores del fondo.