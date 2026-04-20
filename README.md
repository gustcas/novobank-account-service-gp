# NovoBanco — Microservicio de Cuentas y Transacciones

## Descripción del Problema

NovoBanco requiere un microservicio de nivel financiero que gestione cuentas bancarias y
transacciones para su app móvil y portal web. El dominio bancario impone restricciones
estrictas: el saldo nunca puede ser negativo, las transferencias deben ser atómicas y
completamente revertibles, y cada operación debe quedar auditada.

El sistema implementa el patrón CQRS implícito (operaciones de escritura con bloqueo
pesimista, lecturas con `readOnly = true`), arquitectura hexagonal para aislar el dominio
de la infraestructura, y PostgreSQL 16 como motor de base de datos por sus garantías
transaccionales ACID y capacidades de bloqueo a nivel de fila.

La prioridad de diseño es **corrección sobre performance**: en un contexto bancario, un
centavo perdido es inaceptable. Los índices y optimizaciones existen, pero nunca a
expensas de la consistencia de datos.

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                     │
│  ┌──────────────┐              ┌──────────────────────────┐ │
│  │  Web Layer   │              │   Persistence Layer      │ │
│  │  (REST API)  │              │   (JPA / PostgreSQL)     │ │
│  │              │              │                          │ │
│  │ Controllers  │              │  AccountPersistence      │ │
│  │ DTOs         │              │  Adapter                 │ │
│  │ ExHandler    │              │  TransactionPersistence  │ │
│  └──────┬───────┘              │  Adapter                 │ │
│         │                      └──────────────┬───────────┘ │
└─────────┼──────────────────────────────────────┼────────────┘
          │ (port/in interfaces)  (port/out interfaces)       │
┌─────────▼──────────────────────────────────────▼────────────┐
│                    APPLICATION LAYER                        │
│   CreateAccountService  DepositService  WithdrawalService   │
│   TransferService       GetAccountService  GetHistoryService│
└─────────────────────────┬───────────────────────────────────┘
                          │ (domain interfaces only)
┌─────────────────────────▼───────────────────────────────────┐
│                     DOMAIN LAYER (núcleo)                   │
│                                                             │
│  model/          Account.java  Transaction.java             │
│                  AccountType   AccountStatus                │
│                  TransactionType  TransactionStatus         │
│                                                             │
│  exception/      DomainException (sealed interface)         │
│                  InsufficientFundsException                 │
│                  AccountBlockedException                    │
│                  AccountClosedException                     │
│                  AccountNotFoundException                   │
│                  InvalidAmountException                     │
│                                                             │
│  port/in/        CreateAccountUseCase  DepositUseCase       │
│                  WithdrawalUseCase  TransferUseCase         │
│                  GetAccountUseCase  GetHistoryUseCase       │
│                                                             │
│  port/out/       AccountRepository  TransactionRepository   │
│                  (interfaces puras — sin Spring/JPA)        │
└─────────────────────────────────────────────────────────────┘
```

### Reglas de Dependencia (Hexagonal)
- El dominio **no depende** de ninguna capa externa
- La aplicación depende **solo** de interfaces del dominio
- La infraestructura implementa los puertos del dominio
- Los controladores dependen **solo** de interfaces de casos de uso

---

## Inicio Rápido

### Opción A — Docker Compose (recomendado, un solo comando)

```bash
cp .env.example .env
docker-compose up
```

La aplicación estará disponible en:
- **API:** http://localhost:8080/api/v1
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **pgAdmin:** http://localhost:5050

Docker Compose levanta PostgreSQL 16, ejecuta el script de permisos automáticamente y luego inicia la aplicación. No requiere ninguna configuración adicional.

---

### Opción B — PostgreSQL local (sin Docker)

Si tienes PostgreSQL instalado localmente, ejecuta estos comandos **una sola vez** como superusuario (`postgres`):

```sql
-- Conectado como postgres, en cualquier base de datos
CREATE DATABASE novobanco;
CREATE USER novobanco WITH PASSWORD 'novobanco123';
GRANT ALL PRIVILEGES ON DATABASE novobanco TO novobanco;

