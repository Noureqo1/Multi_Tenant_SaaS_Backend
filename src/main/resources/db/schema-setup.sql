-- =============================================================================
-- WorkHub Multi-Tenant Schema Setup Script for PostgreSQL
-- =============================================================================
-- This script creates tenant schemas and replicates the table structure.
-- Run this script as a superuser or the database owner.
-- =============================================================================

-- Create the database (run separately if needed)
-- CREATE DATABASE workhubdb;

-- =============================================================================
-- PUBLIC SCHEMA (Shared/Common Data)
-- =============================================================================
-- The public schema contains shared data like tenant registry, 
-- global configurations, and authentication data.

-- Tenant registry table to track all tenants
CREATE TABLE IF NOT EXISTS public.tenants (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    schema_name VARCHAR(63) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- =============================================================================
-- FUNCTION: Create a new tenant schema
-- =============================================================================
-- This function creates a new schema for a tenant and copies the table
-- structure from the public schema template.

CREATE OR REPLACE FUNCTION create_tenant_schema(tenant_id INTEGER, tenant_name VARCHAR)
RETURNS VARCHAR AS $$
DECLARE
    schema_name VARCHAR(63);
BEGIN
    -- Generate schema name
    schema_name := 'tenant_' || tenant_id;
    
    -- Create the schema
    EXECUTE 'CREATE SCHEMA IF NOT EXISTS ' || quote_ident(schema_name);
    
    -- Register the tenant
    INSERT INTO public.tenants (id, name, schema_name) 
    VALUES (tenant_id, tenant_name, schema_name)
    ON CONFLICT (id) DO UPDATE SET name = tenant_name;
    
    RETURN schema_name;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- EXAMPLE: Create tenant schemas
-- =============================================================================
-- Uncomment and run these to create sample tenants:

-- SELECT create_tenant_schema(1, 'Acme Corporation');
-- SELECT create_tenant_schema(2, 'Globex Industries');
-- SELECT create_tenant_schema(3, 'Initech Ltd');

-- =============================================================================
-- VERIFY SETUP
-- =============================================================================
-- List all schemas:
-- SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'tenant_%';

-- List all tenants:
-- SELECT * FROM public.tenants;
