# Default Environment Manual Acceptance Checklist

Date: 2026-03-31

## Runtime Setup

- Backend database: `lab_booking`
- Backend URL: `http://127.0.0.1:8081`
- Frontend dev URL: `http://127.0.0.1:5173`
- Frontend env:
  - `VITE_API_BASE=/api`
  - `VITE_ENABLE_MOCK=false`
- Vite proxy target:
  - `/api -> http://localhost:8081`

## Built-in Accounts

- Super admin: `SA001 / 000000`
- Admin: `A001 / 000000`
- Teacher: `T2026001 / 000000`
- Student: `20230001 / 000000`

## Startup Order

1. Start backend in `admin-server`
2. Wait for `GET /api/health` to return `UP`
3. Start frontend in `admin-ui`
4. Open `http://127.0.0.1:5173`

## Core Checks

### A. Login

- Open login page
- Use `SA001 / 000000`
- Verify login succeeds
- Verify dashboard loads

### B. Role-based Navigation

- Log in as `SA001`
- Verify user, role, device, reservation, borrow, repair, statistics, menu pages are visible
- Log out
- Log in as `20230001`
- Verify restricted management menus are hidden or inaccessible

### C. Device Management

- Log in as `A001`
- Open device list
- Verify seeded devices are visible
- Open one device detail
- Create a new device
- Edit the new device
- Change the device status

### D. Reservation and Borrow Flow

- Log in as `20230001`
- Create a reservation for available device `1001`
- Log out
- Log in as `A001`
- Approve the reservation
- Log out
- Log in as `20230001`
- Open borrow records
- Confirm pickup
- Confirm return
- Verify final status is `RETURNED`

### E. User and Permission Boundaries

- Log in as `20230001`
- Try to open user management route directly
- Verify access is denied
- Try to open statistics route directly
- Verify access is denied

### F. Profile and Messages

- Log in as any built-in account
- Open personal center
- Verify profile summary loads
- Verify borrow records tab loads
- Verify messages tab loads
- Confirm one unread message if present

### G. Repair

- Log in as `20230001`
- Create a repair request
- Log out
- Log in as `A001`
- Update repair status

### H. Statistics

- Log in as `SA001` or `A001`
- Open statistics page
- Verify overview cards load
- Verify ranking sections load

## Verified Baseline

The following were already verified programmatically on 2026-03-31:

- backend on default `lab_booking` starts successfully
- `GET /api/health` returns `UP`
- `POST /api/auth/login` for `SA001 / 000000` succeeds
- student permission boundary returns `403` for restricted endpoints
- reservation -> approval -> pickup -> return core flow works on real backend

## Backup

Default database backup created before rebuild:

- `backups/lab_booking-20260331-211940.sql`
