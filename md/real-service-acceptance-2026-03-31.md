# Real Service Acceptance Report

Date: 2026-03-31

## Summary

- Frontend real-backend build verification: passed
- Backend real-service startup on default database `lab_booking`: failed
- Backend real-service startup on isolated acceptance database `lab_booking_acceptance`: passed
- Real authenticated API verification: passed
- Core reservation -> approval -> pickup -> return flow: passed

## Environment

- Backend: `http://127.0.0.1:8081`
- Frontend real env used for build:
  - `VITE_ENABLE_MOCK=false`
  - `VITE_API_BASE=http://127.0.0.1:8081`
- Acceptance database: `lab_booking_acceptance`

## Important Finding

The default local database `lab_booking` is not aligned with the current backend schema.

Observed blocker:

- `sys_user` in `lab_booking` is still an old structure
- backend startup logic expects the new schema with fields such as `login_id`
- startup failed in `DatabaseBootstrap` when trying to upgrade `lab_device`
- concrete error from runtime log:
  - `Table 'lab_booking.lab_device' doesn't exist`

Because of that, real acceptance could not be completed safely against the existing `lab_booking` database.

To avoid touching existing local data, acceptance was executed against a separate clean database:

- `lab_booking_acceptance`

## Seed Accounts Verified

- `SA001 / 000000` -> `SUPER_ADMIN`
- `A001 / 000000` -> `ADMIN`
- `T2026001 / 000000` -> `TEACHER`
- `20230001 / 000000` -> `STUDENT`

## Acceptance Results

### 1. Health Check

- `GET /api/health`
- Result: passed
- Response status in payload: `UP`

### 2. Super Admin Login and Basic Data

- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/devices?pageNum=1&pageSize=5`
- `GET /api/users?pageNum=1&pageSize=5`
- Result: passed

Verified:

- login returned token successfully
- `/api/auth/me` returned:
  - `userId=1`
  - `name=System Admin`
  - `roleCode=SUPER_ADMIN`
- device list returned `total=3`
- user list returned `total=4`

### 3. Permission Boundary Verification

Using student account `20230001 / 000000`:

- `GET /api/users?pageNum=1&pageSize=5`
- `GET /api/statistics/overview`

Result: passed

Verified:

- both endpoints returned `403`
- permission denial behavior is working on the real backend

### 4. Core Business Chain

Flow executed:

1. student created reservation for device `1001`
2. admin approved reservation
3. student queried borrow records
4. student confirmed pickup
5. student confirmed return

Real API sequence:

- `POST /api/reservations`
- `PUT /api/reservations/{id}/approve`
- `GET /api/borrow-records?pageNum=1&pageSize=10`
- `PUT /api/borrow-records/{id}/pickup`
- `PUT /api/borrow-records/{id}/return`

Result: passed

Verified statuses:

- reservation after approval: `PICKUP_PENDING`
- borrow record created: `recordId=3001`
- pickup result: `BORROWING`
- return result: `RETURNED`
- returned device condition: `NORMAL`

### 5. Frontend Real Configuration Verification

Command validated:

- frontend build with:
  - `VITE_ENABLE_MOCK=false`
  - `VITE_API_BASE=http://127.0.0.1:8081`

Result: passed

This confirms the frontend can be built in real-backend mode without relying on mock APIs.

## Final Conclusion

The project has passed real-service acceptance under a clean database environment.

At the same time, the default local database environment is not currently ready for direct acceptance because its schema is stale relative to the backend code.

## Recommended Next Step

- either migrate or rebuild the default `lab_booking` database to the current schema
- then repeat the same acceptance flow against the default environment
- after that, switch the frontend local runtime to:
  - `VITE_ENABLE_MOCK=false`
  - `VITE_API_BASE=http://127.0.0.1:8081`
