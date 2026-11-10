ALTER TABLE courses
    ADD COLUMN closed_reason VARCHAR(20);

UPDATE courses
SET closed_reason = CASE
    WHEN current_enrolled >= max_capacity THEN 'CAPACITY'
    ELSE 'MANUAL'
END
WHERE status = 'CLOSED';

ALTER TABLE courses
    ADD CONSTRAINT chk_courses_closed_reason
    CHECK (closed_reason IS NULL OR closed_reason IN ('CAPACITY', 'MANUAL'));