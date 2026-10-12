-- Add full_name and enabled columns to users table
SET @full_name_ddl = IF(
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'users'
          AND column_name = 'full_name'
    ),
    'SELECT 1',
    'ALTER TABLE users ADD COLUMN full_name VARCHAR(100) NULL AFTER username'
);
PREPARE add_full_name_stmt FROM @full_name_ddl;
EXECUTE add_full_name_stmt;
DEALLOCATE PREPARE add_full_name_stmt;

SET @enabled_ddl = IF(
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'users'
          AND column_name = 'enabled'
    ),
    'SELECT 1',
    'ALTER TABLE users ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER role'
);
PREPARE add_enabled_stmt FROM @enabled_ddl;
EXECUTE add_enabled_stmt;
DEALLOCATE PREPARE add_enabled_stmt;

-- Update default role from USER to VIEWER for consistency
UPDATE users SET role = 'VIEWER' WHERE role = 'USER';

-- Ensure admin user has ADMIN role
UPDATE users SET role = 'ADMIN', enabled = 1 WHERE username = 'admin';
