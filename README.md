# Searchicton

Welcome to Searchicton! The landmark searching mobile game for Fredericton, New Brunswick!

This is an android app and it utilizes the Google Maps API to show different landmarks in and around Fredericton! We have the locations stored on an external server, which is then downloaded and saved onto the app's local database upon startup.

We wanted to make this app to allow users to explore Fredericton in a fun way. Users can move around Fredericton using location services to play the game.
A location-based landmark app does not currently exist for Fredericton specifically. We found a few scavenger hunt apps, but they required users to create their own, without any predetermined locations. Although we take some inspiration from Pokemon Go for the game function, we are not directly improving any existing apps.

## Using the app:
- A Google Maps API key is required (We have one hard-coded in `strings.xml` but it will expire sometime in May)
- Wi-Fi/Cellular Data is required upon initial startup to pull landmarks from the external database and to cache Google Maps data.
- The app requires that the user grants location permissions when prompted. The app will also prompt the user to turn on Location Services (if not running) and that the Location Mode is set to one that uses GPS; either High Accuracy mode (AKA Fused Location), or GPS-only mode. 
- Subsequent app startups only require location services, since the landmarks and Google Maps data will be cached.

## Installation:
- Clone the project:
	```
	git clone https://github.com/mateimarica/searchicton
	```
- Import into Android Studio: Open Android Studio, go to File > Open > Select the "searchicton" directory you cloned > OK
- Install the app on an emulator or physical device by clicking "Run app" or pressing Shift+F10
	- If running on an emulator, Google Maps *may* need to be opened first if Searchicton cannot open the MapsActivity. Opening Google Maps initializes the emulator's GPS if this issue occurs.
	- If running on a physical device, the device should be able to function normally with no extra steps.
