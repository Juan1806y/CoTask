# CoTask

App de tareas colaborativas para Android construida con Kotlin + Jetpack Compose, siguiendo
la **arquitectura recomendada por Google de 3 capas** (UI → Domain → Data) + MVVM.

Permite organizar tareas en listas, compartirlas con otras personas por correo con **roles**
(editor / lector), colaborar en tiempo real (asignaciones, comentarios, feed de actividad),
y mantenerse al día con recordatorios, un calendario mensual, un **widget de pantalla de
inicio** y un **panel de estadísticas**.

## Stack técnico

- **Lenguaje**: Kotlin 2.1, JVM target 17
- **UI**: Jetpack Compose (Material 3) + Navigation Compose + Modal Drawer
- **DI**: Hilt (KSP)
- **Persistencia local**: Room (single source of truth, offline-first)
- **Sincronización remota / auth**: Firebase Realtime Database + Firebase Auth
- **Google Sign-In**: Credential Manager + Google Identity (`libraries.identity.googleid`)
- **Asincronía**: Coroutines + Flow (StateFlow, `combine`, `flatMapLatest`, `callbackFlow`)
- **Preferencias**: DataStore (tema, color dinámico, onboarding, bloqueo biométrico)
- **Recordatorios**: WorkManager (notificaciones por fecha límite)
- **Seguridad**: AndroidX Biometric (bloqueo con huella/rostro)
- **Widget**: Glance (`androidx.glance:glance-appwidget`) — Compose para la pantalla de inicio
- **i18n**: español (default) + inglés (`values-en`)

## Funcionalidades

### Autenticación y perfil
- Registro e inicio de sesión con **email/contraseña** y **Google** (Credential Manager).
- Edición del **nombre de perfil**.
- **Bloqueo biométrico** opcional de la app (huella/rostro) configurable en Ajustes.

### Listas
- CRUD de listas con nombre, descripción, **favoritos** y barra de **progreso**.
- **Compartir por correo** invitando contribuyentes con **rol editor o lector**.
- **Compartir vía Intent** del sistema (WhatsApp, email, etc.).
- Pull-to-refresh para forzar re-sincronización.

### Tareas
- CRUD con fecha límite, descripción y **categoría** (dropdown híbrido + categoría custom).
- **Prioridad** (ninguna / baja / media / alta) con chip de color en la tarjeta.
- **Subtareas** (checklist embebido) con contador de progreso.
- **Tareas recurrentes** (diaria / semanal / mensual): al completarse se genera
  automáticamente la siguiente ocurrencia.
- **Asignación** de la tarea a un miembro de la lista.
- **Ordenamiento** por fecha límite, prioridad, alfabético o más recientes.
- **Búsqueda** de tareas dentro de la lista + filtros (todas / pendientes / completadas /
  por categoría).
- **Deshacer** al eliminar (snackbar con *Undo*) tanto en tareas como en listas.

### Colaboración (tiempo real vía RTDB)
- **Invitaciones** con modelo aceptar / rechazar; resolución de invitaciones pendientes
  al registrarse o iniciar sesión.
- **Roles y permisos**: propietario / editor / lector, aplicados en la UI (los lectores no
  pueden editar tareas).
- **Feed de actividad** por lista (creación, edición, completado, reapertura, borrado,
  asignación).
- **Comentarios** por tarea.

### Productividad
- **Recordatorios locales** con WorkManager para tareas con `dueDate` (notificación).
- **Calendario mensual** con marcadores de días con tareas y **proyección de recurrencias**
  futuras.
- **Widget de pantalla de inicio "Mis tareas de hoy"** (Glance): muestra las tareas
  pendientes con vencimiento hoy y abre la app al tocarlo; se refresca al crear/editar/
  completar/borrar tareas.
- **Panel de estadísticas**: tarjetas de resumen (total, completadas, pendientes, tasa de
  cumplimiento) + **gráfico de barras de tareas completadas por semana** (últimas 6 semanas),
  reutilizando el conteo `TaskCounts`.

### Experiencia
- **Onboarding** en el primer arranque.
- Tema **claro / oscuro / sistema** + **Material You** (dynamic color).
- **Internacionalización** español / inglés.
- **Offline-first**: cache local con Room + sync continuo con RTDB.

## Arquitectura

```
ui/         ← Compose screens + ViewModels (MVVM)
   ↓
domain/     ← Modelos puros, contratos de repositorio, casos de uso
   ↓
data/       ← Room (local) + Firebase (remote) + repository impls + mappers
di/         ← Módulos Hilt
widget/     ← Glance app-widget (fuera del grafo de composición de la UI)
```

- **Single source of truth**: Room. Firebase RTDB se observa en background y refleja los
  cambios al cache local; la UI siempre observa Room vía Flow.
- **ViewModels** exponen `StateFlow<UiState>` recolectado con `collectAsStateWithLifecycle()`.
- **Casos de uso** validan input antes de tocar el repositorio.
- **Sincronización por dueño**: un único listener de RTDB por `ownerId` reconcilia de forma
  atómica todas las tareas de ese dueño contra las listas conocidas localmente (evita
  carreras entre listas y borrados accidentales en cascada).
