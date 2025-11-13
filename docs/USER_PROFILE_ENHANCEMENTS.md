# User Profile Enhancements

## Summary
- Added optional `avatar_url` and `address` columns to the `users` table (Flyway migration `V4__add_avatar_and_address_to_users.sql`).
- Updated the `User` entity, DTOs, mapper, and service logic to expose and manage these fields.
- Extended user update API to accept avatar URL updates, optional removal flag, and address value without impacting existing frontend workflows.

## Database Migration
```sql
ALTER TABLE users
    ADD COLUMN avatar_url VARCHAR(255) NULL AFTER email,
    ADD COLUMN address VARCHAR(255) NULL AFTER avatar_url;
```
- Migration file: `src/main/resources/db/migration/V4__add_avatar_and_address_to_users.sql`.
- Run migrations via `./mvnw flyway:migrate` (Linux/macOS) or `mvnw.cmd flyway:migrate` (Windows) if needed outside application startup.

## Entity & DTO Updates
- `User` entity now includes `avatarUrl` and `address` fields mapped to the new columns.
- `UserResponseDTO` exposes both fields for clients consuming user details.
- `UserUpdateRequestDTO` accepts:
  - `avatarUrl` (optional, validated URL, max 255 chars).
  - `address` (optional, max 255 chars).
  - `removeAvatar` flag (`Boolean`) allowing clients to clear the stored avatar without sending a new URL.

## Service Logic Adjustments
- `UserService.updateUser` now handles:
  1. Trimming and persisting non-blank avatar URLs when provided.
  2. Clearing avatar when `removeAvatar` is `true` and no new URL is supplied.
  3. Trimming provided addresses and storing `null` when empty to avoid stale values.
- Existing role validation and other update rules remain unchanged to prevent regressions.

## API Usage Notes
- To set an avatar, first upload the image via `POST /api/v1/files/upload`, then supply the returned `fileUrl` in `avatarUrl` when calling `PUT /api/v1/users/{id}`.
- To remove an avatar, send `"removeAvatar": true` and omit `avatarUrl` (or set it to `null`/empty string).
- Address values can be cleared by sending an empty string.

## Testing Considerations
- Verify Flyway migrations run successfully in the target environment.
- Exercise the user update endpoint with combinations of avatar/address updates to confirm validation and trimming behave as expected.
- Ensure existing frontend flows continue to function; no API contract changes were made beyond the new optional fields.
  - Unit test: `PayrollControllerTest` required `JwtService` and `UserDetailsService` mocks to satisfy security filter wiring in the MVC slice test.
  - Unit test: `AttendanceServiceTest.resolveAssignment_ShouldUseCurrentUserWhenAssignmentIdMissing` adjusted to avoid unnecessary Mockito stubbing while still covering dynamic assignment resolution.
