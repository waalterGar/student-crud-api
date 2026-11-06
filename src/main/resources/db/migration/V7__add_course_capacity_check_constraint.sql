ALTER TABLE courses
    ADD CONSTRAINT chk_courses_current_enrolled
    CHECK (current_enrolled >= 0 AND current_enrolled <= max_capacity);