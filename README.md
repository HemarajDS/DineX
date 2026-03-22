# DineX
DineX is a full-stack Restaurant Management System built using Spring Boot, MongoDB, HTML, CSS, and JavaScript. It supports user authentication, menu management, order processing, and an admin dashboard for efficient restaurant operations.


# Restaurant Management System

This is a complete beginner-friendly full-stack Restaurant Management System built with Spring Boot, MongoDB, Maven, and a simple Bootstrap frontend.

## Tech Stack

- Spring Boot 3.3.16
- Java 17
- Maven
- MongoDB
- Spring Data MongoDB
- HTML, CSS, JavaScript, Bootstrap 5

## Features

- User registration and login with validation
- Role-based access with `ADMIN` and `USER`
- Menu management: add, update, delete, list
- Search and filter menu items
- Create orders with multiple items
- Automatic bill calculation
- Order status tracking: `PENDING`, `PREPARING`, `COMPLETED`
- REST APIs for backend operations
- Global exception handling
- Sample data using a MongoDB startup initializer
- Responsive UI

## Project Structure

```text
restaurant-management-system
|-- pom.xml
|-- README.md
|-- postman/
|   `-- Restaurant-Management-System.postman_collection.json
`-- src/
    `-- main/
        |-- java/com/example/restaurant/
        |   |-- RestaurantManagementApplication.java
        |   |-- config/
        |   |-- controller/
        |   |-- dto/
        |   |-- entity/
        |   |-- exception/
        |   |-- repository/
        |   `-- service/
        `-- resources/
            |-- application.properties
            `-- static/
                |-- css/
                |-- js/
                |-- pages/
                `-- index.html
```

## Database Schema

### `users` collection

- `_id` STRING
- `name` VARCHAR(100)
- `email` VARCHAR(120) UNIQUE
- `password` VARCHAR(255)
- `role` VARCHAR(20)

### `menu_items` collection

- `_id` STRING
- `name` VARCHAR(120)
- `price` DECIMAL(10,2)
- `category` VARCHAR(80)

### `orders` collection

- `_id` STRING
- `userId` STRING
- `customerName` STRING
- `total_amount` DECIMAL(10,2)
- `status` VARCHAR(20)
- `createdAt` DATETIME
- `orderItems` ARRAY

Each embedded `orderItems` object contains:

- `id`
- `menuItemId`
- `menuItemName`
- `quantity`
- `price`

## Sample Accounts

- Admin: `admin@restaurant.com` / `password123`
- User: `rahul@restaurant.com` / `password123`

## API Endpoints

### Authentication

- `POST /api/auth/register`
- `POST /api/auth/login`

### Menu Items

- `GET /api/menu-items`
- `POST /api/menu-items`
- `PUT /api/menu-items/{id}`
- `DELETE /api/menu-items/{id}`

### Orders

- `GET /api/orders`
- `GET /api/orders/{id}`
- `POST /api/orders`
- `PATCH /api/orders/{id}/status`

### Dashboard

- `GET /api/dashboard/summary`

## Authentication Note

This project uses a simple demo-style authentication flow:

1. Login using `/api/auth/login`
2. The frontend stores the returned user object in browser `localStorage`
3. Protected API calls send `X-USER-ID` and `X-USER-ROLE`

This keeps the code simple and beginner-friendly. For production, replace this with Spring Security + JWT or session-based authentication.

## How To Run Locally

### 1. Install prerequisites

- Java 17
- Maven 3.9+
- MongoDB Community Server

Note: this project uses Spring Boot 3.3.16, so Java 17 is required.

### 2. Configure MongoDB

Make sure MongoDB is running locally, then update [`application.properties`](/C:/Users/CapulusTech/Documents/New%20project/src/main/resources/application.properties) if needed:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/restaurant_management
```

### 3. Run the application

```bash
mvn spring-boot:run
```

### 4. Open in browser

Visit [http://localhost:8080](http://localhost:8080)

## Frontend Pages

- `/pages/login.html`
- `/pages/dashboard.html`
- `/pages/menu.html`
- `/pages/order-create.html`
- `/pages/orders.html`

## Postman Collection

Use the included Postman file:

- [Restaurant-Management-System.postman_collection.json](/C:/Users/CapulusTech/Documents/New%20project/postman/Restaurant-Management-System.postman_collection.json)

## Sample Data Note

Sample users, menu items, and orders are inserted automatically by [DataInitializer.java](/C:/Users/CapulusTech/Documents/New%20project/src/main/java/com/example/restaurant/config/DataInitializer.java) when the MongoDB collections are empty.
