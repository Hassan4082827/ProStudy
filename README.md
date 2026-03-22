# ProStudy System Infrastructure `v2.0.0`

![Version](https://img.shields.io/badge/Version-2.0.0-blue?style=for-the-badge) ![Platform](https://img.shields.io/badge/Developed_on-Mobile-orange?style=for-the-badge) ![Status](https://img.shields.io/badge/Status-Online-red?style=for-the-badge)

> [!IMPORTANT]
> **ProStudy v2.0.0** is a private, custom-engineered education management platform. This system is developed exclusively for my school environment and is not a public utility.

---

## 🏛️ System Overview

The ProStudy ecosystem operates as a specialized hybrid application. This repository hosts the **Centralized Web Engine**, serving as the "Single Source of Truth" for the native Android client. 

### ⚙️ Technical Specifications

* **Architecture:** Decoupled Frontend-as-a-Service (FaaS) optimized for **Android 14 System WebView**.
* **Visual Layer:** Custom grid-based UI system engineered for high-DPI mobile displays.
* **Identity:** High-fidelity **1024x1024** branding assets ensuring zero-blur scaling.
* **Logic:** Hardware-accelerated transitions and intelligent "Empty State" protocols.

---

## 📱 Mobile-First Development Environment

> [!TIP]
> **Technical Achievement:** The entire ProStudy ecosystem—including the native Java Android client and this web infrastructure—is **developed, managed, and deployed entirely via mobile hardware.** ---

## 📂 Repository Structure

<details>
<summary><b>View Directory Architecture (Click to Expand) 📂</b></summary>

```text
ProStudy-Web/
├── /assets/                <-- High-Resolution Brand Identity
│   ├── main.png            <-- 1024x1024 Master Logo Asset
│   ├── apple-touch-icon.png
│   └── favicon.ico
├── /interface/             <-- Visual Layer
│   └── style.css           <-- Custom Grid & Card Styling
├── /interactivity/         <-- Logic Layer
│   └── script.js           <-- UI Engine & Data Handling
├── index.html              <-- System Entry Point
├── dashboard.html          <-- Primary User Interface
├── homework.html           <-- Academic Assignment Module
├── news.html               <-- School-Specific Update Feed
├── request.html            <-- User Inquiry & Feedback Portal
└── LICENSE                 <-- Legal Framework
