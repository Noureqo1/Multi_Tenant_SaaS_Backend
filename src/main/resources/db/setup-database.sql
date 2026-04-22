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

-- If table already exists from older versions, ensure required columns exist.
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS password VARCHAR(255);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS role VARCHAR(50);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'tenantid'
    ) THEN
        EXECUTE 'UPDATE public.users SET tenant_id = tenantid WHERE tenant_id IS NULL';
        -- Legacy schema compatibility: tenantid may be NOT NULL while inserts use tenant_id.
        EXECUTE 'ALTER TABLE public.users ALTER COLUMN tenantid SET DEFAULT 1';
    END IF;
END $$;

-- Enforce expected defaults/constraints for app compatibility.
ALTER TABLE public.users ALTER COLUMN role SET DEFAULT 'TENANT_USER';
UPDATE public.users SET role = 'TENANT_USER' WHERE role IS NULL;
UPDATE public.users SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE public.users ALTER COLUMN email SET NOT NULL;
ALTER TABLE public.users ALTER COLUMN password SET NOT NULL;
ALTER TABLE public.users ALTER COLUMN role SET NOT NULL;
ALTER TABLE public.users ALTER COLUMN tenant_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email ON public.users (email);

-- 3. Insert test users 
-- Password for all users: "password123" (BCrypt hashed)
INSERT INTO public.users (email, password, role, tenant_id) VALUES
('admin@acme.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qLlLXOY6hKGAFzPIZW', 'TENANT_ADMIN', 1),
('user@acme.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qLlLXOY6hKGAFzPIZW', 'TENANT_USER', 1),
('admin@globex.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qLlLXOY6hKGAFzPIZW', 'TENANT_ADMIN', 2),
('user@globex.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qLlLXOY6hKGAFzPIZW', 'TENANT_USER', 2),
('admin@initech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qLlLXOY6hKGAFzPIZW', 'TENANT_ADMIN', 3)
ON CONFLICT (email) DO UPDATE
SET password = EXCLUDED.password,
    role = EXCLUDED.role,
    tenant_id = EXCLUDED.tenant_id;

-- Keep legacy tenantid column synchronized if it exists.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'tenantid'
    ) THEN
        EXECUTE 'UPDATE public.users SET tenantid = tenant_id WHERE tenantid IS DISTINCT FROM tenant_id';
    END IF;
END $$;

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