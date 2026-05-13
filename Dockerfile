# Multi-stage Dockerfile for building and running a Spring Boot application

# Stage 1: Build the application using Eclipse Temurin JDK 26 and Maven
# This stage compiles the source code and packages it into a JAR file
FROM eclipse-temurin:26-jdk AS build

# Install Maven in the build stage
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Set the working directory inside the container for the build process
# All subsequent commands in this stage will run from /app
WORKDIR /app

# Copy only the pom.xml first to leverage Docker layer caching
# This allows Maven to download dependencies before copying source code,
# so dependency layers are cached and not rebuilt unless pom.xml changes
COPY pom.xml .

# Download all project dependencies offline in batch mode
# This step caches dependencies, speeding up future builds
RUN mvn dependency:go-offline -B

# Copy the source code into the container
# This is done after dependency download to avoid invalidating the cache on code changes
COPY src ./src

# Compile and package the application into a JAR file
# -DskipTests skips running tests to speed up the build
# The Spring Boot Maven plugin creates a fat JAR with all dependencies included
RUN mvn clean package -DskipTests

# Stage 2: Runtime image using Eclipse Temurin JRE 26 (smaller than JDK)
# This stage contains only the runtime environment, not the build tools
FROM eclipse-temurin:26-jre

# Set the working directory for the runtime container
# The JAR will be placed here and executed from this directory
WORKDIR /app

# Copy the built JAR from the build stage to the runtime stage
# The JAR is renamed to 'app.jar' for simplicity
# Using --from=build references the first stage
COPY --from=build /app/target/*.jar app.jar

# Expose port 8080, which is the default port for Spring Boot applications
# This informs Docker that the container listens on this port (does not publish it)
EXPOSE 8080

# Add a health check to monitor the application's status
# - interval: Check every 30 seconds
# - timeout: Fail if check takes longer than 3 seconds
# - start-period: Wait 5 seconds after container starts before first check
# - retries: Mark unhealthy after 3 failed checks
# Uses curl to hit the Spring Boot Actuator health endpoint
# If the endpoint returns a non-200 status, the container is considered unhealthy
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Define the entrypoint to run the application
# This starts the Spring Boot app when the container is launched
# Equivalent to running 'java -jar app.jar' in the terminal
ENTRYPOINT ["java", "-jar", "app.jar"]
