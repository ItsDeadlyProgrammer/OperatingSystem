# 💻 OS Simulator: Multi-Platform OS Concepts

Welcome to **OS Simulator** — a comprehensive, multi-platform application designed to visualize and simulate core Operating System concepts, including **Process Scheduling**, **Deadlock Management**, and **Continuous Memory Allocation**. Built with **Kotlin Multiplatform (KMP)**, it provides a consistent experience across Desktop, Android, and Web platforms.

## ✨ Features

This simulator provides dynamic visualization and calculation across three major operating system domains:

### 1. ⏱️ CPU Process Scheduling

* **Algorithms Implemented:** Simulate and compare six classic algorithms:

    * First-Come, First-Served (FCFS)

    * Shortest Job First (SJF) (Preemptive and Non-Preemptive)

    * Shortest Remaining Time First (SRTF)

    * Round Robin (RR)

    * Priority Scheduling (Preemptive and Non-Preemptive)

* **Visualization:** Dynamic **Gantt Chart** visualization of process execution.

* **Metrics:** Calculates and displays key performance metrics: **Average Waiting Time (AWT)** and **Average Turnaround Time (ATT)**.

### 2. 🚦 Deadlock Detection & Avoidance

* **Resource Allocation Graph (RAG):** Dynamically builds and visualizes the RAG as resources are allocated and requested.

* **Deadlock Detection:** Features a tool that actively scans the graph to detect the presence of circular wait conditions.

* **Banker's Algorithm:** Computes the **Safe Sequence** for resource allocation to demonstrate deadlock avoidance.

### 3. 🧠 Continuous Memory Management

* **Allocation Algorithms:** Demonstrates the core continuous memory allocation strategies:

    * First Fit

    * Best Fit

    * Worst Fit

    * Next Fit

* **Visualization:** Clears visualization of memory partitioning, allocation, and deallocation.

* **Fragmentation Analysis:** Clearly visualizes and calculates **Internal** and **External Fragmentation** caused by the different strategies.

## 🛠️ Tech Stack & Architecture

This project leverages the power of Kotlin Multiplatform for cross-platform delivery:

* **Core Language:** Kotlin

* **Cross-Platform UI:** Compose Multiplatform (Desktop/JVM, Android, Web)

* **Architecture:** MVVM/State-Driven Composables

* **Targets:** Android, Desktop (JVM), and Web (WasmJs/JS)

## 📦 Setup Instructions

Since this is a Kotlin Multiplatform project, setup is straightforward across all environments.

1.  **Clone the repository:**

    ```bash
    git clone https://github.com/ItsDeadlyProgrammer/OperatingSystem.git
    ```

2.  **Open in IntelliJ IDEA or Android Studio:**

    * Use the latest stable version of IntelliJ IDEA (Ultimate recommended for Compose Multiplatform) or Android Studio.

3.  **Run a specific target:**

    * **Android:** Select the `composeApp` module and run the `androidApp` configuration on an emulator or physical device.

    * **Desktop (JVM):** Run the `composeApp/run` Gradle task (or select the main desktop configuration in your IDE).

    * **Web (WasmJs/JS):** Run the `composeApp:wasmJsBrowserDevelopmentRun` Gradle task and open `http://localhost:8080/` in your browser.

---

## 🚀 Live Demo & Downloads

You can experience the application across all its supported platforms:

### 🌐 Web Demo (GitHub Pages)

Try the interactive simulator built with WasmJs directly in your browser:

[**Launch Web Simulator**](https://itsdeadlyprogrammer.github.io/OperatingSystem/)

### 📱 Android Download

Download the standalone APK to install on your Android device:

[![Download APK](https://img.shields.io/badge/Download-APK-brightgreen?style=for-the-badge&logo=android)](https://github.com/ItsDeadlyProgrammer/OperatingSystem/releases/download/v1.0.0/OperatingSystem.apk)

### 💻 Desktop Download

Download the runnable Fat JAR file for JVM Desktop environments:

[![Download JAR](https://img.shields.io/badge/Download-JAR-blue?style=for-the-badge&logo=java)](https://github.com/ItsDeadlyProgrammer/OperatingSystem/releases/download/v1.0.0/OperatingSystem-1.0.0.msi)

---

## 🧑‍💻 Author

**Harshvardhan Singh**  
[![GitHub](https://img.shields.io/badge/GitHub-ItsDeadlyProgrammer-blue)](https://github.com/ItsDeadlyProgrammer)

❤️ This project is a strong demonstration of complex data structure visualization and the power of Kotlin Multiplatform for building educational tools. Feel free to fork, explore, and contribute! PRs are always welcome. ❤️
