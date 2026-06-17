# ♻️ RecycleGO

## Overview

RecycleGO is a community-driven mobile application designed to promote sustainable recycling practices and environmental awareness. The platform combines recycling services, community engagement, reward mechanisms, location-based services, and AI-powered assistance to encourage users to adopt greener lifestyles.

Built using Android Studio with Firebase integration, RecycleGO aims to bridge the gap between recycling awareness and actual participation by making recycling more accessible, interactive, and rewarding.

---

# Problem Statement

Malaysia continues to face increasing waste generation due to rapid urbanization and population growth. A significant portion of household waste consists of recyclable materials, yet much of it ends up in landfills due to low public participation, limited awareness, and inconvenience in accessing recycling services.

Existing recycling applications primarily focus on providing information or collection services but often lack:

* Community engagement features that motivate long-term participation.
* Educational tools that help users understand proper recycling practices.
* Convenient recycling pickup services.
* Reward mechanisms that encourage sustainable behavior.
* AI-powered assistance for recycling guidance.

As a result, many individuals remain unaware of proper recycling methods or lack motivation to participate consistently in recycling activities.

---

# Proposed Solution

RecycleGO provides a centralized platform that integrates recycling services, environmental education, community interaction, and reward-based incentives into a single mobile application.

The platform enables users to:

* Request recycling pickups from nearby recycling centers.
* Discover recycling centers through map navigation.
* Earn rewards and points for sustainable actions.
* Participate in environmental campaigns and events.
* Engage with communities that promote recycling awareness.
* Learn recycling practices through quizzes and AI assistance.
* Track recycling progress and environmental contributions.

By combining convenience, education, and motivation, RecycleGO encourages users to actively participate in sustainable waste management practices.

---

# Features

## 🔐 User Authentication

* Secure registration and login using Firebase Authentication.
* Role-based access:

  * User
  * Admin (Recycling Center Manager)

## ♻️ Recycling Pickup Request

Users can:

* Submit recycling pickup requests.
* Select recycling centers through an interactive map.
* Track request status.

Request statuses include:

1. Requesting
2. Accepted
3. Complete
4. Verified

## 🗺️ GPS Recycling Center Locator

* OpenStreetMap integration.
* Locate nearby recycling centers.
* Search recycling centers.
* Distance calculation between users and centers.

## 🎁 Reward System

* Earn points through recycling activities.
* Reward users for sustainable behavior.
* Display accumulated reward points.
* Foundation for future voucher redemption.

## 👥 Community Platform

Users can:

* Join recycling communities.
* Create and interact with posts.
* Comment on discussions.
* Share images and recycling experiences.
* Follow communities of interest.

Admins can:

* Create communities.
* Manage content.
* Moderate user interactions.

## 📅 Environmental Campaigns & Events

* Admins create environmental campaigns.
* Users browse available campaigns.
* Join sustainability-related events.
* Track campaign participation.

## 🤖 AI Recycling Assistant

Supports:

* Text-based recycling guidance.
* Image-based recyclable item identification.
* Voice interaction support.

AI assistance helps users determine:

* Whether an item is recyclable.
* Proper recycling methods.
* Recycling best practices.

## 🧠 Recycling Quiz

* Interactive multiple-choice questions.
* Educational learning experience.
* Reward points upon completion.
* Promotes environmental awareness.

## 📚 Recycling Tips API

* Retrofit REST API integration.
* Fetches recycling tips from remote JSON resources.
* Displays dynamic educational content on the home page.

---

# Technology Stack

## Frontend

* Java
* XML
* Android Studio
* Fragment Architecture

## Backend & Database

* Firebase Authentication
* Firebase Firestore

## APIs & Libraries

* Gemini API
* OpenStreetMap
* Retrofit REST API
* Glide Image Library

## Storage

* Firebase Firestore
* Local Storage (Images)

---

# Workflow

## User Journey

### 1. Registration & Login

* User creates an account.
* Selects role (User/Admin).
* Authentication is handled through Firebase.

### 2. Explore Home Page

* View recycling tips.
* Access rewards.
* Browse campaigns.
* Navigate to community features.

### 3. Request Recycling Pickup

* Fill in recycling details.
* Select preferred recycling center.
* Submit request.

### 4. Request Processing

Admin receives request:

* Accept request.
* Contact user via chat.
* Schedule pickup.
* Mark request as completed.

### 5. Verification

User:

* Confirms item collection.
* Verifies completion.
* Receives reward points.

### 6. Community Participation

Users:

* Join communities.
* Share experiences.
* Interact with posts.
* Follow sustainability initiatives.

### 7. Learning & Engagement

Users:

* Complete quizzes.
* Read recycling tips.
* Use AI assistant.
* Participate in campaigns.

### 8. Reward Accumulation

Points are awarded through:

* Recycling requests.
* Verification completion.
* Quiz participation.
* Community engagement.
* Campaign participation.

---

# System Architecture

```
User
   │
   ▼
Android Application
   │
   ├── Firebase Authentication
   ├── Firebase Firestore
   ├── OpenStreetMap
   ├── Gemini AI
   ├── Retrofit API Service
   └── Local Storage
```

---

# Future Improvements

## 🚀 Complete Recycling Pickup Module

* Fully automate request lifecycle.
* Better status tracking.
* Improved recycling center management.

## 🗺️ Advanced Map Features

* Smart filtering by recyclable material.
* Real-time recycling center availability.
* Route optimization.

## 💬 Real-Time Messaging

* Direct communication between users and admins.
* Push notifications.
* Real-time updates.

## 🎁 Voucher Redemption System

* Convert points into vouchers.
* Merchant partnerships.
* Discount and coupon marketplace.

## 🤖 Enhanced AI Features

* Image recognition for waste classification.
* Personalized recycling recommendations.
* Voice-powered recycling assistant.

## 📈 Sustainability Analytics

* Carbon footprint tracking.
* Recycling impact dashboard.
* Environmental contribution reports.

## 👥 Community Expansion

* Community challenges.
* Achievement badges.
* Leaderboards and rankings.

## 🔔 Notification System

* Campaign reminders.
* Pickup status updates.
* Reward notifications.

## ☁️ Scalability & Performance

* Cloud storage optimization.
* Large-scale user support.
* Improved database performance.

## 📱 Production Deployment

* Comprehensive testing.
* Security hardening.
* Google Play Store deployment.
* Future iOS version.

---

# Project Goal

RecycleGO aims to build a sustainable recycling ecosystem by connecting individuals, recycling centers, communities, and technology. Through education, rewards, and convenience, the platform empowers users to contribute actively toward a greener and more sustainable future.
