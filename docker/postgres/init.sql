-- Este script se ejecuta automáticamente cuando PostgreSQL arranca por primera vez.
-- El usuario y la base de datos ya fueron creados por las variables de entorno
-- POSTGRES_USER / POSTGRES_DB del contenedor. Solo necesitamos los permisos.

GRANT ALL PRIVILEGES ON DATABASE novobanco TO novobanco;
GRANT ALL ON SCHEMA public TO novobanco;
GRANT CREATE ON SCHEMA public TO novobanco;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO novobanco;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO novobanco;
