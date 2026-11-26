# ShaadiSwap

Android app (Java) to buy and sell pre-owned wedding and occasion outfits in Delhi NCR.
Buyers contact sellers on WhatsApp. English and Hindi. Free tier: 2 listings per user.

See `PROJECT_PLAN.md` for the roadmap. This code covers steps 1-9 (setup, login, feed,
detail + WhatsApp, add listing, my listings, report, favourites, release prep).

## What you need to do once (about 20 minutes)

1. Install **Android Studio** and open this folder.
2. In the [Firebase console](https://console.firebase.google.com) create a project, then:
   - Add an Android app with package name `com.shaadiswap.app`.
   - Add your debug **SHA-1 and SHA-256** (Android Studio: Gradle > app > Tasks > android > signingReport). Phone login does not work without this.
   - Download `google-services.json` and put it in the `app/` folder (it is git-ignored on purpose).
   - Enable **Authentication > Sign-in method > Phone**.
   - Create a **Firestore** database and paste `firestore.rules` into its Rules tab.
   - Enable **Storage** and paste `storage.rules` into its Rules tab.
   - Storage and phone auth may require the Blaze (pay as you go) plan. Free usage limits are generous at this size.
3. Optionally add test phone numbers in Firebase Auth to avoid SMS during development.
4. Press Run.

## Moderation (admin)

There is no admin screen yet. Use the Firebase console:
- Reported listings appear in the `reports` collection.
- To take a listing down, set its `status` field to `removed`. It disappears from the app.

## Before releasing on Google Play

- Use a release signing key and add its SHA-1 to Firebase.
- Add a privacy policy URL (the app stores phone number, name, city and photos).
- Play requires a recent target SDK. This project targets 35. Raise it if Play asks.
- Test on 3-4 real phones, then use an internal / closed test track first.

## Not built yet

- Push notifications (needs Firebase Cloud Functions)
- Paid listings after 2 free ones (needs Play Billing or a payment gateway, plus GST/business setup)
- Admin screen, in-app chat, seller ratings, iOS app

## Notes

- Seller WhatsApp numbers are visible to signed-in users on listings, by design.
- The free-listing limit is checked in the app only. A determined user could bypass it; enforce it
  server-side with Cloud Functions when you add paid listings.
- The feed loads the latest 200 listings and filters on the device. Move to server-side queries
  and paging when you grow past that.
- This code was written without an Android SDK available, so it has not been compiled or run yet.
  Expect a few small fixes on first build.
