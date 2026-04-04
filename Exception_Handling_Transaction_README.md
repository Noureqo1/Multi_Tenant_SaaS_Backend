# Exception Handling & Transaction Module

## Features

- Global Exception Handling
- Custom Exceptions
- Transaction Management
- Rollback Demonstration

## Endpoints

Create Order
POST /orders?productId=1&quantity=2

Rollback Demo
POST /orders?productId=1&quantity=10

## Expected Behavior

Quantity <=5 → Success
Quantity >5 → Rollback Triggered

## Error Format

{
  "status":400,
  "message":"Not enough stock",
  "path":"/orders",
  "timestamp":"2026-01-01T10:00:00"
}
