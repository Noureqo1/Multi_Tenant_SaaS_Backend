# Exception Handling & Transaction Module - Design Documentation

## Overview
This module implements comprehensive exception handling and transaction management for the Multi-Tenant SaaS Backend application. It provides centralized error handling, custom exceptions, and demonstrates transaction rollback capabilities.

## Architecture

### Component Structure
```
Multi_TenantSaaS.SW452.Project/
├── exception/
│   ├── ApiError.java              # Error response model
│   ├── BusinessException.java     # Custom business exception
│   ├── ResourceNotFoundException.java # Custom resource exception
│   └── GlobalExceptionHandler.java # Central exception handler
├── entity/
│   ├── Order.java                 # Order entity
│   └── Product.java               # Product entity
├── repository/
│   ├── OrderRepository.java       # Order data access
│   └── ProductRepository.java     # Product data access
├── service/
│   └── OrderService.java          # Transactional business logic
└── controller/
    └── OrderController.java       # REST endpoints
```

## Design Decisions

### 1. Global Exception Handler

**Rationale**: Centralized error handling provides:
- **Code Readability**: Exception handling logic in one place
- **Error Consistency**: Uniform error response format across all endpoints
- **Maintainability**: Easy to modify error handling behavior
- **Separation of Concerns**: Business logic separate from error handling

**Implementation**: `@ControllerAdvice` with `@ExceptionHandler` methods

### 2. Custom Exceptions

**BusinessException**: 
- Used for business rule violations
- Returns HTTP 400 Bad Request
- Examples: Insufficient stock, invalid business operations

**ResourceNotFoundException**:
- Used for missing resources
- Returns HTTP 404 Not Found
- Examples: Product not found, user not found

### 3. Transaction Management

**@Transactional Annotation**:
- **Atomic Operations**: Ensures all operations succeed or fail together
- **Data Consistency**: Prevents partial database updates
- **Rollback Capability**: Automatically rolls back on runtime exceptions

**Transaction Boundaries**:
- Service layer methods marked as `@Transactional`
- Database operations within transaction scope
- Automatic rollback on unhandled exceptions

### 4. Rollback Strategy

**Rollback Triggers**:
- **RuntimeException**: Automatic rollback
- **Business Rule Violations**: Custom exceptions with rollback
- **System Errors**: Unhandled exceptions cause rollback

**Demo Implementation**:
- Order creation with stock management
- Intentional exception for demonstration
- Verification of rollback through stock levels

## Exception Flow

```
Request → Controller → Service → Repository → Database
                    ↓
               Exception Thrown
                    ↓
         GlobalExceptionHandler
                    ↓
           Formatted JSON Response
```

## Error Response Format

**Standardized ApiError Structure**:
```json
{
  "status": 400,
  "message": "Not enough stock",
  "path": "/orders",
  "timestamp": "2026-04-05T00:45:39.5852716"
}
```

**Fields**:
- `status`: HTTP status code
- `message`: Human-readable error description
- `path`: Request endpoint
- `timestamp`: Error occurrence time

## API Endpoints

### Order Management
- **POST /orders?productId={id}&quantity={count}**
  - Creates new order with product and quantity
  - Validates stock availability
  - Demonstrates transaction rollback

### Test Scenarios

1. **Success Case** (200 OK):
   - Quantity ≤ 5 and sufficient stock
   - Order created successfully

2. **Business Error** (400 Bad Request):
   - Insufficient stock
   - BusinessException thrown

3. **Rollback Demo** (500 Internal Server Error):
   - Quantity > 5 triggers intentional exception
   - Transaction rollback demonstrated

4. **Resource Not Found** (400 Bad Request):
   - Invalid productId
   - ResourceNotFoundException thrown

## Security Configuration

**Endpoint Access**:
- `/auth/login` - Public access
- `/orders/**` - Public access for testing
- Other endpoints - Authentication required

## Database Schema

**Products Table**:
```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    stock INT NOT NULL
);
```

**Orders Table**:
```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

## Testing Strategy

### Unit Testing
- Exception handler methods
- Service layer business logic
- Repository data access

### Integration Testing
- End-to-end API testing
- Transaction rollback verification
- Error response validation

### Manual Testing
- Postman collection provided
- All exception scenarios covered
- Rollback demonstration included

## Performance Considerations

### Exception Handling
- Minimal overhead for exception processing
- Efficient JSON serialization
- Proper HTTP status codes

### Transaction Management
- Short-lived transactions
- Optimistic locking where applicable
- Proper resource cleanup

## Future Enhancements

### Potential Improvements
1. **Audit Logging**: Log all exceptions for monitoring
2. **Custom Error Codes**: More granular error classification
3. **Internationalization**: Multi-language error messages
4. **Rate Limiting**: Prevent abuse of error-prone endpoints

### Scalability
- Distributed transaction support
- Circuit breaker pattern
- Async error processing

## Deliverables Checklist

### Core Components
✅ **GlobalExceptionHandler.java** - Centralized exception handling
✅ **ApiError.java** - Standardized error response model
✅ **Custom Exceptions** - BusinessException, ResourceNotFoundException
✅ **Transactional Use Case** - OrderService with @Transactional
✅ **Rollback Demo** - Intentional exception for demonstration

### Documentation & Testing
✅ **README.md** - Module documentation and usage guide
✅ **Postman Collection** - Complete API testing suite
✅ **DESIGN-NOTE** - Comprehensive design documentation

### Integration
✅ **Security Configuration** - Proper endpoint access control
✅ **Database Integration** - Entity and repository setup
✅ **Sample Data** - Product initialization for testing

## Conclusion

The Exception Handling & Transaction Module provides a robust foundation for error management and data consistency in the Multi-Tenant SaaS Backend. It demonstrates best practices in Spring Boot application development and serves as a reference for implementing similar functionality in enterprise applications.

**Module Status**: ✅ **COMPLETE AND TESTED**

**Member 4 Implementation**: ✅ **SUCCESSFULLY DELIVERED**