-- Ahora conectado a la base novobanco
ALTER SCHEMA public OWNER TO novobanco;
```

> **Nota para PostgreSQL 15+:** A partir de la versión 15, el permiso CREATE en el esquema
> `public` ya no se otorga por defecto a usuarios no-superusuario. El comando
> `ALTER SCHEMA public OWNER TO novobanco` resuelve esto de forma permanente.

Luego configura las variables de entorno o modifica `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/novobanco
spring.datasource.username=novobanco
spring.datasource.password=novobanco123
```

---

### Ejecutar pruebas

```bash
# Pruebas unitarias (sin infraestructura)
mvn test

# Pruebas de integración (requiere Docker para Testcontainers)
mvn verify

# Todo
mvn verify -B
```

---

## Endpoints

| Método | Ruta                                               | Descripción                                      |
|--------|----------------------------------------------------|--------------------------------------------------|
| POST   | `/api/v1/accounts`                                 | Crear cuenta                                     |
| GET    | `/api/v1/accounts?customerId={customerId}`         | Listar cuentas de un cliente                     |
| GET    | `/api/v1/accounts/{accountId}`                     | Consultar cuenta por ID                          |
| GET    | `/api/v1/accounts/number/{accountNumber}`          | Consultar cuenta por número                      |
| POST   | `/api/v1/accounts/{accountId}/transactions/deposits`    | Depositar fondos                            |
| POST   | `/api/v1/accounts/{accountId}/transactions/withdrawals` | Retirar fondos                              |
| POST   | `/api/v1/accounts/{accountId}/transactions/transfers`   | Transferencia atómica entre cuentas         |
| GET    | `/api/v1/accounts/{accountId}/transactions`        | Historial de movimientos paginado                |

### Ejemplos de Request

**Crear cuenta:**
```json
POST /api/v1/accounts
{
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "SAVINGS"
}
```

**Listar cuentas por customerId:**
```http
GET /api/v1/accounts?customerId=550e8400-e29b-41d4-a716-446655440000
```

**Depositar:**
```json
POST /api/v1/accounts/{accountId}/transactions/deposits
Idempotency-Key: a1b2c3d4-e5f6-7890-abcd-ef1234567890  (opcional)
{
  "amount": 1000.00
}
```

**Transferencia:**
```json
POST /api/v1/accounts/{sourceId}/transactions/transfers
{
  "targetAccountId": "660e8400-e29b-41d4-a716-446655440001",
  "amount": 200.00
}
```

### Flujo de integracion para frontend

Cuando el canal consumidor autentica al usuario en un `auth-service` separado, el flujo
recomendado para descubrir sus cuentas es:

1. El `auth-service` devuelve el `customerId` del usuario autenticado.
2. El frontend consulta `GET /api/v1/accounts?customerId=...`.
3. Si existen varias cuentas, la interfaz permite seleccionar la cuenta activa.
4. El `accountId` seleccionado se reutiliza luego en saldo, historial, depÃ³sito,
   retiro y transferencia.

Este endpoint complementa el contrato original por `accountId` y permite una
integraciÃ³n web/mobile coherente cuando un cliente posee mÃºltiples cuentas.

---

## Resolución de Escenarios de Negocio Críticos

### Escenario 1: Saldo Negativo

**Implementación: doble capa de defensa**

```
Request Retiro $500  (saldo actual: $100)
         │
         ▼
[Capa Aplicación] account.debit(500)
  → balance (100) < amount (500)
  → throws InsufficientFundsException
  → HTTP 422 con detalle: cuenta, saldo actual, monto solicitado
         │
         ▼ (si el bug omite la validación de aplicación)
[Capa DB] CHECK (balance >= 0)
  → PostgreSQL rechaza el UPDATE
  → DataIntegrityViolationException → HTTP 500
