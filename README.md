# Senbird Desk - QuickStart for Android

## introduction
SendBird Desk is a chat customer service platform built on SendBird SDK and API.

Desk Android SDK provides customer-side integration on your own application, so you can easily implement a ticketing system with chat inquiry, inquiries inbox with UI theming. 

This repo was made to share a barebones quickstart implementation of how to use SendBird Desk.

It goes through the steps of:
- Connecting to SendBird
- Connecting to SendBird Desk
- Creating a Ticket
- Retrieving Closed Tickets

## Table of Contents

  1. [Create a Sendbird application](#create-a-sendbird-application)
  1. [Installation](#installation)
  1. [Initialization](#initialization)
  1. [Authentication](#authentication)
  1. [Creating a new ticket](#creating-a-new-ticket)
  1. [Loading ticket list](#loading-ticket-list)
  1. [Confirming end of chat](#confirming-end-of-chat)

## Prerequisites
- Android 4.0 or later and SendBird Android SDK 3.0.55 or later

## Create a Sendbird application
  1. Login or Sign-up for an account at [dashboard](https://dashboard.sendbird.com/)
  1. Create or select an application on the SendBird Dashboard.
  1. Note the `Application ID` for future reference.
  1. [Contact sales](https://sendbird.com/contact-sales) to get the `Desk` menu enabled in the dashboard. 
  1. Sendbird Desk is available only for free-trial or Enterprise plan 
  
## Installation

Installing the Desk SDK is a straightforward process if you're familiar with using external libraries or SDKs in your projects.
To install the Desk SDK using Gradle, add the following lines to your project-level `build.gradle` file.
```gradle
repositories {
    maven { url "https://raw.githubusercontent.com/sendbird/SendBird-SDK-Android/master/" }
    maven { url "https://raw.githubusercontent.com/sendbird/SendBird-Desk-SDK-Android/master/" }
}
```

And then add the following lines to your app-level `build.gradle` file.
```gradle
dependencies {
    implementation 'com.sendbird.sdk:sendbird-android-sdk:3.0.136'
    implementation 'com.sendbird.sdk:sendbird-desk-android-sdk:1.0.7'
}
```

## Initialization

Invoke `SendBird.init()` with your SendBird App ID just like when you initialize SendBird SDK and then
call `SendBirdDesk.init()` to use SendBird Desk SDK's features. Please be sure to initialize SendBird SDK before SendBirdDesk SDK.
```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SendBird.init(APP_ID, this);
        SendBirdDesk.init();
    }
}
```

> Calling `SendBird.init()` and `SendBirdDesk.init()` on `Application.onCreate()` is highly recommended.

> Even you use SendBird Desk SDK, you have to handle chat messages thru SendBird SDK. SendBird Desk SDK provides add-on features like chat ticket creation and loading chat tickets.
Ticket is the concept that does not exist on SendBird SDK and newly introduced on SendBird Desk SDK to support customer service ticketing system.
Every ticket created will be assigned to the appropriate agents and it will have a mapping channel of SendBird SDK, so you can implement real-time messaging on tickets with SendBird SDK.
> While using SendBird Desk SDK, it is also possible that you implement your own chat service using SendBird SDK.
For example, if you are operating an on-demand service, you can add an in-app messenger (for your platform users) as well as customer service chat (between users and agents)
into your application or website by combination of SendBird SDK and SendBird Desk SDK.


## Authentication

After initialization, connecting to SendBird's server by SendBird SDK is required for real-time messaging. 
This part is fully described on [SendBird SDK guide docs](https://docs.sendbird.com/android#authentication_2_authentication).
Authentication of SendBird Desk `SendBirdDesk.authenticate()` is also a mandatory for you to use ticket related features.
Below is an example for SendBird SDK connection and SendBird Desk SDK authentication.
```java
SendBird.connect(userId, accessToken, new SendBird.ConnectHandler() {
    @Override
    public void onConnected(User user, SendBirdException e) {
        if (e != null) {
            // Error handling.
            return;
        }

        // Use the same user Id and access token used on SendBird.connect.
        SendBirdDesk.authenticate(userId, accessToken, new SendBirdDesk.AuthenticateHandler() {
            @Override
            public void onResult(SendBirdException e) {
                if (e != null) {
                    // Error handling.
                    return;
                }
                
                // Now you can create a ticket, get open ticket count and load tickets.
            }
        });
    }
});
```
  
Now your customers are ready to create chat tickets and start inquiry with your agents!

## Creating a new ticket

Creating a new ticket is as simple as just calling `Ticket.create()`. Ticket title and user name can be passed at the same time.
The returned ticket will have a channel instance which can be accessed by `ticket.getChannel()`. So you can send messages to the channel using SendBird SDK.
For more detail of sending messages to channel, please refer to [SendBird SDK guide docs](https://docs.sendbird.com/android#group_channel_3_sending_messages).
Please notice that only after customers sending at least one message to the ticket, the ticket will be routed to the online agents so they can answer it.
```java
Ticket.create(ticketTitle, userName, new Ticket.CreateHandler() {
    @Override
    public void onResult(Ticket ticket, SendBirdException e) {
        if (e != null) {
            // Error handling.
           return;
        }
        // Now you can send messages to the ticket by ticket.getChannel().sendUserMessage() or sendFileMessage().
    }
});
```
> `Ticket.create()` has a overloaded method with `Priority` parameters so you can set the priority of ticket as well.
```java
Ticket.create(ticketTitle, userName, Priority priority, new Ticket.CreateHandler() {
    @Override
    public void onResult(Ticket ticket, SendBirdException e) {
        if (e != null) {
            // Error handling.
           return;
        }
        // Now you can send messages to the ticket by ticket.getChannel().sendUserMessage() or sendFileMessage().
    }
});
```
> `Ticket.create()` has a overloaded method with `groupKey` and `customField` parameters. The values could be evaluated when a ticket is created though it's used only in Dashboard currently. `groupKey` is the key of an agent group so that the ticket is assigned to the agents in that group. `customField` holds customizable data for the individual ticket.
```java
HashMap<String, String> customFields = new HashMap<>();
customFields.put("text", "hello");
customFields.put("number", "14");
customFields.put("select", "option2");

Ticket.create(ticketTitle, userName,
        "cs-team-1",    // groupKey
        customFields,   // customFields
        new Ticket.CreateHandler() {
    @Override
    public void onResult(Ticket ticket, SendBirdException e) {
        if (e != null) {
            // Error handling.
            return;
        }
        // Ticket is created with groupKey "cs-team-1" and customFields.
    }
});
```
> Each key in `customFields` should be preregistered in Dashboard. Otherwise, the key would be ignored.


## Loading ticket list
Usually you will design `Inbox` activity for open tickets and closed tickets history for your customer.
Open tickets and closed tickets can be loaded from `Ticket.getOpenedList()` and `Ticket.getClosedList()`.
Zero is a good start value of the offset, then the maximum 10 tickets will be returned for each call by last message creation time descending order.
Open ticket list and closed ticket list can be loaded like below:
```java
Ticket.getOpenedList(offset, new Ticket.GetOpenedListHandler() {
    @Override
    public void onResult(List<Ticket> tickets, boolean hasNext, SendBirdException e) {
        if (e != null) {
            // Error handling.
            return;
        }

        // offset += tickets.size(); for the next tickets.
        // This is the best place you display tickets on inbox. 
    }
});
```

```java
Ticket.getClosedList(offset, new Ticket.GetClosedListHandler() {
    @Override
    public void onResult(List<Ticket> tickets, boolean hasNext, SendBirdException e) {
        if (e != null) {
            // Error handling.
            return;
        }
        
        // offset += tickets.size(); for the next tickets.
        // This is the best place you display tickets on inbox. 
    }
});
```
> `Ticket.getOpenedList()` and `Ticket.getClosedList()` have overloaded methods with `customFieldFilter` parameter. Once you set `customField` to tickets, you can put `customFieldFilter` to `getOpenedList()` and `getClosedList()` in order to filter the tickets by `customField` values.

## Confirming end of chat
There are predefined rich messages on SendBird Desk and `Confirm end of chat` is one of them. For other rich messages, please refer to [Handling messages](#handling-messages).
All rich messages have message custom type (can be accessed by `UserMessage.getCustomType()` on SendBird SDK) as `SENDBIRD_DESK_RICH_MESSAGE`,
and `Confirm end of chat` message has custom data (can be accessed by `UserMessage.getData()` on SendBird SDK) as below:
```js
{
    "type": "SENDBIRD_DESK_INQUIRE_TICKET_CLOSURE",
    "body": {
        "state": "WAITING" // also can have "CONFIRMED", "DECLINED"
    }
}
```
This `Confirm end of chat` massage is initiated from agents to inquire closure of ticket to customers.
The initial `state` will be `WAITING` and you have to implement of updating the `state` according to customers action.
Usually, you can display `YES` or `NO` button like sample and update to `CONFIRMED` when customers touch `YES` button. Updating to `DECLINED` is also possible when customers touch `NO`.
For update the `state` of `Confirm end of chat`, please use `ticket.confirmEndOfChat()`.
```java
ticket.confirmEndOfChat(userMessage, confirm_or_decline, new Ticket.ConfirmEndOfChatHandler() {
    @Override
    public void onResult(Ticket ticket, SendBirdException e) {
        if (e != null) {
            // Error handling.
            return;
        }
        
        // You can update message UI like hiding YES NO buttons.
    }
});
```
At the moment, tickets will be closed (ticket close event will be sent to customers) only after customers confirming end of chat,  

## Reference
Please see the following link for Android Desk SDK Documentation https://github.com/sendbird/SendBird-Desk-SDK-Android
