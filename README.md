![](https://user-images.githubusercontent.com/19232017/181865348-d86ad356-0932-49d3-873e-653f3c8d39ac.gif)

<br>

<img align="right" width=135 src="https://user-images.githubusercontent.com/19232017/181865090-9492b7b3-80ce-4236-bd2c-dbd54571c1c3.png">

Welcome to Searchicton! The landmark-searching mobile game for Fredericton, New Brunswick!

This is an Android app that uses the Google Maps API to show different landmarks in and around Fredericton! The locations are stored on an external server, which are then cached by the app upon startup.

<br>

## Using the app
- Wi-Fi/Cellular Data is required upon initial startup to pull landmarks from the external database and to cache Google Maps data.
- The app requires that the user grants location permissions when prompted. The app will also prompt the user to turn on Location Services (if not running) and that the Location Mode is set to one that uses GPS; either High Accuracy mode (AKA Fused Location), or GPS-only mode. 
- Subsequent app startups only require location services, since the landmarks and Google Maps data will be cached.

<br>

## Installation
You're gonna have to compile it yourself - no API keys for you.
- Clone the project:
	```
	git clone https://github.com/mateimarica/searchicton
	```
- Import into Android Studio: Open Android Studio, go to File > Open > Select the "searchicton" folder > OK
- Replace the `YOUR_KEY_HERE` in `app/src/main/res/values/strings.xml` with a [Google Maps API key](https://developers.google.com/maps/documentation/javascript/get-api-key)
- Install the app on an emulator or physical device by clicking "Run app" or pressing Shift+F10