```

**¿Por qué doble capa?**
- La capa de aplicación provee mensajes ricos con contexto (cuenta, monto, saldo actual) → mejor UX
- El constraint de DB es la última línea de defensa contra bugs en el código o accesos directos a la DB
- Si solo existiera la capa de aplicación: un bug que omita la validación corrompería el saldo
- Si solo existiera la DB: el error sería genérico y no expondría el contexto necesario para el cliente

### Escenario 2: Cuenta Inactiva

`Account.validateOperational()` usa el estado de la cuenta para lanzar excepciones específicas:

```java
// En Account.java (dominio puro)
public void validateOperational() {
    if (status == BLOCKED) throw new AccountBlockedException(accountNumber);
    if (status == CLOSED)  throw new AccountClosedException(accountNumber);
}
```

Respuesta HTTP diferenciada:
- `AccountBlockedException` → HTTP 422, `errorCode: "ACCOUNT_BLOCKED"`
- `AccountClosedException`  → HTTP 422, `errorCode: "ACCOUNT_CLOSED"`

Nunca se retorna un 400 genérico. El `GlobalExceptionHandler` mapea cada tipo de excepción
al formato RFC 7807 con `errorCode` específico.

### Escenario 3: Fallo en Transferencia Parcial

```java
@Transactional  // Spring inicia la transacción
public List<Transaction> transfer(...) {
    source.debit(amount);   // 1. Débito en memoria
    target.credit(amount);  // 2. Crédito en memoria
    accountRepository.save(source);  // 3. Flush a DB
    accountRepository.save(target);  // 4. Flush a DB
    // 5. Registrar ambas transacciones
    // 6. Commit (ambas o ninguna)
}
```

**¿Qué pasa si la JVM cae entre el paso 3 y el 4?**
Sin `@Transactional`, el débito persistiría sin el crédito. Con `@Transactional`:
- Si la JVM cae antes del commit, PostgreSQL revierte la transacción completa al restart
- El débito y el crédito son atómicos — persisten juntos o no persisten
- El registro de transacción nunca queda en estado REVERSED por una caída de JVM:
  o todo persiste (SUCCESS) o nada persiste (la transacción no existe en DB)

### Escenario 4: Concurrencia Básica

```
Thread-A: findByIdForUpdate(id)   → LOCK row
Thread-B: findByIdForUpdate(id)   → WAIT (bloqueado por A)
Thread-A: debit(100)              → balance 100→0
Thread-A: save(account)           → flush
Thread-A: commit                  → UNLOCK
Thread-B: resumes                 → lee balance=0
Thread-B: debit(100)              → InsufficientFundsException
```

**¿Por qué pesimista y no optimista?**

El bloqueo **optimista** usa un número de versión:
```
Thread-A y Thread-B leen version=1 simultáneamente
Thread-A hace commit → version=2
Thread-B intenta commit → detecta version != 1 → OptimisticLockException
Thread-B debe reintentar desde el principio
```

En banca:
- El costo de un conflicto no detectado (saldo negativo) es **irreversible**
- Los reintentos del cliente son **predecibles** (retiro del mismo saldo frecuente)
- La ventana de tiempo del lock es muy corta (una operación DB atómica)
- **Pesimista** garantiza que el segundo hilo siempre ve el saldo actualizado sin reintentar

El bloqueo optimista es mejor cuando los conflictos son raros y los reintentos baratos.
En retiros bancarios concurrentes, los conflictos son frecuentes y el costo de un saldo
negativo es alto. El pesimista es la elección correcta.

### Escenario 5: Idempotencia en Depósito

**Implementación en `DepositService`:**

```java
if (idempotencyKey != null) {
    Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
        return existing.get();  // Retorno sin procesar el depósito nuevamente
    }
}
```

**Flujo de idempotencia:**
1. Cliente envía `POST /deposits` con header `Idempotency-Key: uuid`
2. Primera vez: no existe → procesamos el depósito y guardamos con `idempotency_key`
3. Segunda vez (reintento por red): existe → retornamos la misma transacción sin acreditar
4. El saldo no cambia en el segundo request

**Defensa en DB:** `UNIQUE constraint` en `idempotency_key` garantiza que incluso con
race conditions (dos requests llegan al mismo tiempo antes de que el primero persista),
solo uno tendrá éxito — el segundo recibirá `DataIntegrityViolationException`.

**¿Qué haría el sistema sin implementación de idempotencia?**
Un depósito enviado dos veces por error de red (timeout seguido de reintento) acreditaría
el saldo dos veces. El cliente vería el doble del dinero esperado. Con el `Idempotency-Key`
y el UNIQUE constraint, esto es imposible.

---

## Decisiones de Diseño de Base de Datos

### ¿Normalización vs. Desnormalización?

El esquema está **normalizado** (3NF). Justificación para el dominio bancario:
- Las reglas de negocio requieren que el saldo sea consistente con el historial de transacciones
- Una desnormalización (guardar saldo en `transactions`) crearía duplicación y riesgo de divergencia
- El historial de transacciones es inmutable — no hay UPDATE en producción, solo INSERT
- Los JOINs entre `accounts` y `transactions` son simples (FK directa) y baratos con índices

**Trade-off aceptado:** Algunas consultas de reportes requieren JOINs. Para reportes
complejos a escala, se usaría una réplica de lectura o un modelo de datos analítico separado.

### Estructura de Índices — Justificación de Cada Uno

| Índice | Columnas | Consulta que soporta |
|--------|----------|----------------------|
| `idx_transactions_account_created` | `(account_id, created_at DESC)` | Historial paginado — la query más frecuente |
| `idx_transactions_type_created` | `(type, created_at DESC)` | Transferencias salientes por período |
| `idx_accounts_customer_id` | `(customer_id)` | Todas las cuentas de un cliente |
| UNIQUE en `reference` | `(reference)` | Búsqueda por referencia (soporte) — implícito del constraint |
| UNIQUE en `idempotency_key` | `(idempotency_key)` | Verificación de idempotencia — implícito del constraint |
| UNIQUE en `account_number` | `(account_number)` | Login/búsqueda por número — implícito del constraint |

**¿Por qué el índice compuesto `(account_id, created_at DESC)?`**
La consulta de historial es `WHERE account_id = ? ORDER BY created_at DESC LIMIT 20`.
PostgreSQL puede resolver esta consulta con un único Index Scan sin Sort: navega por el
índice en orden DESC directamente. Sin el índice compuesto, necesitaría:
1. Seq Scan de toda la tabla filtrado por `account_id`
2. Sort en memoria/disco por `created_at`
Esto es O(n log n) vs O(log n + k) con el índice. Con 100M de transacciones, la diferencia
es segundos vs milisegundos.

### ¿Cómo soporta el esquema el historial paginado?

```sql
-- Query ejecutada por JpaTransactionRepository
SELECT t FROM TransactionEntity t
WHERE t.account.id = :accountId
ORDER BY t.createdAt DESC
-- + LIMIT/OFFSET via Spring Pageable

