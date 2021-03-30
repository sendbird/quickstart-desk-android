# Sendbird Desk for Android sample
![Platform](https://img.shields.io/badge/platform-ANDROID-orange.svg)
![Languages](https://img.shields.io/badge/language-JAVA-orange.svg)

## Introduction

Built with Sendbird Chat platform, Sendbird Desk is a live chat customer support that offers customer satisfaction through enhanced engagement. Through its integration, Desk SDK for Android enables you to easily customize your ticketing support system with a UI theme, thereby elevating your overall customers’ experience. For example, you can modify the inbox - a management tool and storage unit for the agents’ and tickets’ conversations - to fit within your color scheme and layout.

### More about Sendbird Desk for Android

Find out more about Sendbird Desk for Android on [Desk for Android doc](https://sendbird.com/docs/desk/v1/android/getting-started/about-desk-sdk). If you need any help in resolving any issues or have questions, visit [our community](https://community.sendbird.com).

<br />

## Before getting started

This section shows you the prerequisites you need for testing Sendbird Desk for Android sample app.

### Requirements

- Android 4.0 or higher
- [Chat SDK for Android](https://github.com/sendbird/SendBird-SDK-Android/tree/master/com/sendbird/sdk/sendbird-android-sdk) 3.0.55 or higher

### Try the sample app using your data 

If you would like to customize the sample app for your usage, you can replace the default sample app ID with your ID - which you can obtain by [creating your Sendbird application from the dashboard](https://sendbird.com/docs/chat/v3/android/getting-started/install-chat-sdk#2-step-1-create-a-sendbird-application-from-your-dashboard).

> Note: After creating the Sendbird application, please be sure to contact [sales](https://get.sendbird.com/talk-to-sales.html) to enable the **Desk** menu onto the dashboard. Currently, Sendbird Desk is available only for **free-trial** or **Enterprise** plans.

Following the previous instructions will allow you to experience the sample app with your data from the Sendbird application.

<br />

## Getting started

This section explains how to install Desk SDK for Android before testing the sample app. If you're familiar with using external libraries or SDKs in your projects, installing the Desk SDK will be an easy and straightforward process. 

### Install Desk SDK for Android

Installing the Chat SDK is simple if you're familiar with using external libraries or SDKs. First, add the following code to your **root** `build.gradle` file:

```gradle
allprojects {
    repositories {
        ...
        maven { url "https://repo.sendbird.com/public/maven" }
    }
}
```

> **Note**: Make sure the above code block isn't added to your module `bundle.gradle` file.

Then, add the dependency to the project's top-level `build.gradle` file.

```gradle
dependencies {
    implementation 'com.sendbird.sdk:sendbird-android-sdk:3.0.160'
    implementation 'com.sendbird.sdk:sendbird-desk-android-sdk:1.0.12'
}
```

> **Note**: Desk SDK versions `1.0.12` or lower can be downloaded from JCenter until February 1, 2022. SDK versions higher than `1.0.12` will be available on Sendbird's remote repository.

## For further reference

Please visit the following link to learn more about Desk SDK for Android: https://github.com/sendbird/SendBird-Desk-SDK-Android