- **El widget vive fuera de Hilt**: al ser un `GlanceAppWidget` (no un componente Android
  inyectable), accede al `TaskDao` mediante un `@EntryPoint` de Hilt resuelto con
  `EntryPointAccessors`. El repositorio notifica al widget (`updateAll`) tras cada mutación.

## Configuración de Firebase

1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com/).
2. Añade una app **Android** con el package `com.uni.colabtasks` (y `com.uni.colabtasks.debug`
   para builds de debug).
3. Sube la SHA‑1 de tu keystore:
   - Debug: `./gradlew signingReport` o
     `keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android`
4. Descarga `google-services.json` y colócalo en `app/`.
5. En **Build → Authentication**, habilita los proveedores **Email/Password** y **Google**.
6. En **Build → Realtime Database**, crea una base de datos.
7. Copia el **Web client ID** (OAuth 2.0 *Web application*) en `app/src/main/res/values/strings.xml`
   reemplazando `default_web_client_id`.

## Modelo de datos en Firebase RTDB

```
/userProfiles/{uid}                      { uid, email, displayName, photoUrl, updatedAt }
/usersByEmail/{emailKey}: uid            (índice email→uid para invitar contribuyentes)

/users/{ownerId}/lists/{listId}          { id, ownerId, name, description, isFavorite,
                                            contributors[], viewerEmails[],
                                            memberIds[], viewerIds[], createdAt, updatedAt }
/users/{ownerId}/tasks/{taskId}          { id, listId, ownerId, title, description, category,
                                            isCompleted, dueDate, priority, assignedTo,
                                            recurrence, subtasks[], createdAt, updatedAt }

/users/{ownerId}/activity/{listId}/{id}  { actorName, action, taskTitle, timestamp }
/users/{ownerId}/comments/{taskId}/{id}  { id, authorUid, authorName, text, timestamp }

/users/{memberUid}/sharedListPointers/{listId}: { ownerId }
                                         (punteros para listas compartidas con este usuario)
/users/{inviteeUid}/invitations/{listId} { listId, ownerId, listName, inviterName, role }
                                         (invitaciones pendientes de aceptar/rechazar)
```

`emailKey` es el email normalizado a una key segura para RTDB (lowercase + `.` `/` `$` `#`
`[` `]` reemplazados por `_`).

### Roles de miembro

Cada lista distingue **editores** (`memberIds` / `contributors`) de **lectores**
(`viewerIds` / `viewerEmails`). `TaskList.roleFor(uid)` deriva el rol
(`OWNER` / `EDITOR` / `VIEWER`) y `canEditTasks(uid)` decide si la UI permite mutar tareas.

### Cómo funcionan los contribuyentes

1. Al iniciar sesión, la app escribe automáticamente `/userProfiles/{miUid}` y
   `/usersByEmail/{miEmailKey}: miUid`.
2. Cuando creas/editas una lista con emails de contribuyentes:
   - El repositorio resuelve cada email → uid via `/usersByEmail/*`. Los emails sin cuenta
     se conservan en `contributors`/`viewerEmails` (para reintento futuro) pero no en
     `memberIds`/`viewerIds`.
   - La lista se guarda en `/users/{ownerId}/lists/{listId}` con los uids resueltos.
   - Se escribe una **invitación** en `/users/{inviteeUid}/invitations/{listId}`.
3. El invitado ve la invitación y puede **aceptar o rechazar**. Al aceptar, se planta
   `/users/{miUid}/sharedListPointers/{listId}: { ownerId }` y la lista aparece junto a
   las propias.
4. Cuando un contribuyente crea/edita/marca una tarea o comenta, ésta se escribe bajo el
   árbol del **dueño** de la lista (`/users/{listOwnerId}/...`), no del contribuyente — para
   que todos los miembros vean los mismos datos.

## Reglas de seguridad RTDB sugeridas

```json
{
  "rules": {
    "userProfiles": {
      "$uid": {
        ".read": "auth != null",
        ".write": "auth != null && $uid === auth.uid"
      }
    },
    "usersByEmail": {
      "$emailKey": {
        ".read": "auth != null",
        ".write": "auth != null && newData.val() === auth.uid"
      }
    },
    "users": {
      "$uid": {
        "lists": {
          "$listId": {
            ".read": "auth != null && ($uid === auth.uid || data.child('memberIds').val().contains(auth.uid))",
            ".write": "auth != null && $uid === auth.uid"
          }
        },
        "tasks": {
          "$taskId": {
            ".read": "auth != null && ($uid === auth.uid || root.child('users').child($uid).child('lists').child(data.child('listId').val()).child('memberIds').val().contains(auth.uid))",
            ".write": "auth != null && ($uid === auth.uid || root.child('users').child($uid).child('lists').child(newData.child('listId').val()).child('memberIds').val().contains(auth.uid))"
          }
        },
        "sharedListPointers": {
          ".read": "auth != null && $uid === auth.uid",
          ".write": "auth != null"
        }
      }
    }
  }
}
```