-- Plan de ejecución esperado:
Index Scan Backward using idx_transactions_account_created
  on transactions (account_id = 'uuid', created_at DESC)
  LIMIT 20
```

La paginación usa `LIMIT/OFFSET`. Para tablas muy grandes (>100M filas), `OFFSET` degrada
porque PostgreSQL debe leer y descartar las filas anteriores. Alternativa: **keyset pagination**
(`WHERE (account_id, created_at) < (?, ?)`) — documentada como mejora futura.

### ¿Dónde vive la regla de negocio?

**En ambas capas, con roles distintos:**

| Capa | Responsabilidad | Por qué |
|------|----------------|---------|
| **Aplicación** (`Account.debit()`) | Primera defensa — valida antes de ir a DB | Mensaje de error rico con contexto, control de flujo, lanzar excepciones tipadas |
| **Base de datos** (`CHECK balance >= 0`) | Última defensa — constraint de integridad | Protege contra bugs en la app, accesos directos a DB, migraciones incorrectas |

El dominio es el **árbitro** de las reglas de negocio. La DB es el **guardián** de la
integridad estructural. No son redundantes — son complementarios.

---

## Architecture Decision Records (ADR)

### ADR-001: PostgreSQL sobre MongoDB o MySQL

**Contexto:** El sistema necesita transferencias atómicas entre dos cuentas con garantía
de rollback completo ante cualquier fallo.

**Opciones consideradas:**
1. **MongoDB** — escalabilidad horizontal, documentos flexibles
2. **MySQL 8** — relacional, ampliamente conocido
3. **PostgreSQL 16** — relacional avanzado, ACID nativo, extensiones

**Decisión:** PostgreSQL 16.

**Razones:**
1. **Transacciones ACID nativas** — `@Transactional` con rollback completo garantizado
2. **`SELECT FOR UPDATE`** — bloqueo pesimista a nivel de fila sin plugins adicionales
3. **`CHECK (balance >= 0)`** — constraint a nivel DB como última defensa
4. **`NUMERIC(19,4)`** — tipo exacto para aritmética decimal (MongoDB usa `Decimal128` pero
   es más complejo de configurar en Spring Data)
5. **UUID como PK** — generación distribuida sin secuencia central

**¿Cuándo cambiaría a otro motor?**
Si el sistema requiriera sharding geográfico (balance >1B cuentas), consideraría CockroachDB
(PostgreSQL-compatible, distribuido) antes que MongoDB. MongoDB no es apropiado para
transacciones multi-documento en contextos financieros de alta consistencia.

**Consecuencias:**
- Ganamos: ACID, bloqueos, constraints, tipos exactos
- Sacrificamos: escalabilidad horizontal trivial (mitigable con réplicas de lectura)

---

### ADR-002: Arquitectura Hexagonal sobre MVC tradicional

**Contexto:** El sistema necesita ser testeable de forma independiente por capas y
adaptable a cambios de infraestructura (motor de DB, framework web) sin tocar el dominio.

**Opciones consideradas:**
1. **MVC tradicional** (Controller → Service → Repository con `@Entity` en el dominio)
2. **Arquitectura Hexagonal** (dominio puro, adaptadores en infraestructura)

**Decisión:** Arquitectura Hexagonal.

**Razones:**
1. Las reglas de negocio son el activo más valioso — deben ser testeables sin Spring
2. Los repositorios son interfaces en el dominio → podemos cambiar de JPA a JDBC
   sin tocar casos de uso
3. Las pruebas unitarias son más rápidas y deterministas (sin contexto Spring)
4. La separación de puertos/adaptadores hace explícitas las dependencias

**¿Cómo agregar comisiones por retiro sin tocar código existente?**
Se añadiría un puerto de salida `FeeCalculationPort` en el dominio, una implementación
en infraestructura, y `WithdrawalService` inyectaría el puerto. Ningún código existente
se modifica (principio Open/Closed).

**¿Cómo integrar notificaciones SMS/email?**
Se definiría un puerto de salida `NotificationPort` en el dominio con el método
`notifyTransactionCompleted(Transaction tx)`. La implementación usaría un cliente SMS/email
y se inyectaría en los casos de uso relevantes. Los dos servicios no se acoplan — el
microservicio de cuentas solo conoce la interfaz `NotificationPort`, no el servicio de
notificaciones.

**Consecuencias:**
- Ganamos: testabilidad, flexibilidad, separación clara de responsabilidades
- Sacrificamos: algo más de boilerplate (adaptadores, mappers de dominio↔entidad JPA)

---

### ADR-003: Bloqueo Pesimista sobre Bloqueo Optimista

**Contexto:** Dos retiros simultáneos sobre la misma cuenta con saldo suficiente para uno
pero no para ambos. El sistema debe garantizar que el saldo nunca sea negativo.

**Opciones consideradas:**
1. **Bloqueo optimista** — `@Version` en la entidad, reintento ante `OptimisticLockException`
2. **Bloqueo pesimista** — `SELECT FOR UPDATE`, el segundo hilo espera al primero

**Decisión:** Bloqueo pesimista (`PESSIMISTIC_WRITE`).

**Razonamiento:**
```
Escenario con bloqueo optimista:
  T=0: Thread-A lee cuenta (saldo=100, version=1)
  T=0: Thread-B lee cuenta (saldo=100, version=1)
  T=1: Thread-A debita 100 → intenta commit con version=1
  T=1: Thread-B debita 100 → intenta commit con version=1
  T=2: Thread-A hace commit → version=2 ✓
  T=2: Thread-B falla → OptimisticLockException → reintenta
  T=3: Thread-B relee → saldo=0, version=2
  T=3: Thread-B valida → InsufficientFundsException (correcto)
