package com.ahni.backend.service;

import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.dto.GradeCourseResponse;
import com.ahni.backend.dto.GradeRegistrationRequest;
import com.ahni.backend.dto.GradeResponse;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Department;
import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.StudentGrade;
import com.ahni.backend.exception.CourseNotFoundException;
import com.ahni.backend.exception.GradeAlreadyRegisteredException;
import com.ahni.backend.exception.InvalidGradeException;
import com.ahni.backend.exception.StudentNotFoundException;
import com.ahni.backend.repository.CourseRepository;
import com.ahni.backend.repository.StudentGradeRepository;
import com.ahni.backend.repository.StudentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GradeService {
    private static final Comparator<StudentGrade> NEWEST_FIRST =
        Comparator.comparingInt(StudentGrade::getAcademicYear).reversed()
            .thenComparing(
                grade -> grade.getTerm().sequence(),
                Comparator.reverseOrder()
            )
            .thenComparing(grade -> grade.getCourse().getCode());

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final StudentGradeRepository gradeRepository;

    public GradeService(
        StudentRepository studentRepository,
        CourseRepository courseRepository,
        StudentGradeRepository gradeRepository
    ) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.gradeRepository = gradeRepository;
    }

    @Transactional
    public GradeResponse register(
        UUID authUserId,
        GradeRegistrationRequest request
    ) {
        Student student = findStudent(authUserId);
        Course course = courseRepository
            .findByEntityIdAndActiveTrue(request.courseEntityId())
            .orElseThrow(CourseNotFoundException::new);

        if (gradeRepository.existsByStudentAndCourseAndAcademicYearAndTerm(
            student,
            course,
            request.academicYear(),
            request.term()
        )) {
            throw new GradeAlreadyRegisteredException();
        }

        StudentGrade grade;
        try {
            grade = new StudentGrade(
                student,
                course,
                request.academicYear(),
                request.term(),
                request.gradeCode(),
                request.credit(),
                request.rpl(),
                request.retake()
            );
        } catch (IllegalArgumentException exception) {
            throw new InvalidGradeException(exception.getMessage());
        }

        try {
            return toResponse(gradeRepository.saveAndFlush(grade));
        } catch (DataIntegrityViolationException exception) {
            throw new GradeAlreadyRegisteredException();
        }
    }

    public List<GradeResponse> getGrades(UUID authUserId) {
        Student student = findStudent(authUserId);

        return gradeRepository.findAllByStudent(student).stream()
            .sorted(NEWEST_FIRST)
            .map(GradeService::toResponse)
            .toList();
    }

    private Student findStudent(UUID authUserId) {
        return studentRepository.findByAuthUserId(authUserId)
            .orElseThrow(StudentNotFoundException::new);
    }

    private static GradeResponse toResponse(StudentGrade grade) {
        Course course = grade.getCourse();
        Department department = course.getDepartment();
        DepartmentResponse departmentResponse = department == null
            ? null
            : new DepartmentResponse(department.getEntityId(), department.getName());
        GradeCourseResponse courseResponse = new GradeCourseResponse(
            course.getEntityId(),
            course.getCode(),
            course.getName(),
            course.getCategory(),
            departmentResponse
        );

        return new GradeResponse(
            grade.getEntityId(),
            courseResponse,
            grade.getAcademicYear(),
            grade.getTerm(),
            grade.getGradeCode(),
            grade.getGradePoint(),
            grade.getCredit(),
            grade.isRpl(),
            grade.isRetake(),
            grade.getCreatedAt(),
            grade.getUpdatedAt()
        );
    }
}
