# GymTracker 🏋️

An Android app (Java) for tracking my personal workout program, with user accounts and cloud storage.
Available in **English** and **Greek** (switchable inside the app).

## Features
- **Accounts:** sign up, sign in, "Keep me signed in", sign out, password reset by email (Firebase Authentication)
- **Program:** Monday–Sunday tabs; exercises with sets, reps, weight and a note; a note for each day
- **Cardio:** running, cycling, etc. with distance, time, pace (min/km) and date
- **Goals:** e.g. "Bench press 70 kg in 2 weeks" or "Running 5 km in 25:00", with progress calculated automatically
- **Progress charts:** weight, distance and pace over time, with the goal shown as a dashed line (MPAndroidChart)
- **Works offline:** Firestore syncs when the connection comes back
- **Languages:** English (default) and Greek, with an in-app language switch
- Material Design 3, light and dark theme

## Tech stack
Java · Android SDK (minSdk 26) · Firebase Authentication · Cloud Firestore · Material Components · MPAndroidChart

## Project structure
```
com.vasilis.gymtracker
├── auth/            – LoginActivity, SignupActivity, SessionManager, AuthErrors
├── data/            – FirestoreRepository (all database access)
├── model/           – Exercise, StrengthLog, CardioSession, Goal
├── ui/program/      – ProgramFragment, DayFragment, adapters
├── ui/cardio/       – CardioFragment, CardioAdapter
├── ui/goals/        – GoalsFragment, GoalAdapter, GoalProgress
├── util/            – DateUtils, Formatters, CardioTypes, LanguageHelper, UiUtils
├── MainActivity     – bottom navigation + menu
└── ProgressActivity – progress charts
```

## Database structure (Firestore)
```
users/{uid}
  ├── exercises/      program exercises
  ├── strengthLogs/   weight history
  ├── cardio/         cardio workouts
  ├── goals/          goals
  └── dayNotes/       day notes
```
The security rules (`firestore.rules`) only let each user access their own data.

## Setup
`google-services.json` is not included in the repository. To run the project you need your own Firebase project with:
1. An Android app with the package name `com.vasilis.gymtracker` (put its `google-services.json` in `app/`)
2. **Authentication** → Email/Password enabled
3. **Cloud Firestore** created, with the rules from `firestore.rules`
