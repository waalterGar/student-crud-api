INSERT INTO roles (name, description)
VALUES
    ('ROLE_ADMIN', 'Full system administration and unrestricted resource access'),
    ('ROLE_INSTRUCTOR', 'Academic management: view courses/students and grade enrollments'),
    ('ROLE_STUDENT', 'Self-service access restricted to own linked student profile')
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (email, password, enabled, student_id)
VALUES
    ('admin@students.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE, NULL),
    ('instructor@students.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE, NULL),
    ('carlos.mendoza@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE,
        (SELECT id FROM students WHERE email = 'carlos.mendoza@example.com')),
    ('ana.garcia@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE,
        (SELECT id FROM students WHERE email = 'ana.garcia@example.com'))
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
VALUES
    ((SELECT id FROM users WHERE email = 'admin@students.com'),
     (SELECT id FROM roles WHERE name = 'ROLE_ADMIN')),

    ((SELECT id FROM users WHERE email = 'instructor@students.com'),
     (SELECT id FROM roles WHERE name = 'ROLE_INSTRUCTOR')),

    ((SELECT id FROM users WHERE email = 'carlos.mendoza@example.com'),
     (SELECT id FROM roles WHERE name = 'ROLE_STUDENT')),

    ((SELECT id FROM users WHERE email = 'ana.garcia@example.com'),
     (SELECT id FROM roles WHERE name = 'ROLE_STUDENT'))
ON CONFLICT (user_id, role_id) DO NOTHING;