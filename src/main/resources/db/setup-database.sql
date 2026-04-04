-- ==================================================================
-- WorkHub Multi-Tenant Database Setup Script
-- Run this after creating the workhubdb database
-- ==================================================================

-- Connect to workhubdb database first, then run:

-- 1. Create tenant schemas
CREATE SCHEMA IF NOT EXISTS tenant_1;
CREATE SCHEMA IF NOT EXISTS tenant_2;
CREATE SCHEMA IF NOT EXISTS tenant_3;

-- 2. Create users table in public schema (for authentication)
CREATE TABLE IF NOT EXISTS public.users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Insert test users 
-- Password for all users: "password123" (BCrypt hashed)
INSERT INTO public.users (email, password, role, tenant_id) VALUES
('admin@acme.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qLlLXOY6hKGAFzPIZW', 'ADMIN', 1),
('user@acme.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qLlLXOY6hKGAFzPIZW', 'USER', 1),
('admin@globex.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qLlLXOY6hKGAFzPIZW', 'ADMIN', 2),
('user@globex.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qLlLXOY6hKGAFzPIZW', 'USER', 2),
('admin@initech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qLlLXOY6hKGAFzPIZW', 'ADMIN', 3);

-- 4. Create sample tables in each tenant schema to test multi-tenancy
-- These will be created automatically by Hibernate, but you can create them manually:

-- For tenant_1 (Acme Corp)
CREATE TABLE IF NOT EXISTS tenant_1.tasks (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- For tenant_2 (Globex Industries)
CREATE TABLE IF NOT EXISTS tenant_2.tasks (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample data
INSERT INTO tenant_1.tasks (title, description, status) VALUES
('Setup Acme Project', 'Initialize the new project for Acme Corp', 'IN_PROGRESS'),
('Deploy to Production', 'Deploy Acme application to production', 'PENDING');

INSERT INTO tenant_2.tasks (title, description, status) VALUES
('Globex Analysis', 'Analyze requirements for Globex project', 'COMPLETED'),
('Update Documentation', 'Update Globex project documentation', 'PENDING');

-- 5. Verify setup
SELECT 'Users created:' as info, count(*) as count FROM public.users;
SELECT 'Tenant 1 tasks:' as info, count(*) as count FROM tenant_1.tasks;
SELECT 'Tenant 2 tasks:' as info, count(*) as count FROM tenant_2.tasks;

-- Show all users with their tenant assignments
SELECT id, email, role, tenant_id FROM public.users ORDER BY tenant_id, role;