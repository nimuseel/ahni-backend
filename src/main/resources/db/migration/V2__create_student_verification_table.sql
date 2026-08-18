CREATE TABLE student_verification (
    student_verification_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id uuid NOT NULL,
    verification_type varchar(40) NOT NULL,
    provider varchar(100) NOT NULL,
    status varchar(20) NOT NULL,
    external_reference varchar(255),
    requested_at timestamptz NOT NULL DEFAULT now(),
    verified_at timestamptz,
    expires_at timestamptz,
    failure_reason text,
    CONSTRAINT fk_student_verification_student
        FOREIGN KEY (student_id) REFERENCES student (student_id),
    CONSTRAINT ck_student_verification_type
        CHECK (verification_type IN ('ENROLLMENT_CERTIFICATE', 'ACADEMIC_RECORD')),
    CONSTRAINT ck_student_verification_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED'))
);

CREATE INDEX idx_student_verification_student_id
    ON student_verification (student_id);
