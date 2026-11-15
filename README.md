# Kedai Makan Kai

An Android app (Java) for booking tables at Kedai Makan Kai. Customers book tables and track them. Admins review bookings and approve or reject them. The app talks to a REST backend through Retrofit.

## Tech Stack

| Item | Value |
|---|---|
| Language | Java 11 |
| Build | Gradle (Kotlin DSL), AGP 8.13.2, version catalog in `gradle/libs.versions.toml` |
| SDK | `minSdk 24`, `targetSdk 36`, `compileSdk 36` |
| UI | AppCompat, Material Components, ConstraintLayout, RecyclerView |
| Networking | Retrofit 2.11 + Gson converter, OkHttp logging interceptor 4.12 |
| Package | `com.example.kedaimakankai` |

## Build & Run

```bash
./gradlew assembleDebug        # build debug APK
./gradlew installDebug         # install on connected device/emulator
./gradlew test                 # unit tests
./gradlew connectedAndroidTest # instrumented tests
```

On Windows, use `gradlew.bat`. You can also open the project in Android Studio and run the `app` configuration.

The app needs the `INTERNET` permission, which is already declared in `AndroidManifest.xml`.

## Project Structure

```
app/src/main/java/com/example/kedaimakankai/
├── MainActivity.java              # Launcher: pick role (customer/admin) → Login or Register
├── LoginActivity.java             # Email + password login, routes by role
├── RegisterActivity.java          # Creates user, then a CUSTOMER record
├── CustomerDashboardActivity.java # Make booking / view my bookings / logout
├── AdminDashboardActivity.java    # View all bookings / manage (approve-reject) / logout
├── MakeBookingActivity.java       # Date + time pickers, pax, submits a BOOKING
├── ViewBookingsActivity.java      # Lists bookings (mode = "customer" | "admin"), cancel PENDING
├── BookingApprovalActivity.java   # Admin approve / reject (with reason)
├── ViewSlotsActivity.java         # Lists available slots (not linked from any screen yet)
├── SessionManager.java            # SharedPreferences session (token, role, ids)
├── adapter/                       # RecyclerView adapters: Booking, Approval, Slot
├── model/                         # Gson models (Booking, Customer, Status, User, ...)
└── remote/                        # ApiClient + Retrofit service interfaces
```

## App Flow

```
MainActivity ──► RegisterActivity ──► POST users/register ──► POST CUSTOMER
     │
     └─► LoginActivity ──► POST users/login
              ├─ role admin/superadmin ──► AdminDashboardActivity
              │       ├─ ViewBookingsActivity (mode=admin)
              │       └─ BookingApprovalActivity ──► POST STATUS (APPROVED / REJECTED + AdminNote)
              └─ customer ──► (resolve CustID from GET CUSTOMER) ──► CustomerDashboardActivity
                      ├─ MakeBookingActivity ──► POST BOOKING
                      └─ ViewBookingsActivity (mode=customer) ──► POST STATUS (CANCELLED)
```

## Backend API

The base URL is set in `remote/ApiClient.java`:

```
https://aptitude.my/2023622954/api/
```

`ApiClient` adds `Accept: application/json` to every request. Once the user is logged in, it also adds an `api-key: <token>` header, using the token saved by `SessionManager`. HTTP bodies are logged at `BODY` level.

| Service | Method & Path | Params | Used by |
|---|---|---|---|
| `AuthService` | `POST users/login` (form) | `email`, `password` | Login |
| `AuthService` | `POST users/register` (form) | `email`, `username`, `password` | Register |
| `CustomerService` | `GET CUSTOMER` | – | Resolve CustID after login/booking |
| `CustomerService` | `POST CUSTOMER` (form) | `UserID`, `CustName`, `CustPhone` | Register (plus fallbacks, see below) |
| `BookingService` | `GET BOOKING` | optional `CustID` | View bookings |
| `BookingService` | `POST BOOKING` (form) | `TimeSlot`, `Capacity`, `CustID` | Make booking |
| `StatusService` | `GET STATUS` | optional `BookID` | Latest status per booking |
| `StatusService` | `POST STATUS` (form) | `BookingStatus`, `BookID`, optional `AdminNote` | Approve / reject / cancel |
| `SlotService` | `GET slots/list_available` | – | ViewSlotsActivity |
| `ApprovalService` | `GET bookings/list_pending`, `POST bookings/approve`, `POST bookings/reject` | `BookingID` | Not used yet |

