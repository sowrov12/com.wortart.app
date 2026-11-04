# Firebase setup for the Members tab

Everything below works from a phone browser. The app builds and runs without these steps;
the Members tab then says "Accounts are not set up yet".

## 1. Create the project
1. Open https://console.firebase.google.com and tap **Add project** (name: WortArt).
2. Google Analytics is not needed: switch it off.

## 2. Register the Android app
1. In the project, tap the Android icon (**Add app**).
2. **Package name:** `com.wortart.app`
3. **Debug signing certificate SHA-1:** `4E:42:C0:05:EC:AF:FA:AD:BB:38:65:A6:8B:AC:13:AE:5B:22:5A:91`
   (this is the fingerprint of `app/debug.keystore`; every CI build is signed with it)
4. Register the app, then **download `google-services.json`**.

## 3. Turn on Google sign-in
1. **Build > Authentication > Get started > Sign-in method > Google > Enable**.
2. Choose a support email and save.
3. Download `google-services.json` again afterwards (Project settings > Your apps). It must
   now contain an `oauth_client` entry of type 3 (the web client ID). The older file does not.

## 4. Create the database
1. **Build > Firestore Database > Create database**, location near your users, **production mode**.
2. Open the **Rules** tab, paste the content of `firebase/firestore.rules`, and **Publish**.

## 5. Add the files to GitHub
- Upload `google-services.json` to the `app/` folder (path `app/google-services.json`).
- Upload `app/debug.keystore` if it is not in the repository yet.
- The next **Android Build** run picks both up automatically.

## 6. Make yourself the first admin
Admin rights cannot be set from the app (the rules forbid it). Do it once by hand:
1. Install the app, open **Members**, sign in with Google.
2. In Firebase console > Firestore Database > `users` > your document, set `isAdmin` to `true`.
3. Reopen the Members tab: the **Admin panel** button appears.

## 7. How invitations work
1. A member shares the invite code (Members tab > Invite friends).
2. A friend signs in, enters the code under "Have an invite code?" and taps **Apply**. This creates
   `invitations/{friendUid}` with status `pending`.
3. An admin opens **Members > Admin panel**, checks the invitation and taps **Approve**
   (friend +7 days, inviter +5 days and +1 referral) or **Reject**.
4. The friend's Members tab shows the result. After a rejection they can try another code.

The Admin panel also has a **User ID or email** search to enable or extend Premium by hand
(+7, +30 or +365 days) or to remove it.

After updating the app, publish the latest `firebase/firestore.rules` again: the invitation rules
changed (a friend may update only the "cards answered" number of a pending invitation).

## Notes
- `app/debug.keystore` is only for debug builds. The release key (Task 4.3) is separate and
  must never be committed.
- If Google sign-in shows "developer error", the SHA-1 in step 2 or the web client ID in step 3 is
  missing. Re-download `google-services.json` after fixing it.
- Deleting an account in the app deletes the `users/{uid}` and `referralCodes/{code}` documents
  and the Firebase Auth account. Check this against the Google Play "Data deletion" form.