> ⚠️ **Nota MVP**: las reglas anteriores son una aproximación. RTDB no tiene operador
> `contains` nativo sobre arrays — para producción conviene reestructurar `memberIds`
> a un mapa `{ uid: true }` para que las reglas usen `.hasChild(auth.uid)`. Para el demo,
> puedes aflojar las reglas a `auth != null` y restringir después. Por la misma razón el
> cliente **no usa `orderByChild`** (filtra en memoria) para no requerir índices `.indexOn`.

## Modelo de datos en Room (local)

- `task_lists` — espejo de `/users/{ownerId}/lists/*`. Para listas compartidas, las filas
  tienen `ownerId == listOwnerId ≠ currentUid`.
- `tasks` — espejo de `/users/{ownerId}/tasks/*`. Para tareas de listas compartidas,
  `ownerId == listOwnerId`. Listas (subtareas, etc.) y campos compuestos se serializan a
  **JSON** vía `TypeConverters` (org.json).

Las **listas compartidas** se cachean en Room a medida que `sharedListPointers` emite, lo
que permite navegar y operar sobre ellas sin perder el offline-first. El DAO usa `@Upsert`
(no `INSERT OR REPLACE`) para no disparar borrados en cascada por claves foráneas.

## Cómo correr la app

1. Abre el proyecto en **Android Studio Iguana** o superior.
2. Verifica que `app/google-services.json` exista.
3. Sync Gradle. Run en un emulador o dispositivo con **Android 8+ (API 26)**.
4. (Opcional) Mantén pulsada la pantalla de inicio → **Widgets** → *CoTask – Mis tareas de
   hoy* para añadir el widget.

## Estructura de carpetas

```
app/src/main/java/com/uni/colabtasks/
├── ColabTasksApp.kt              # Application con @HiltAndroidApp
├── MainActivity.kt               # FragmentActivity que monta AppRoot (necesaria para biometría)
├── di/                           # Hilt: Database, Firebase, Repository, Coroutines, Qualifiers
├── domain/
│   ├── model/                    # Task, TaskList, User, UserProfile, TaskCounts, Priority,
│   │                             #   Recurrence, Subtask, MemberRole, ActivityEntry, Comment,
│   │                             #   Invitation, ThemeMode, AppPreferences, TaskFilter, TaskSort
│   ├── repository/               # Interfaces (Auth, TaskList, Task, Preferences, UserDirectory,
│   │                             #   Activity, Comment)
│   └── usecase/                  # auth/, tasklist/, task/, activity/
├── data/
│   ├── local/                    # Room: entities, DAOs, Converters (JSON), Database
│   ├── remote/                   # Firebase auth + RTDB data sources, DTOs, UserDirectory
│   ├── preferences/              # DataStore impl (tema, onboarding, biometría)
│   ├── mapper/                   # Entity ↔ Domain ↔ DTO
│   └── repository/               # Auth/TaskList/Task/Preferences/UserDirectory/... impls
├── reminder/                     # WorkManager: ReminderScheduler + ReminderWorker
├── widget/                       # Glance: MyTasksWidget, Receiver, EntryPoint, Notifier
└── ui/
    ├── AppRoot.kt                # Tema reactivo + gate biométrico + NavGraph
    ├── navigation/               # AppNavGraph + Destinations + DrawerSection
    ├── theme/                    # CoTask theme (naranja) + dynamic color
    ├── common/                   # UiState, LoadingIndicator, EmptyState, AppDrawerContent
    ├── lock/                     # Pantalla de bloqueo biométrico
    ├── onboarding/               # Pantallas de bienvenida (primer arranque)
    ├── auth/                     # Login, SignUp, GoogleSignInHelper, AuthGateViewModel
    ├── tasklists/                # Mis Listas + EditListDialog (contribuyentes + roles)
    ├── tasks/                    # Tareas de una lista + stats + filtros + búsqueda + orden
    ├── taskedit/                 # Modal crear/editar tarea (fecha, prioridad, subtareas,
    │                             #   recurrencia, asignación, categoría híbrida)
    ├── activity/                 # Feed de actividad por lista
    ├── calendar/                 # Vista mensual con marcadores + proyección de recurrencias
    ├── stats/                    # Panel de estadísticas (resumen + gráfico completadas/semana)
    ├── settings/                 # Tema, dynamic color, biometría, perfil, sign-out
    └── util/                     # DateFormatting, ShareIntent
```

## Próximos pasos sugeridos

- **Tests unitarios** de casos de uso y del agregador de estadísticas (la capa domain ya está
  desacoplada y lista para mockear repos).
- **Tests de UI** (Compose) e instrumentados de Room.
- **CI** con GitHub Actions: lint + tests en cada push.
- **Cloud Functions** para invitaciones a emails que aún no son usuarios (correo de bienvenida).
- **Migrar `memberIds` a mapa** en RTDB (`{ uid: true }`) para reglas con `.hasChild` e índices.

## Licencia

Proyecto académico — uso educativo.
