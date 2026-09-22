package com.ahni.backend.service;

import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.dto.GraduationProgressResponse;
import com.ahni.backend.entity.Department;
import com.ahni.backend.entity.GraduationRequirement;
import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.StudentGrade;
import com.ahni.backend.entity.StudentMajor;
import com.ahni.backend.exception.GraduationRequirementNotFoundException;
import com.ahni.backend.exception.StudentNotFoundException;
import com.ahni.backend.repository.GraduationRequirementRepository;
import com.ahni.backend.repository.StudentGradeRepository;
import com.ahni.backend.repository.StudentMajorRepository;
import com.ahni.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GraduationProgressService {
    private static final Comparator<StudentMajor> MAJOR_TYPE_ORDER =
        Comparator.comparing(StudentMajor::getMajorType);

    private final StudentRepository studentRepository;
    private final StudentMajorRepository studentMajorRepository;
    private final GraduationRequirementRepository graduationRequirementRepository;
    private final StudentGradeRepository gradeRepository;

    public GraduationProgressService(
        StudentRepository studentRepository,
        StudentMajorRepository studentMajorRepository,
        GraduationRequirementRepository graduationRequirementRepository,
        StudentGradeRepository gradeRepository
    ) {
        this.studentRepository = studentRepository;
        this.studentMajorRepository = studentMajorRepository;
        this.graduationRequirementRepository = graduationRequirementRepository;
        this.gradeRepository = gradeRepository;
    }

    public List<GraduationProgressResponse> getProgress(UUID authUserId) {
        Student student = studentRepository.findByAuthUserId(authUserId)
            .orElseThrow(StudentNotFoundException::new);
        List<StudentGrade> grades = gradeRepository.findAllByStudent(student);

        return studentMajorRepository.findAllByStudentAndDeletedAtIsNull(student).stream()
            .sorted(MAJOR_TYPE_ORDER)
            .map(major -> progress(student, major, grades))
            .toList();
    }

    private GraduationProgressResponse progress(
        Student student,
        StudentMajor major,
        List<StudentGrade> grades
    ) {
        GraduationRequirement requirement = graduationRequirementRepository
            .findByDepartmentAndAdmissionYearAndMajorType(
                major.getDepartment(),
                student.getAdmissionYear(),
                major.getMajorType()
            )
            .orElseThrow(GraduationRequirementNotFoundException::new);
        Department department = requirement.getDepartment();

        return new GraduationProgressResponse(
            requirement.getEntityId(),
            requirement.getAdmissionYear(),
            requirement.getMajorType().name(),
            new DepartmentResponse(department.getEntityId(), department.getName()),
            GraduationProgressCalculator.calculate(requirement, grades)
        );
    }
}