```

El optimista funciona correctamente pero **requiere que el cliente reintente**. En banca:
- El cliente espera una respuesta definitiva sin reintentar
- El reintento implica reenviar el request HTTP completo → complejidad en el cliente
- La ventana de lock es muy corta (microsegundos de una operación DB)
- Los conflictos en retiros son frecuentes (múltiples dispositivos del mismo cliente)

El pesimista es más simple de razonar, no requiere reintentos y es correcto para este dominio.

**Consecuencias:**
- Ganamos: simpleza, correctitud garantizada, sin reintentos
- Sacrificamos: algo de throughput en alta concurrencia sobre la misma cuenta
  (mitigable con timeout en el lock)

---

### ADR-004: FK Auto-referencial DEFERRABLE INITIALLY DEFERRED en Transferencias

**Contexto:** La tabla `transactions` tiene una FK auto-referencial `related_tx_id → transactions(id)`
para vincular el débito y el crédito de cada transferencia. Dentro de un mismo `@Transactional`,
se insertan primero el débito (con `related_tx_id = creditId`) y luego el crédito. PostgreSQL
valida las FKs por defecto al momento de cada INSERT — no al commit.

**Problema:** Al insertar el débito, PostgreSQL verifica que `creditId` exista en `transactions.id`.
Pero el crédito aún no ha sido insertado → violación de FK → error 500.

**Opciones consideradas:**
1. **Eliminar la FK** — perder integridad referencial a nivel de DB
2. **Insertar crédito primero** — el crédito referenciaría al débito que tampoco existe → mismo problema
3. **Guardar ambos con `related_tx_id = NULL` y hacer UPDATE después** — dos roundtrips extra a la DB
4. **`DEFERRABLE INITIALLY DEFERRED`** — PostgreSQL valida la FK al COMMIT, no por INSERT

**Decisión:** `DEFERRABLE INITIALLY DEFERRED` (migración V2).

```sql
ALTER TABLE transactions
    DROP CONSTRAINT fk_transactions_related_tx;
ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_related_tx
        FOREIGN KEY (related_tx_id) REFERENCES transactions(id)
        DEFERRABLE INITIALLY DEFERRED;
