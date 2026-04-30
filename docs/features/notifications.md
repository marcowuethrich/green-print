# Feature: Local Notifications

Automated reminders to ensure plants are watered on time.

## 1. Background Reminders
The app schedules reminders even when not in the foreground.

### User Stories
- As a user, I want to receive a notification on my phone when a plant needs watering.
- As a user, I want to be notified when a plant's hydration enters the "thirsty" zone.

### Technical Notes
- **Implementation**: Platform-specific (`AlarmManager` on Android, `UNUserNotificationCenter` on iOS).
- **Trigger**: Calculated based on the `next_due` timestamp.
- **Shared Logic**: The shared module provides the "Thirsty" threshold (e.g., 10% hydration remaining).
