# Temperature App

This application fetches temperature data for a given latitude and longitude and caches it in MongoDB for performance optimization. If the cached data is older than 1 minute, it fetches fresh data from the Open Meteo API. Additionally, the application includes a Kafka producer to send temperature data for each request received, and Swagger is used for API documentation.

## Features

- Retrieves temperature data based on latitude and longitude.
- Uses MongoDB as a cache to reduce the number of API calls.
- Automatically fetches fresh data if cached data is over 1 minute old.
- Exposes endpoints to delete cached data by location.
- Sends temperature data to a Kafka topic whenever a GET request is received.
- Swagger documentation for easy API interaction.
- Containerized with Docker and Docker Compose.

## Prerequisites

- Java 21
- Docker and Docker Compose
- MongoDB (included in Docker Compose)
- Kafka (included in Docker Compose)

## Project Structure

- **Controller**: Contains the REST controllers for handling HTTP requests.
- **Service**: Interface and implementation for business logic.
- **Model**: Entity classes for temperature data and response structure.
- **Repository**: MongoDB repository for temperature data.
- **Kafka**: Kafka producer for sending messages on data retrieval.
- **Config**: Application configurations, including Kafka and RestTemplate beans.

## Running the Application

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd <repository-folder>

2. **Build the Docker Image**
   ```bash
   docker-compose up --build -d

3. **Access the Application**
- **API**: The application runs on http://localhost:8080.
- **Swagger**: The Swagger documentation is available at http://localhost:8080/swagger-ui.html.
- **Kafka**: The Kafka server runs on http://localhost:9092.
- **MongoDB**: The MongoDB server runs on http://localhost:27017.

## API Endpoints

### 1. GET /temperature

#### Request Parameters:
- **latitude**: Latitude of the location (required).
- **longitude**: Longitude of the location (required).

#### Example Request:
```http
GET - /temperature?latitude=40.7128&longitude=-74.0060
```
#### Example Response:
```json
{
  "latitude": 40.7128,
  "longitude": -74.0060,
  "currentWeather": {
    "temperature": 25.0
  }
}
```

