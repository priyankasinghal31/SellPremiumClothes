# SellPremiumClothes: Step-by-Step Plan

A simple Android app (Java) where people list pre-owned wedding and occasion outfits, and buyers contact sellers on WhatsApp. No inspection, delivery or payments in version 1.

When you are ready, tell me: "Do Step N" and I will build it.

## Before you start (about 2 weeks, no coding)
- [ ] Collect 20-30 outfits from friends and family and list them on an Instagram page and a WhatsApp group.
- [ ] Talk to 30-50 recent brides, bridesmaids and guests: would they sell or buy?
- [ ] Note real sales, time to sell and what people ask for.
- [ ] Check Google Play for similar apps (search: "sell lehenga", "pre-owned ethnic wear"). Check last update date, installs and 1-star reviews.
- [ ] Create a Google account for the project and a Google Play Developer account (one-time fee, about US$25).
- [ ] Decide the city to launch in.

## Build steps (Android, Java, Firebase)

### Step 1: Project setup
- Android Studio project in Java
- Connect to Firebase (Auth, Firestore, Storage)
- App name, icon placeholder, basic theme

### Step 2: Login
- Phone number OTP login (Firebase Auth)
- Simple profile: name, city, WhatsApp number

### Step 3: Home feed
- List of outfits with photo, price, size, city
- Search and filters: size, colour, price, city, occasion (lehenga, gown, saree, sherwani)

### Step 4: Item detail screen
- Photo gallery, description, condition, seller name and city
- "Chat on WhatsApp" button (opens WhatsApp with a prefilled message)

### Step 5: Add a listing
- Take or pick 4-6 photos (tag and flaws included)
- Fields: title, price, size, condition, occasion, city, original price (optional)
- Photos stored in Firebase Storage

### Step 6: My listings
- See, edit, mark as sold, delete

### Step 7: Safety features
- "Report listing" button
- Warning text: meet in a public place, do not pay in advance
- Simple admin approval or removal of reported listings (Firebase console at first)

### Step 8: Notifications and polish
- Push notification when someone favourites a listing
- Favourites/wishlist
- Empty states, loading states, error messages

### Step 9: Testing and release
- Test on 3-4 real phones
- Privacy policy and terms pages
- Play Store listing (screenshots, description)
- Internal test, then closed test with 20 users, then public release

## After launch (only if people use it)

### Step 10: Ads
- Add AdMob banner on the feed (small income at first)

### Step 11: Paid listings
- 2 free listings per user, then packs (for example Rs 109 for two, Rs 299 for two on high-value dresses)
- Check Google Play payment policy first (Play Billing vs Razorpay), and set up business, PAN, bank and GST details
- Backend check with Firebase Cloud Functions so payments cannot be faked

### Step 12: Featured listings and vendor promotions
- Sellers pay to appear at the top for 7 days
- Dry cleaners, tailors and wedding vendors pay to be promoted

### Step 13: Later ideas
- In-app chat, seller ratings, verified badge, iOS app, more cities

## Rough timeline (one Android developer)
- Steps 1-4: 2-3 weeks
- Steps 5-7: 2-3 weeks
- Steps 8-9: 2 weeks
- Total: about 6-10 weeks to a first release

## Decisions to confirm before Step 1
1. Launch city or country
2. App name and logo
3. Free listings per user (suggest 2)
4. Languages (English, Hindi)