### Data Model Notes

- **Booking status is stored in its own `STATUS` table and is append-only.** A booking's current status is its most recent `STATUS` row: the latest `StatusTime` wins, and if two rows have the same time, the higher `StatusID` wins. A booking with no status row is shown as `PENDING`.
- Possible statuses are `PENDING`, `APPROVED`, `REJECTED` and `CANCELLED`. A customer can only cancel a booking while it is `PENDING`.
- Admins choose one of these rejection reasons, which is saved as `AdminNote`: *Slot Unavailable*, *Capacity Exceeded* or *Booking Overlap*.
- `TimeSlot` is sent in the format `yyyy-MM-dd HH:mm:ss`.
- `Booking.CustID` and `Customer.UserID` come back as **nested objects**, not plain ints. `Status.BookID` can come back as an int, a string or an object, and `Status.getBookId()` handles all three.
- Bookings must use `CUSTOMER.CustID`, **not** `users.id`. After login, the app finds the customer's `CustID` by matching `UserID.id`, then `AccID.AccEmail`, then `UserID.email`, and stores it in the session.

## Session

`SessionManager` keeps the session in the SharedPreferences file `kai_session`, with these keys: `token`, `role`, `user_id`, `username`, `email` and `cust_id`. Logging out clears all of them and returns the user to `MainActivity`.

## Known Issues / TODO

- **Customer record creation after registration is unreliable.** `RegisterActivity` tries several request shapes one after another: form `POST CUSTOMER`, camelCase fields, a JSON body, then `POST customers`. See [Troubleshooting: Customer Record Not Created](#troubleshooting-customer-record-not-created) below.
- No screen opens `ViewSlotsActivity`, and `ApprovalService` isn't used anywhere. Approvals currently work by posting to `STATUS`.
- The base URL is hard-coded. Consider moving it to `BuildConfig` or `gradle.properties`.
- OkHttp `BODY` logging is always on, including in release builds. It should be restricted to debug builds.

## Troubleshooting: Customer Record Not Created

**Symptom:** a user registers successfully, but no row is saved in the `CUSTOMER` table. As a result, the app can't find a `CustID` and bookings fail.

### Likely Causes

1. **Auth required.** `POST CUSTOMER` may require the `api-key` header. Right after registration the user has no token yet, so the header is missing.
2. **Backend not saving.** The endpoint may return success without actually inserting the row.
3. **Database constraints.** Check for other `NOT NULL` columns in `CUSTOMER`, a `UserID` foreign key that the insert doesn't satisfy, and triggers or validation that block the insert.
4. **Request format.** The app sends form-urlencoded `UserID`, `CustName` and `CustPhone`. Check whether the backend expects JSON or different field names, such as `userId` instead of `UserID`.

### How to Debug

1. **Logcat:** filter by `RegisterActivity` and look for `Sending POST /CUSTOMER with: ...`, `Customer creation response - HTTP: ...` and any errors.
2. **Backend logs:** check whether `POST CUSTOMER` reaches the server and which status code it returns.
3. **Database:** after registering, check by hand whether the customer row exists.

### Recommended Fixes

1. **Best option: create the customer inside `users/register`, in the same transaction as the user:**
   ```java
   @POST("users/register")
   public SimpleResponse register(@RequestParam String email,
                                  @RequestParam String username,
                                  @RequestParam String password,
                                  @RequestParam String custName,
                                  @RequestParam String custPhone) {
       User user = userService.createUser(email, username, password);
       customerService.createCustomer(user.getId(), custName, custPhone);
       return new SimpleResponse(true, "Registration successful", user.getId());
   }
   ```
2. Allow unauthenticated `POST CUSTOMER` during registration.
3. Return a temporary token from `users/register` and use it to create the customer.
