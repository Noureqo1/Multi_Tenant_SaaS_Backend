# Tenant Isolation Proof (Schema-Based Multi-Tenancy)

## Overview

This project implements **schema-based multi-tenancy** using PostgreSQL and Hibernate.  
Each tenant operates in an isolated database schema, ensuring strict data separation.

---

## Architecture

- **Database**: PostgreSQL
- **Multi-Tenancy Strategy**: SCHEMA-based
- **Schema Pattern**: `tenant_{id}`
- **Default Schema**: `public`

---

## Core Components

### 1. TenantContext
- Stores tenant ID per request using ThreadLocal
- Provides fallback to default schema (`public`)

### 2. TenantContextFilter
- Extracts tenant ID from:
    - JWT token (primary)
    - `X-Tenant-ID` header (fallback for testing)
- Sets schema dynamically per request

### 3. Tenant_Connection_Provider
- Switches PostgreSQL schema using:
```sql
SET search_path TO tenant_x