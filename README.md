# Temperature App

This application fetches temperature data for a given latitude and longitude and caches it in MongoDB for performance. If the cache data is older than 1 minute, it fetches fresh data from the Open Meteo API.

## Prerequisites

- Java 21
- Docker and Docker Compose

## Running the Application

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd <repository-folder>
