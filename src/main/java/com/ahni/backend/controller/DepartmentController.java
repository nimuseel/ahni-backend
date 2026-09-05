package com.ahni.backend.controller;

import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Operation(
        summary = "학과 목록 조회",
        description = "[인증 X] 가입 화면에서 사용할 학과 목록을 조회합니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "학과 목록 조회 성공",
        content = @Content(
            mediaType = "application/json",
            array = @ArraySchema(
                schema = @Schema(
                    implementation = DepartmentResponse.class
                )
            ),
            examples = @ExampleObject(
                value = """
                    [
                        {
                            "entityId": "00000000-0000-0000-0000-000000000001",
                            "name": "금융투자학과"
                        }
                    ]
                    """
            )
        )
    )
    @GetMapping
    public List<DepartmentResponse> getDepartments() {
        return departmentService.getDepartments();
    }
}
