ALTER TABLE enrollments DROP CONSTRAINT IF EXISTS uk_student_course;

CREATE UNIQUE INDEX uk_enrollments_active_student_course
ON enrollments (student_id, course_id)
WHERE status = 'ACTIVE';