# SmartHR FanControl

![App Icon](https://github.com/chudzik28/SmartHR-FanControl/blob/master/assets/app_icon.png?raw=true)

**Automate your Xiaomi Smart Fan based on your heart rate.**

SmartHR FanControl is a free, open-source Android application that connects to your Bluetooth heart rate sensor and dynamically controls the speed of your Xiaomi Smart Fan. It's the perfect companion for indoor training sessions on a bike trainer, treadmill, or any other workout, keeping you cool by adjusting the fan speed to your effort level.

---

## Get the App

<a href='<LINK_DO_APLIKACJI_W_GOOGLE_PLAY>'>
    <img alt='Get it on Google Play'
         src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'
         width='240'/>
</a>

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

## Supported Devices

The app is designed to be compatible with a range of Xiaomi Smart Fans. The following models should be supported:

*   Pedestal Fan P9 (`dmaker.fan.p9`)
*   Pedestal Fan P10 (`dmaker.fan.p10`)
*   Mija Pedestal Fan (`dmaker.fan.p11`)
*   Pedestal Fan P15 (`dmaker.fan.p15`)
*   Mi Smart Standing Fan 2 (`dmaker.fan.p18`)
*   Mi Smart Standing Fan 2 Pro (`dmaker.fan.p33`)

**Please Note:** While the app is designed to work with all the models listed above, it has only been physically tested on the **Mi Smart Standing Fan 2 Pro**. Functionality with other models is based on the protocol implementation from the `python-miio` library and is not guaranteed.

---

## Screenshots

| Main Screen | HR Sensors | Settings |
| :---: | :---: | :---: |
| ![Main Screen](https://github.com/chudzik28/SmartHR-FanControl/blob/master/assets/screenshot_main.jpg?raw=true) | ![HR Sensors Screen](https://github.com/chudzik28/SmartHR-FanControl/blob/master/assets/screenshot_hrsensors1.jpg?raw=true) | ![Settings Screen](https://github.com/chudzik28/SmartHR-FanControl/blob/master/assets/screenshot_automode_1.jpg?raw=true) |

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

[![Buy Me A Coffee](https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png)](https://buymeacoffee.com/chudzim)

---

## License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for the full license text.
