# Multi-Tenant SaaS Backend

A comprehensive Spring Boot backend application demonstrating multi-tenant architecture with exception handling, transaction management, and security features.

## 🏗️ Project Overview

This project implements a Software-as-a-Service (SaaS) backend with multi-tenant capabilities, built using Spring Boot and modern Java technologies. It serves as a reference implementation for enterprise-grade applications requiring robust error handling and transaction management.

## ✨ Key Features

- **Multi-Tenant Architecture**: Support for multiple tenants with data isolation
- **Exception Handling**: Global exception handling with custom error responses
- **Transaction Management**: Comprehensive transaction rollback demonstrations
- **Security**: JWT-based authentication with role-based access control
- **RESTful APIs**: Well-designed REST endpoints with proper HTTP status codes
- **Database Integration**: H2 in-memory database with JPA/Hibernate
- **Comprehensive Testing**: Complete test coverage with Postman collections

## 🛠️ Technology Stack

- **Backend**: Spring Boot 4.0.3
- **Language**: Java 17
- **Database**: H2 (In-memory)
- **ORM**: Spring Data JPA with Hibernate
- **Security**: Spring Security with JWT
- **Build Tool**: Gradle
- **Testing**: JUnit 5, Postman

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Gradle 8.0 or higher
- Postman (for API testing)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Noureqo1/Multi_Tenant_SaaS_Backend.git
   cd Multi_Tenant_SaaS_Backend
   ```

2. **Build the application**
   ```bash
   ./gradlew build
   ```

3. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

4. **Access the application**
   - API Base URL: `http://localhost:8081`
   - H2 Console: `http://localhost:8081/h2-console`
   - Database URL: `jdbc:h2:mem:workhubdb`
   - Username: `sa`
   - Password: (empty)

## 📚 API Documentation

### Authentication Endpoints

#### Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "admin@tenant1.com",
  "password": "123456"
}
```

#### Get Current User
```http
GET /auth/me
Authorization: Bearer {jwt_token}
```

### Order Management Endpoints

#### Create Order
```http
POST /orders?productId=1&quantity=2
```

**Test Scenarios:**
- **Success**: `quantity ≤ 5` and sufficient stock
- **Business Error**: Insufficient stock (400 Bad Request)
- **Rollback Demo**: `quantity > 5` triggers rollback (500 Internal Server Error)
- **Not Found**: Invalid productId (400 Bad Request)

## 🎯 Exception Handling Module

### Features Demonstrated

1. **Global Exception Handler**: Centralized error processing
2. **Custom Exceptions**: Business and resource-specific exceptions
3. **Transaction Rollback**: Automatic rollback on runtime exceptions
4. **Error Response Format**: Consistent JSON error structure

### Error Response Format
```json
{
  "status": 400,
  "message": "Not enough stock",
  "path": "/orders",
  "timestamp": "2026-04-05T00:45:39.5852716"
}
```

### Test Cases

| Scenario | Request | Expected Response | Description |
|----------|---------|-------------------|-------------|
| Success | `POST /orders?productId=1&quantity=2` | 200 OK | Order created successfully |
| Business Error | `POST /orders?productId=1&quantity=25` | 400 Bad Request | Insufficient stock |
| Rollback Demo | `POST /orders?productId=1&quantity=10` | 500 Internal Server Error | Transaction rollback |
| Not Found | `POST /orders?productId=999&quantity=2` | 400 Bad Request | Product not found |

## 🔐 Security Configuration

### Default Users

| Email | Password | Role | Tenant |
|-------|----------|------|--------|
| admin@tenant1.com | 123456 | TENANT_ADMIN | 1 |
| user@tenant1.com | 123456 | TENANT_USER | 1 |
| admin@tenant2.com | 123456 | TENANT_ADMIN | 2 |

### JWT Configuration

- **Secret**: Configured in application.properties
- **Expiration**: 24 hours (86400000 ms)
- **Algorithm**: HS256

## 📊 Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    tenant_id BIGINT NOT NULL
);
```

### Products Table
```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    stock INT NOT NULL
);
```

### Orders Table
```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

## 🧪 Testing

### Postman Collection

A comprehensive Postman collection is provided with all test scenarios:

1. **Import the collection**: `Postman_Collection_Exception_Handling.json`
2. **Set environment**: Base URL configured as `{{baseUrl}}` (http://localhost:8081)
3. **Run tests**: Execute requests in order to demonstrate all features

### Test Data

Sample products are automatically created on startup:
- **Product 1**: Laptop (Stock: 20)
- **Product 2**: Mouse (Stock: 30)
- **Product 3**: Keyboard (Stock: 15)

## 🏗️ Project Structure

```
Multi_Tenant_SaaS_Backend/
├── src/main/java/Multi_TenantSaaS/SW452/Project/
│   ├── auth/                    # Authentication components
│   ├── common/                  # Common utilities
│   ├── config/                  # Configuration classes
│   ├── controller/              # REST controllers
│   ├── entity/                  # JPA entities
│   ├── exception/               # Exception handling
│   ├── repository/              # Data repositories
│   ├── security/                # Security configuration
│   ├── service/                 # Business logic
│   └── user/                    # User management
├── src/test/                    # Test classes
├── Postman_Collection_Exception_Handling.json
├── Exception_Handling_Transaction_README.md
├── DESIGN-NOTE-Exception-Handling.md
└── ReadMe.md
```

## 🔧 Configuration

### Application Properties

```properties
# Server Configuration
server.port=8081
spring.application.name=Multi_Tenant_SaaS_Backend

# Database Configuration
spring.datasource.url=jdbc:h2:mem:workhubdb
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# JWT Configuration
jwt.secret=U29tZVN1cGVyU2VjcmV0S2V5U29tZVN1cGVyU2VjcmV0S2V5MTIzNDU2
jwt.expiration=86400000
```

## 📈 Performance Considerations

- **Connection Pooling**: HikariCP for database connections
- **Lazy Loading**: JPA relationships optimized
- **Transaction Scope**: Minimal transaction boundaries
- **Exception Handling**: Efficient error processing

## 🔮 Future Enhancements

### Planned Features

1. **Multi-Database Support**: PostgreSQL, MySQL integration
2. **Caching Layer**: Redis for performance optimization
3. **Audit Logging**: Comprehensive audit trail
4. **API Documentation**: Swagger/OpenAPI integration
5. **Microservices**: Distributed architecture support
6. **Monitoring**: Actuator and Micrometer metrics

### Scalability Improvements

- **Horizontal Scaling**: Load balancer support
- **Database Sharding**: Multi-tenant data distribution
- **Circuit Breaker**: Resilience patterns
- **Async Processing**: Message queue integration

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Team

- **Project Lead**: Multi-Tenant SaaS Team
- **Exception Handling Module**: Member 4
- **Architecture & Design**: Development Team

## 📞 Support

For support and questions:

- 📧 Email: support@multitenant-saas.com
- 🐛 Issues: [GitHub Issues](https://github.com/Noureqo1/Multi_Tenant_SaaS_Backend/issues)
- 📖 Documentation: [Wiki](https://github.com/Noureqo1/Multi_Tenant_SaaS_Backend/wiki)

## 🙏 Acknowledgments

- Spring Boot team for excellent framework
- H2 Database for lightweight testing solution
- Postman team for API testing tools
- Open source community for valuable contributions

---

**Version**: 1.0.0  
**Last Updated**: April 2026  
**Status**: Production Ready ✅