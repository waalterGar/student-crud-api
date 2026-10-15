INSERT INTO courses (code, title, credits, max_capacity, current_enrolled, status)
VALUES
    ('CS101', 'Software Engineering Principles', 6, 30, 2, 'ACTIVE'),
    ('CS102', 'Data Structures & Algorithms', 6, 25, 1, 'ACTIVE'),
    ('MATH201', 'Linear Algebra for Engineers', 4, 20, 1, 'ACTIVE'),
    ('DB301', 'Database Systems & SQL Design', 5, 15, 0, 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO enrollments (student_id, course_id, status, grade, enrolled_at)
VALUES
    ((SELECT id FROM students WHERE email = 'carlos.mendoza@example.com'),
     (SELECT id FROM courses WHERE code = 'CS101'),
     'COMPLETED', 8.50, CURRENT_TIMESTAMP - INTERVAL '30 days'),

    ((SELECT id FROM students WHERE email = 'carlos.mendoza@example.com'),
     (SELECT id FROM courses WHERE code = 'MATH201'),
     'ACTIVE', NULL, CURRENT_TIMESTAMP - INTERVAL '10 days'),

    ((SELECT id FROM students WHERE email = 'ana.garcia@example.com'),
     (SELECT id FROM courses WHERE code = 'CS101'),
     'ACTIVE', NULL, CURRENT_TIMESTAMP - INTERVAL '15 days'),

    ((SELECT id FROM students WHERE email = 'lucia.fernandez@example.com'),
     (SELECT id FROM courses WHERE code = 'CS102'),
     'COMPLETED', 9.10, CURRENT_TIMESTAMP - INTERVAL '20 days')
ON CONFLICT (student_id, course_id) DO NOTHING;