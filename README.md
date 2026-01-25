# 🏥 Appointment & Queue Management System (QMS)

A robust, full-stack Spring Boot application designed to streamline clinic operations. This project features a patient booking portal, a real-time doctor dashboard, and an automated public display system.

## 🌟 Key Features
* **Token-Based Booking:** Automatically generates unique tokens for patients to ensure privacy.
* **Live QMS Dashboard:** Allows staff to "Call" and "Complete" appointments, moving patients through states (Waiting -> In-Progress -> Completed).
* **Public Waiting Room Display:** A dedicated, auto-refreshing view for monitors that announces the current token via **Voice (Text-to-Speech)**.
* **Modern UI:** Fully responsive design using Bootstrap 5.

## 🛠️ Tech Stack
* **Backend:** Java 23, Spring Boot 3.4
* **Data:** Spring Data JPA, H2 (In-Memory Database)
* **Frontend:** Thymeleaf, Bootstrap 5, JavaScript (Web Speech API)
* **Build Tool:** Maven

## 🚦 How to Run
1. Clone the repository.
2. Run `./mvnw spring-boot:run`
3. Access the app at `http://localhost:8080`
