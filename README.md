# CoTask

App de tareas colaborativas para Android construida con Kotlin + Jetpack Compose, siguiendo
la **arquitectura recomendada por Google de 3 capas** (UI → Domain → Data).

## Stack técnico

- **Lenguaje**: Kotlin 2.1, JVM target 17
- **UI**: Jetpack Compose (Material 3) + Navigation Compose + Drawer
- **DI**: Hilt (KSP)
- **Persistencia local**: Room
- **Sincronización remota / auth**: Firebase Realtime Database + Firebase Auth
- **Google Sign-In**: Credential Manager + Google Identity (libraries.identity.googleid)
- **Coroutines + Flow** para asincronía reactiva
- **DataStore** para preferencias (tema claro/oscuro, dynamic color)

## Funcionalidades

- Autenticación con email/contraseña y Google
- CRUD de listas con descripción, favoritos, progreso, **contribuyentes invitados por email**
- CRUD de tareas con fecha límite, categoría (dropdown híbrido + custom) y filtros
- **Calendario mensual** con marcadores de días con tareas
- **Compartir lista** vía Intent del sistema (WhatsApp, email, etc.)
- Tema claro/oscuro/sistema + opción de Material You dynamic color
- Sincronización offline-first: cache local con Room + sync continuo con RTDB

## Arquitectura

```
ui/         ← Compose screens + ViewModels (MVVM)
   ↓
domain/     ← Modelos puros, contratos de repositorio, casos de uso
   ↓
data/       ← Room (local) + Firebase (remote) + repository impls + mappers
di/         ← Módulos Hilt
```

- **Single source of truth**: Room. Firebase RTDB se observa en background y refleja
  los cambios al cache local; la UI siempre observa Room vía Flow.
- **ViewModels** exponen `StateFlow<UiState>` recolectado con `collectAsStateWithLifecycle()`.
- **Casos de uso** validan input antes de tocar el repositorio.

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
                                            contributors[], memberIds[], createdAt, updatedAt }
/users/{ownerId}/tasks/{taskId}          { id, listId, ownerId, title, description, category,
                                            isCompleted, dueDate, createdAt, updatedAt }

/users/{memberUid}/sharedListPointers/{listId}: { ownerId }
                                         (punteros para listas compartidas con este usuario)
```

`emailKey` es el email normalizado a una key segura para RTDB (lowercase + `.` `/` `$` `#`
`[` `]` reemplazados por `_`).

### Cómo funcionan los contribuyentes

1. Al iniciar sesión, la app escribe automáticamente `/userProfiles/{miUid}` y
   `/usersByEmail/{miEmailKey}: miUid`.
2. Cuando creas/editas una lista con emails de contribuyentes:
   - El repositorio resuelve cada email → uid via `/usersByEmail/*`. Los emails sin cuenta
     se conservan en `contributors` (para reintento futuro) pero no en `memberIds`.
   - La lista se guarda en `/users/{ownerId}/lists/{listId}` con `memberIds = [...uids]`.
   - Por cada miembro resuelto se planta `/users/{memberUid}/sharedListPointers/{listId}: { ownerId }`.
3. Cuando un contribuyente abre la app, observa sus `sharedListPointers` y por cada uno
   lee la lista del owner correspondiente. La lista aparece junto con sus listas propias.
4. Cuando un contribuyente crea/edita/marca una tarea, ésta se escribe bajo el árbol del
   **dueño** de la lista (`/users/{listOwnerId}/tasks/*`), no del contribuyente — para que
   todos los miembros vean los mismos datos.

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
> puedes aflojar las reglas a `auth != null` y restringir después.

## Modelo de datos en Room (local)

- `task_lists` — espejo de `/users/{ownerId}/lists/*`. Para listas compartidas, las filas
  tienen `ownerId == listOwnerId ≠ currentUid`.
- `tasks` — espejo de `/users/{ownerId}/tasks/*`. Para tareas de listas compartidas,
  `ownerId == listOwnerId`.

Las **listas compartidas** se cachean en Room a medida que `sharedListPointers` emite, lo
que permite navegar y operar sobre ellas sin perder el offline-first.

## Cómo correr la app

1. Abre el proyecto en **Android Studio Iguana** o superior.
2. Verifica que `app/google-services.json` exista.
3. Sync Gradle. Run en un emulador o dispositivo con **Android 8+ (API 26)**.

## Estructura de carpetas

```
app/src/main/java/com/uni/colabtasks/
├── ColabTasksApp.kt              # Application con @HiltAndroidApp
├── MainActivity.kt               # Activity que monta AppRoot
├── di/                           # Hilt: Database, Firebase, Repository, Coroutines, Qualifiers
├── domain/
│   ├── model/                    # Task, TaskList, User, UserProfile, TaskCategory, TaskCounts,
│   │                             #   ThemeMode, AppPreferences, AuthResult, TaskFilter
│   ├── repository/               # Interfaces (Auth, TaskList, Task, Preferences, UserDirectory)
│   └── usecase/                  # auth/, tasklist/, task/
├── data/
│   ├── local/                    # Room: entities, DAOs, Converters, Database
│   ├── remote/                   # Firebase auth + RTDB data sources, DTOs, UserDirectory
│   ├── preferences/              # DataStore impl (theme)
│   ├── mapper/                   # Entity ↔ Domain ↔ DTO
│   └── repository/               # Auth/TaskList/Task/Preferences/UserDirectory impls
└── ui/
    ├── AppRoot.kt                # Tema reactivo + NavGraph
    ├── navigation/               # AppNavGraph + Destinations + DrawerSection
    ├── theme/                    # CoTask theme (naranja) + dynamic color
    ├── common/                   # UiState, LoadingIndicator, EmptyState, AppDrawerContent
    ├── auth/                     # Login, SignUp, GoogleSignInHelper, AuthGateViewModel
    ├── tasklists/                # Mis Listas + EditListDialog (con contribuyentes)
    ├── tasks/                    # Tareas de una lista + stats + filtros
    ├── taskedit/                 # Modal crear/editar tarea (DatePicker + categoría híbrida)
    ├── calendar/                 # Vista mensual con marcadores
    ├── settings/                 # Tema, dynamic color, sign-out
    └── util/                     # DateFormatting, ShareIntent
```

## Próximos pasos sugeridos

- **Notificaciones locales** con WorkManager/AlarmManager para tareas con `dueDate`.
- **Tests unitarios** de casos de uso (ya están desacoplados, listos para mockear los repos).
- **Cloud Functions** para resolver invitaciones de emails que aún no son usuarios (envío
  de correo de bienvenida).
- **Migrar `memberIds` a mapa** en RTDB (`{ uid: true }`) para reglas con `.hasChild`.
- **CI** con GitHub Actions: lint + tests en cada push.

## Licencia

Proyecto académico — uso educativo.
