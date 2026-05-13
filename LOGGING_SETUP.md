# SLF4J Logging Implementation - Industry Level Architecture

## Overview
This document describes the SLF4J logging setup implemented in the Inventory Management System following industry-level best practices and standards.

## Components

### 1. Dependencies Added
- **SLF4J API** (org.slf4j:slf4j-api): The logging facade providing the standard API
- **Logback Core** (ch.qos.logback:logback-core): The core logging implementation
- **Logback Classic** (ch.qos.logback:logback-classic): SLF4J implementation with advanced features

### 2. Configuration Files

#### a) logback-spring.xml
Located at: `src/main/resources/logback-spring.xml`

**Key Features:**
- **Console Appender**: Logs to stdout with color-coded output for development
- **File Appender**: Rolls daily or when 10MB is reached, with 10-day history
- **Error File Appender**: Separate ERROR and FATAL logs for issue tracking
- **Async Appenders**: Non-blocking file logging for performance optimization
- **Spring Profiles**: Different logging configurations for dev, prod, and test environments

**Log Patterns:**
```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```
- `%d`: Timestamp for precise timing
- `[%thread]`: Thread name for debugging multi-threaded issues
- `%-5level`: Log level (INFO, DEBUG, WARN, ERROR)
- `%logger{36}`: Logger name (truncated to 36 chars)
- `%msg`: The actual log message

#### b) application.properties
Updated with logging configuration sections:
```properties
# Application logging levels (DEBUG for development, INFO for production)
logging.level.root=INFO
logging.level.com.inventory=DEBUG

# Spring Framework levels
logging.level.org.springframework=INFO
logging.level.org.hibernate=INFO
logging.level.org.postgresql=INFO

# File configuration
logging.file.name=logs/application.log
logging.file.max-size=10MB
logging.file.max-history=10
```

### 3. Logging Implementation in Classes

#### ProductService.java
- **@Slf4j**: Lombok annotation to inject SLF4J logger
- **INFO**: Business operations (save, update, delete)
- **DEBUG**: Detailed flow information (method entry, mapping operations)
- **WARN**: Suspicious events (resource not found)
- **ERROR**: Exceptions with stack trace

Example:
```java
log.info("Saving new product - Name: {}, Brand: {}", name, brand);
log.debug("Mapping product entity to response DTO - ID: {}", id);
log.warn("Product not found with ID: {}", id);
log.error("Error occurred while saving product", e);
```

#### ProductController.java
- **@Slf4j**: Lombok annotation
- **INFO**: API requests and responses
- Tracks HTTP methods, parameters, and response details

Example:
```java
log.info("API Request: POST /api/products - Creating new product");
log.info("API Response: Product created successfully with ID: {}", id);
```

#### GlobalExceptionHandler.java
- **@Slf4j**: Lombok annotation
- **WARN**: Validation and business exceptions
- **ERROR**: Critical/unhandled exceptions with stack trace
- **DEBUG**: Response details

#### HelloController.java
- **@Slf4j**: Lombok annotation
- **INFO**: Health check endpoint access

## Logging Best Practices Applied

### 1. Log Levels Usage
- **TRACE**: Method parameter values (not used in current setup, but available)
- **DEBUG**: Detailed flow, method entry/exit, data transformations
- **INFO**: Business operations, API requests/responses, successful operations
- **WARN**: Suspicious situations, recoverable errors, unexpected states
- **ERROR**: Exceptions, unrecoverable errors

### 2. Performance Optimization
- **Async Appenders**: Non-blocking file I/O operations
- **Queue Size**: 512 items before blocking
- **Discarding Threshold**: 0 (no messages discarded to prevent data loss)
- **Rolling Policies**: Size-based (10MB) and time-based (daily) rotation

### 3. Structured Logging
- **Consistent Format**: Timestamp, thread, level, logger, message
- **Parameter Interpolation**: Using `{}` placeholders instead of string concatenation
  ```java
  // Good: Deferred string concatenation
  log.info("Product ID: {}", productId);
  
  // Bad: Immediate concatenation
  log.info("Product ID: " + productId);
  ```

### 4. File Organization
- **Main Logs**: `logs/application.log` (rotated daily)
- **Error Logs**: `logs/error.log` (ERROR and FATAL only)
- **Retention**: 10 days of history, up to 100MB total

### 5. Environment-Specific Configuration
- **Development**: DEBUG level for application logs, Console + File output
- **Production**: INFO level, File only (no console), Async appenders
- **Testing**: INFO level, Console only

## Running the Application

### Development
```bash
# Run with default profile (development)
java -jar app.jar

# Logs will appear in:
# - Console (real-time)
# - logs/application.log (file)
```

### Production
```bash
# Run with prod profile
java -jar app.jar --spring.profiles.active=prod

# Logs will only appear in logs/application.log (async, no console)
```

## Log File Locations
- **Application Logs**: `logs/application.log`
- **Error Logs**: `logs/error.log`
- **Old Logs**: `logs/application.2024-01-15.1.log` (date-based)

## Example Log Output

```
2026-05-13 10:30:45.123 [main] INFO com.inventory.inventorymanagementsystem.controller.ProductController - API Request: POST /api/products - Creating new product with name: Laptop
2026-05-13 10:30:45.124 [main] DEBUG com.inventory.inventorymanagementsystem.service.ProductService - Saving new product - Name: Laptop, Brand: Dell, Price: 50000.0, Stock: 10
2026-05-13 10:30:45.234 [main] INFO com.inventory.inventorymanagementsystem.service.ProductService - Product saved successfully with ID: 1
2026-05-13 10:30:45.235 [main] INFO com.inventory.inventorymanagementsystem.controller.ProductController - API Response: Product created successfully with ID: 1
```

## Migration from Old Logging

If you were previously using System.out.println():
```java
// Old Way
System.out.println("Product saved: " + product.getName());

// New Way
log.info("Product saved: {}", product.getName());
```

## Adding Logging to New Classes

When creating new classes, follow this template:

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MyService {
    
    public void doSomething(String param) {
        log.info("Starting operation with param: {}", param);
        
        try {
            // Business logic
            log.debug("Processing details...");
            
            log.info("Operation completed successfully");
        } catch (Exception e) {
            log.error("Error during operation", e);
            throw e;
        }
    }
}
```

## Troubleshooting

### Logs not appearing in console
- Check `application.properties` for correct logging levels
- Ensure `logback-spring.xml` is in `src/main/resources`

### Logs not being written to files
- Check if `logs/` directory exists and has write permissions
- Verify `logging.file.name` configuration in `application.properties`

### Too much logging noise
- Adjust package-level logging levels in `application.properties`
- Reduce DEBUG level for verbose packages like Hibernate

## References
- [SLF4J Documentation](https://www.slf4j.org/)
- [Logback Documentation](https://logback.qos.ch/)
- [Spring Boot Logging](https://spring.io/guides/gs/logging-log4j2/)
- [12-Factor App - Logs](https://12factor.net/logs)