```

**¿Por qué es correcto?**
- La FK sigue existiendo — la integridad referencial se garantiza al COMMIT
- Al momento del COMMIT, ambas filas (débito y crédito) ya están presentes → constraint satisfecho
- `@Transactional` garantiza que o persisten ambas o ninguna — el DEFERRED se alinea perfectamente
  con la semántica transaccional de Spring

**Consecuencias:**
- Ganamos: FK real en el esquema + transferencias atómicas sin workarounds
- Sacrificamos: la verificación fila por fila (que en este caso era incorrecta de todas formas)
- Nota: `DEFERRABLE` solo funciona dentro de una transacción explícita. En autocommit, se verifica igual al INSERT.

---

## Supuestos Documentados

1. **Moneda única:** El sistema soporta solo USD. Una cuenta multi-moneda requeriría
   un tipo de cambio auditado y una tabla adicional de tasas.

2. **Un solo tenant:** El sistema no implementa multi-tenancy. En producción, se agregaría
   `tenant_id` a todas las tablas.

3. **Generación de número de cuenta:** Se usa UUID truncado con prefijo `ACC-`. En
   producción, se usaría una secuencia PostgreSQL (`SEQUENCE`) para mayor eficiencia
   y números más amigables para el usuario.

4. **Historial paginado:** Usa `OFFSET`. Para tablas >10M filas, se documentaría la
   migración a keyset pagination como deuda técnica conocida.

5. **El `customer_id` es un UUID externo:** Se asume que existe un microservicio de
   clientes. Este servicio no valida la existencia del cliente.
