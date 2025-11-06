# SmartHR FanControl

![App Icon](<https://github.com/chudzik28/SmartHR-FanControl/blob/master/assets/app_icon.png?raw=true>)

**Automate your Xiaomi Smart Fan based on your heart rate.**

SmartHR FanControl is a free, open-source Android application that connects to your Bluetooth heart rate sensor and dynamically controls the speed of your Xiaomi Smart Fan. It's the perfect companion for indoor training sessions on a bike trainer, treadmill, or any other workout, keeping you cool by adjusting the fan speed to your effort level.

---

## Features

*   **Automatic Fan Control:** Fan speed intelligently adapts to your real-time heart rate.
*   **Highly Customizable Algorithm:** Tailor the HR-to-speed curve to your personal fitness zones and preferences (Min/Max HR, Min/Max Speed, Exponent, Smoothing).
*   **Manual Mode:** Full manual control over the fan speed with a simple slider.
*   **BLE Heart Rate Sensor Support:** Connects to any standard Bluetooth Low Energy heart rate sensor.
*   **BLE Heart Rate Sharing:** Re-broadcasts your HR data, allowing you to connect your sensor to the app and another device (like a bike computer or Zwift) simultaneously.
*   **Secure & Private:** All your data (fan token, IP, device addresses) is stored securely and encrypted on your device. No data is ever collected or transmitted.
*   **Free & Open Source:** No ads, no subscriptions. The app is licensed under the GNU General Public License v3.0.

---

## Screenshots

| Main Screen | HR Sensors | Settings |
| :---: | :---: | :---: |
| ![Main Screen](<https://github.com/chudzik28/SmartHR-FanControl/blob/master/assets/screenshot_main.jpg?raw=true>) | ![HR Sensors Screen](<https://github.com/chudzik28/SmartHR-FanControl/blob/master/assets/screenshot_hrsensors1.jpg?raw=true>) | ![Settings Screen](<https://github.com/chudzik28/SmartHR-FanControl/blob/master/assets/screenshot_automode_1.jpg?raw=true>) |

---

## Setup Guide

Setting up the app involves three main steps:

### 1. Obtain Fan IP Address and Token

To control your Xiaomi fan, the app needs its local IP address and a unique 32-character token. A highly recommended and user-friendly tool for this is the **Xiaomi Cloud Tokens Extractor**. You can find it by searching online or visiting its GitHub page. Log in with your Xiaomi/Mi Home account, and the tool will display the IP and Token for all your devices.

### 2. Configure the Fan in the App

Navigate to the **Settings -> Fan Config** tab, enter the IP Address and Token, and tap **SAVE FAN SETTINGS**. The fan status on the Main screen should change to "Connected".

### 3. Pair and Select Your HR Sensor

Navigate to the **HR Sensors** tab, search for your device, **Pair** it, and then **Select** it to make it the active sensor.

---

## Acknowledgements

The core fan control logic in this application is a Kotlin reimplementation of the excellent work done by the developers of the open-source **[python-miio](https://github.com/rytilahti/python-miio)** library. This project would not have been possible without their efforts in reverse-engineering the Miio protocol.

---

## Support the Project

This is a hobby project developed in my free time. If you find this app useful, please consider supporting its development:

<a href="<https://buymeacoffee.com/chudzim>">
  <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;" >
</a>

---

## License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for the full license text.
