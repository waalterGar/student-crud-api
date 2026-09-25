# Student Management REST API

A clean RESTful API for managing student records built with **Java 21** and **Spring Boot 3**.

## Architecture & Features

- **Layered Architecture**: Controller -> Service -> Repository -> Entity/DTO.
- **Java 21 Records**: Implements DTOs (`StudentRequestDTO` & `StudentResponseDTO`) for complete data decoupling.
- **Input Validation**: Configured with `jakarta.validation` (`@NotBlank`, `@Email`, `@Min`).
- **Global Error Handling**: Centralized exceptions using `@RestControllerAdvice`.
- **Database**: In-memory H2 Database for fast development.

## Tech Stack

- Java 21 | Spring Boot 3 | Spring Data JPA | H2 Database | Maven

## API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/students` | Get all students |
| `GET` | `/api/v1/students/{id}` | Get student by ID |
| `POST` | `/api/v1/students` | Create new student |
| `PUT` | `/api/v1/students/{id}` | Update student |
| `DELETE` | `/api/v1/students/{id}` | Delete student |

## Quick Start

```bash
# Clone repository
git clone [https://github.com/](https://github.com/)<YOUR_USERNAME>/student-crud-api.git
cd student-crud-api

# Run application
./mvnw spring-boot:run