# eScrims – Plataforma de organización de partidas eSports

**Trabajo Práctico Final – Patrones de Diseño**  
**Proceso de Desarrollo de Software - UADE Costa Argentina**

## Descripción del proyecto

eScrims es una plataforma desarrollada en Java para organizar partidas amistosas (*scrims*) de videojuegos competitivos como Valorant, League of Legends y Counter-Strike 2.

El sistema permite gestionar encuentros, jugadores y equipos, incorporando criterios de emparejamiento según rango, región, latencia y compatibilidad entre participantes.

El desarrollo se basa en programación orientada a objetos, arquitectura por capas y aplicación de patrones de diseño.

## Tecnologías

- **Lenguaje:** Java
- **IDE:** IntelliJ IDEA
- **Control de versiones:** Git y GitHub

## Funcionalidades principales

- Registro y gestión de usuarios.
- Creación, búsqueda y administración de scrims.
- Postulación y confirmación de jugadores.
- Emparejamiento según rango, latencia e historial.
- Gestión de equipos y roles.
- Administración de estados de las partidas.
- Notificaciones, estadísticas y moderación.

## Patrones de diseño

| Patrón | Aplicación |
|---|---|
| State | Gestión de los estados de un scrim. |
| Strategy | Algoritmos de emparejamiento. |
| Observer | Notificaciones ante eventos del sistema. |
| Abstract Factory | Creación de notificadores. |
| Facade | Simplificación de operaciones del sistema. |

## Ejecución

1. Clonar el repositorio.
2. Abrir el proyecto en IntelliJ IDEA.
3. Configurar el JDK.
4. Ejecutar `Main.java`.

## Integrantes

- **Ovelar Evangelina**
- **Peralta Luka**
