# Searchicton

Welcome to Searchicton! The landmark searching mobile game for Fredericton, New Brunswick!

This is an android app and it utilizes the Google Maps API to show different landmarks in and around Fredericton! We have the locations stored on an external server, which is then downloaded and saved onto the app's local database upon startup.

We wanted to make this app to allow users to explore Fredericton in a fun way. Users can move around Fredericton using location services to play the game.
A location-based landmark app does not currently exist for Fredericton specifically. We found a few scavenger hunt apps, but they required users to create their own, without any predetermined locations. Although we take some inspiration from Pokemon Go for the game function, we are not directly improving any existing apps.

Using the app:
- A Google Maps API Key** is needed. In this case, the one coded in should work universally.
- Wi-Fi/Cellular Data is required upon initial startup to pull from the external database.
- Subsequent app startups only require GPS/Cell Service to be enabled to utilize the app to its full potential.
**Please note this project is private for the sake of protection the security of the API key. If it was public, the API key could be abused.
  
Installation:
- Clone the project into android studio locally.
- Run the app.
- If running on an emulator, Google Maps may need to be opened first if Searchicton cannot open the MapsActivity. Opening Google Maps initializes the emulator's GPS if this issue occurs.
- If running on a physical device, the device should be able to function normally with no extra steps.