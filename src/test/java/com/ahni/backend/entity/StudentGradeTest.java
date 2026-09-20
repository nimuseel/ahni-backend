package com.ahni.backend.entity;

import com.ahni.backend.domain.AcademicTerm;
import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.EnrollmentStatus;
import com.ahni.backend.domain.GradeCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Year;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class StudentGradeTest {
    private final Department department = new Department("소프트웨어융합공학과");
    private final Student student = new Student(
        UUID.randomUUID(),
        "student@inha.edu",
        2024,
        EnrollmentStatus.ENROLLED,
        "인하"
    );
    private final Course course = new Course(
        department,
        "CSE101",
        "프로그래밍 기초",
        new BigDecimal("3.0"),
        CourseCategory.MAJOR
    );

    @ParameterizedTest
    @MethodSource("letterGradeCases")
    void 등급으로_평점_스냅샷을_생성한다(GradeCode code, String point) {
        StudentGrade grade = createGrade(code, new BigDecimal("3.0"), false, false);

        assertThat(grade.getGradePoint()).isEqualByComparingTo(point);
    }

    static Stream<Arguments> letterGradeCases() {
        return Stream.of(
            arguments(GradeCode.A_PLUS, "4.50"),
            arguments(GradeCode.A_ZERO, "4.00"),
            arguments(GradeCode.B_PLUS, "3.50"),
            arguments(GradeCode.B_ZERO, "3.00"),
            arguments(GradeCode.C_PLUS, "2.50"),
            arguments(GradeCode.C_ZERO, "2.00"),
            arguments(GradeCode.D_PLUS, "1.50"),
            arguments(GradeCode.D_ZERO, "1.00"),
            arguments(GradeCode.F, "0.00")
        );
    }

    @ParameterizedTest
    @MethodSource("passFailGradeCases")
    void 이수_여부_등급은_평점에서_제외한다(GradeCode code) {
        StudentGrade grade = createGrade(code, new BigDecimal("2.0"), false, false);

        assertThat(grade.getGradePoint()).isNull();
    }

    static Stream<GradeCode> passFailGradeCases() {
        return Stream.of(GradeCode.P, GradeCode.NP);
    }

    @Test
    void RPL은_등급과_평점_없이_생성한다() {
        StudentGrade grade = createGrade(null, new BigDecimal("3.0"), true, false);

        assertThat(grade.getGradeCode()).isNull();
        assertThat(grade.getGradePoint()).isNull();
        assertThat(grade.isRpl()).isTrue();
    }

    @Test
    void RPL에는_등급을_입력할_수_없다() {
        assertThatThrownBy(() -> createGrade(
            GradeCode.A_PLUS,
            new BigDecimal("3.0"),
            true,
            false
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 일반_성적에는_등급이_필수다() {
        assertThatThrownBy(() -> createGrade(
            null,
            new BigDecimal("3.0"),
            false,
            false
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 학생은_필수다() {
        assertThatThrownBy(() -> new StudentGrade(
            null,
            course,
            2025,
            AcademicTerm.SECOND,
            GradeCode.A_PLUS,
            new BigDecimal("3.0"),
            false,
            (StudentGrade) null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 과목은_필수다() {
        assertThatThrownBy(() -> new StudentGrade(
            student,
            null,
            2025,
            AcademicTerm.SECOND,
            GradeCode.A_PLUS,
            new BigDecimal("3.0"),
            false,
            (StudentGrade) null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 학기는_필수다() {
        assertThatThrownBy(() -> new StudentGrade(
            student,
            course,
            2025,
            null,
            GradeCode.A_PLUS,
            new BigDecimal("3.0"),
            false,
            (StudentGrade) null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 수강연도는_2000년부터_현재_한국연도까지_허용한다() {
        int currentYear = Year.now(ZoneId.of("Asia/Seoul")).getValue();

        StudentGrade firstBoundary = createGrade(2000);
        StudentGrade currentBoundary = createGrade(currentYear);

        assertThat(firstBoundary.getAcademicYear()).isEqualTo(2000);
        assertThat(currentBoundary.getAcademicYear()).isEqualTo(currentYear);
    }

    @Test
    void 범위를_벗어난_수강연도는_허용하지_않는다() {
        int nextYear = Year.now(ZoneId.of("Asia/Seoul")).getValue() + 1;

        assertThatThrownBy(() -> createGrade(1999))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> createGrade(nextYear))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.1", "30.0"})
    void 허용_범위의_학점을_저장한다(String credit) {
        StudentGrade grade = createGrade(
            GradeCode.A_PLUS,
            new BigDecimal(credit),
            false,
            false
        );

        assertThat(grade.getCredit()).isEqualByComparingTo(credit);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0", "-0.1", "30.1", "3.25"})
    void 유효하지_않은_학점은_허용하지_않는다(String credit) {
        assertThatThrownBy(() -> createGrade(
            GradeCode.A_PLUS,
            new BigDecimal(credit),
            false,
            false
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 학점은_필수다() {
        assertThatThrownBy(() -> createGrade(GradeCode.A_PLUS, null, false, false))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 재수강_여부를_기록한다() {
        StudentGrade grade = createGrade(
            GradeCode.B_PLUS,
            new BigDecimal("3.0"),
            false,
            true
        );

        assertThat(grade.isRetake()).isTrue();
    }

    @Test
    void 재수강은_같은_과목의_이전_성적을_명시적으로_연결한다() {
        StudentGrade previous = new StudentGrade(
            student,
            course,
            2024,
            AcademicTerm.SECOND,
            GradeCode.C_PLUS,
            new BigDecimal("3.0"),
            false,
            (StudentGrade) null
        );

        StudentGrade retake = new StudentGrade(
            student,
            course,
            2025,
            AcademicTerm.FIRST,
            GradeCode.A_ZERO,
            new BigDecimal("3.0"),
            false,
            previous
        );

        assertThat(retake.getReplacedGrade()).isSameAs(previous);
        assertThat(retake.isRetake()).isTrue();
    }

    @Test
    void 다른_학생의_성적은_재수강_대상으로_연결할_수_없다() {
        Student otherStudent = new Student(
            UUID.randomUUID(),
            "other@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "다른 학생"
        );
        StudentGrade previous = grade(
            otherStudent,
            course,
            2024,
            AcademicTerm.SECOND,
            false,
            null
        );

        assertThatThrownBy(() -> grade(
            student,
            course,
            2025,
            AcademicTerm.FIRST,
            false,
            previous
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("같은 학생의 성적만 재수강 대상으로 선택할 수 있습니다.");
    }

    @Test
    void 다른_과목의_성적은_재수강_대상으로_연결할_수_없다() {
        Course otherCourse = new Course(
            department,
            "CSE201",
            "자료구조",
            new BigDecimal("3.0"),
            CourseCategory.MAJOR
        );
        StudentGrade previous = grade(
            student,
            otherCourse,
            2024,
            AcademicTerm.SECOND,
            false,
            null
        );

        assertThatThrownBy(() -> grade(
            student,
            course,
            2025,
            AcademicTerm.FIRST,
            false,
            previous
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("같은 과목의 성적만 재수강 대상으로 선택할 수 있습니다.");
    }

    @ParameterizedTest
    @MethodSource("notEarlierReplacementCases")
    void 이전_학기의_성적만_재수강_대상으로_연결할_수_있다(
        int previousYear,
        AcademicTerm previousTerm
    ) {
        StudentGrade previous = grade(
            student,
            course,
            previousYear,
            previousTerm,
            false,
            null
        );

        assertThatThrownBy(() -> grade(
            student,
            course,
            2025,
            AcademicTerm.FIRST,
            false,
            previous
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이전 학기의 성적만 재수강 대상으로 선택할 수 있습니다.");
    }

    static Stream<Arguments> notEarlierReplacementCases() {
        return Stream.of(
            arguments(2025, AcademicTerm.FIRST),
            arguments(2025, AcademicTerm.SECOND)
        );
    }

    @Test
    void RPL은_재수강_관계에_포함할_수_없다() {
        StudentGrade regular = grade(
            student,
            course,
            2024,
            AcademicTerm.SECOND,
            false,
            null
        );
        StudentGrade rpl = grade(
            student,
            course,
            2024,
            AcademicTerm.FIRST,
            true,
            null
        );

        assertThatThrownBy(() -> grade(
            student,
            course,
            2025,
            AcademicTerm.FIRST,
            true,
            regular
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("RPL 성적은 재수강 관계에 포함할 수 없습니다.");
        assertThatThrownBy(() -> grade(
            student,
            course,
            2025,
            AcademicTerm.FIRST,
            false,
            rpl
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("RPL 성적은 재수강 관계에 포함할 수 없습니다.");
    }

    @Test
    void 성적_정보를_수정한다() {
        StudentGrade grade = createGrade(
            GradeCode.B_PLUS,
            new BigDecimal("3.0"),
            false,
            false
        );

        grade.update(
            2024,
            AcademicTerm.WINTER,
            GradeCode.A_ZERO,
            new BigDecimal("2.0"),
            false,
            (StudentGrade) null
        );

        assertThat(grade.getAcademicYear()).isEqualTo(2024);
        assertThat(grade.getTerm()).isEqualTo(AcademicTerm.WINTER);
        assertThat(grade.getGradeCode()).isEqualTo(GradeCode.A_ZERO);
        assertThat(grade.getGradePoint()).isEqualByComparingTo("4.00");
        assertThat(grade.getCredit()).isEqualByComparingTo("2.0");
        assertThat(grade.isRpl()).isFalse();
        assertThat(grade.isRetake()).isFalse();
    }

    @Test
    void 성적_수정으로_이전_성적과_재수강_관계를_설정한다() {
        StudentGrade previous = grade(
            student,
            course,
            2024,
            AcademicTerm.SECOND,
            false,
            null
        );
        StudentGrade grade = grade(
            student,
            course,
            2025,
            AcademicTerm.SECOND,
            false,
            null
        );

        grade.update(
            2025,
            AcademicTerm.SECOND,
            GradeCode.A_PLUS,
            new BigDecimal("3.0"),
            false,
            previous
        );

        assertThat(grade.getReplacedGrade()).isSameAs(previous);
        assertThat(grade.isRetake()).isTrue();
    }

    @Test
    void 자기_자신을_재수강_대상으로_설정할_수_없다() {
        StudentGrade grade = grade(
            student,
            course,
            2024,
            AcademicTerm.SECOND,
            false,
            null
        );

        assertThatThrownBy(() -> grade.update(
            2025,
            AcademicTerm.FIRST,
            GradeCode.A_PLUS,
            new BigDecimal("3.0"),
            false,
            grade
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("자기 자신의 성적은 재수강 대상으로 선택할 수 없습니다.");
    }

    @Test
    void 수정한_RPL은_등급과_평점이_없다() {
        StudentGrade grade = createGrade(
            GradeCode.A_PLUS,
            new BigDecimal("3.0"),
            false,
            false
        );

        grade.update(
            2025,
            AcademicTerm.SECOND,
            null,
            new BigDecimal("3.0"),
            true,
            (StudentGrade) null
        );

        assertThat(grade.getGradeCode()).isNull();
        assertThat(grade.getGradePoint()).isNull();
        assertThat(grade.isRpl()).isTrue();
    }

    @Test
    void 유효하지_않은_수정은_기존_성적을_변경하지_않는다() {
        StudentGrade grade = createGrade(
            GradeCode.B_PLUS,
            new BigDecimal("3.0"),
            false,
            false
        );

        assertThatThrownBy(() -> grade.update(
            2024,
            AcademicTerm.FIRST,
            GradeCode.A_PLUS,
            new BigDecimal("2.0"),
            true,
            (StudentGrade) null
        )).isInstanceOf(IllegalArgumentException.class);

        assertThat(grade.getAcademicYear()).isEqualTo(2025);
        assertThat(grade.getTerm()).isEqualTo(AcademicTerm.SECOND);
        assertThat(grade.getGradeCode()).isEqualTo(GradeCode.B_PLUS);
        assertThat(grade.getGradePoint()).isEqualByComparingTo("3.50");
        assertThat(grade.getCredit()).isEqualByComparingTo("3.0");
        assertThat(grade.isRpl()).isFalse();
        assertThat(grade.isRetake()).isFalse();
    }

    private StudentGrade createGrade(
        GradeCode gradeCode,
        BigDecimal credit,
        boolean rpl,
        boolean retake
    ) {
        StudentGrade replacedGrade = retake
            ? grade(
                student,
                course,
                2024,
                AcademicTerm.SECOND,
                false,
                null
            )
            : null;
        return new StudentGrade(
            student,
            course,
            2025,
            AcademicTerm.SECOND,
            gradeCode,
            credit,
            rpl,
            replacedGrade
        );
    }

    private StudentGrade createGrade(int academicYear) {
        return new StudentGrade(
            student,
            course,
            academicYear,
            AcademicTerm.FIRST,
            GradeCode.A_ZERO,
            new BigDecimal("3.0"),
            false,
            (StudentGrade) null
        );
    }

    private StudentGrade grade(
        Student owner,
        Course targetCourse,
        int academicYear,
        AcademicTerm term,
        boolean rpl,
        StudentGrade replacedGrade
    ) {
        return new StudentGrade(
            owner,
            targetCourse,
            academicYear,
            term,
            rpl ? null : GradeCode.A_ZERO,
            new BigDecimal("3.0"),
            rpl,
            replacedGrade
        );
    }
}
