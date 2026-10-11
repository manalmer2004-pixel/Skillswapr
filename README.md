# SkillSwap Android App

SkillSwap is a community time-banking Android application built with Kotlin, Jetpack Compose, Coroutines/Flow, and Firebase Firestore.

## Firebase setup

1. **Publish Security Rules**:
   - Open the [Firebase Console](https://console.firebase.google.com/).
   - Navigate to **Build** $\rightarrow$ **Firestore Database** $\rightarrow$ **Rules**.
   - Copy the contents of `firestore.rules` and paste them into the online editor.
   - Click **Publish**.

2. **Create Composite Indexes**:
   - In **Firestore Database**, navigate to the **Indexes** tab.
   - Click **Add Index** and create the following composite indexes:
     - **Collection**: `transactions` | **Fields**: `userId` (ASC), `createdAt` (DESC)
     - **Collection**: `notifications` | **Fields**: `userId` (ASC), `createdAt` (DESC)

3. **Administrator & Category Seeding Setup**:
   - Register the first user account in the app.
   - Open **Firebase Console** $\rightarrow$ **Firestore Database** $\rightarrow$ `users` collection.
   - Select the document for your first user and manually set the field `isAdmin = true` (type: `boolean`).
   - Log in as that administrator in the app. Upon accessing categories, the default skill categories will automatically seed into the `categories` collection in Firestore.
