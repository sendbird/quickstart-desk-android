# Sendbird Desk for Android sample
![Platform](https://img.shields.io/badge/platform-ANDROID-orange.svg)
![Languages](https://img.shields.io/badge/language-JAVA-orange.svg)

## Introduction

Built with Sendbird Chat platform, Sendbird Desk is a live chat customer support that offers customer satisfaction through enhanced engagement. Through its integration, Desk Android SDK enables you to easily customize your ticketing support system with a UI theme, thereby elevating your overall customers’ experience. For example, you can modify the inbox - a management tool and storage unit for the agents’ and tickets’ conversations - to fit within your color scheme and layout.

<br />

## Before getting started

This section provides the pre-installation steps for Sendbird Desk for Android sample app. 

### Requirements

- Android 4.0 or higher
- Chat SDK for Android 3.0.55 or higher

### Try the sample app applied with your data 

If you would like to customize the sample app for your usage, you can replace the default sample app ID with your ID - which you can obtain by c[creating your Sendbird application from the dashboard](https://docs.sendbird.com/android/quick_start#3_install_and_configure_the_chat_sdk_4_step_1_create_a_sendbird_application_from_your_dashboard).

> Note: After creating the Sendbird application, please be sure to contact [sales](https://get.sendbird.com/talk-to-sales.html) to enable the Desk menu onto the dashboard. Currently, Sendbird Desk is available only for free-trial or Enterprise plans.

Following the previous instructions will allow you to experience the sample app with your data from the Sendbird application.

<br />

## Getting started

If you're familiar with external libraries or SDKs, installing the Desk SDK is an easy and straightforward process. 

### Create a project

Create a project to get started. Currently Sendbird Desk only supports `Java`.

### Install Desk for Android

You can install Desk for Android as follows: 

#### Gradle

Add the following lines to your project-level `build.gradle` file.

```gradle
repositories {
    maven { url "https://raw.githubusercontent.com/sendbird/SendBird-SDK-Android/master/" }
    maven { url "https://raw.githubusercontent.com/sendbird/SendBird-Desk-SDK-Android/master/" }
}
```

And then add the following lines to your app-level `build.gradle` file.
```gradle
dependencies {
    implementation 'com.sendbird.sdk:sendbird-android-sdk:3.0.141'
    implementation 'com.sendbird.sdk:sendbird-desk-android-sdk:1.0.8'
}
```

<br />

## For further reference

Please visit the following link to learn more about Desk SDK for Android: https://github.com/sendbird/SendBird-Desk-SDK-Android
