INSERT INTO students (first_name, last_name, email, age)
VALUES
    ('Carlos', 'Mendoza', 'carlos.mendoza@example.com', 22),
    ('Ana', 'García', 'ana.garcia@example.com', 20),
    ('Lucía', 'Fernández', 'lucia.fernandez@example.com', 25),
    ('Mateo', 'Torres', 'mateo.torres@example.com', 21)
ON CONFLICT (email) DO NOTHING;