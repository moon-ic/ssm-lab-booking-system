package com.lab.booking;

import com.lab.booking.config.DatabaseBootstrap;
import com.lab.booking.infrastructure.cache.AppCacheService;
import com.lab.booking.repository.AuthRepository;
import com.lab.booking.service.SystemTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/lab_booking_test?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai",
        "spring.datasource.username=root",
        "spring.datasource.password=200453",
        "app.redis.enabled=false"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminServerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SystemTaskService systemTaskService;

    @Autowired
    private DatabaseBootstrap databaseBootstrap;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private AppCacheService cacheService;

    @BeforeEach
    void resetDatabase() {
        databaseBootstrap.resetDatabase();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void redisModuleFallsBackToInMemoryStorageWhenDisabled() throws Exception {
        if (!"MEMORY".equals(authRepository.tokenStorageType())) {
            throw new IllegalStateException("Expected in-memory token store when Redis is disabled");
        }
        if (!"MEMORY".equals(cacheService.storageType())) {
            throw new IllegalStateException("Expected in-memory cache when Redis is disabled");
        }

        String token = loginAndGetToken("20230001", "0000");
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.jobNoOrStudentNo").value("20230001"));
    }

    @Test
    void loginMeAndChangePasswordFlowWorks() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "20230001",
                                  "password": "0000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userInfo.roleCode").value("STUDENT"))
                .andExpect(jsonPath("$.data.firstLoginRequired").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.jobNoOrStudentNo").value("20230001"));

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldPassword": "0000",
                                  "newPassword": "abc000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "20230001",
                                  "password": "abc000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void firstLoginUserMustChangePasswordBeforeAccessingOtherModules() throws Exception {
        String teacherToken = loginAndGetToken("T2026001", "0000");

        String createResponse = mockMvc.perform(post("/api/users/students")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "First Login Student",
                                  "studentNo": "20239998",
                                  "phone": "13800001008"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.firstLoginRequired").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userId = extractField(createResponse, "\"userId\":(\\d+)");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "20239998",
                                  "password": "0000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.firstLoginRequired").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studentToken = extractField(loginResponse, "\"token\":\"([^\"]+)\"");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstLoginRequired").value(true));

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldPassword": "0000",
                                  "newPassword": "abc000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String changedToken = loginAndGetToken("20239998", "abc000000");

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + changedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(Long.parseLong(userId)));
    }

    @Test
    void resetPasswordForcesFirstLoginAndCreatesMessage() throws Exception {
        String adminToken = loginAndGetToken("A001", "0000");

        mockMvc.perform(put("/api/users/4/reset-password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newPassword": "0000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String studentLoginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "20230001",
                                  "password": "0000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstLoginRequired").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studentToken = extractField(studentLoginResponse, "\"token\":\"([^\"]+)\"");

        mockMvc.perform(get("/api/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("userId", "4")
                        .param("confirmStatus", "UNCONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("userId", "4")
                        .param("type", "PASSWORD_RESET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].type").value("PASSWORD_RESET"));
    }

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void healthEndpointWorks() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void superAdminCanCreateAdminAndAdminCanCreateTeacherAndTeacherCanCreateStudent() throws Exception {
        String superAdminToken = loginAndGetToken("SA001", "0000");
        mockMvc.perform(post("/api/users/admins")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "管理员A",
                                  "account": "admin_a",
                                  "phone": "13800001000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.roleCode").value("ADMIN"))
                .andExpect(jsonPath("$.data.account").value("admin_a"));

        String adminToken = loginAndGetToken("A001", "0000");
        mockMvc.perform(post("/api/users/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "教师A",
                                  "jobNo": "T2026999",
                                  "phone": "13800001001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.roleCode").value("TEACHER"))
                .andExpect(jsonPath("$.data.jobNoOrStudentNo").value("T2026999"));

        String teacherToken = loginAndGetToken("T2026001", "0000");
        mockMvc.perform(post("/api/users/students")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "学生A",
                                  "studentNo": "20239999",
                                  "phone": "13800001002"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.roleCode").value("STUDENT"))
                .andExpect(jsonPath("$.data.jobNoOrStudentNo").value("20239999"));
    }

    @Test
    void teacherCanOnlySeeManagedStudents() throws Exception {
        String teacherToken = loginAndGetToken("T2026001", "0000");
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].roleCode").value("STUDENT"))
                .andExpect(jsonPath("$.data.list[0].jobNoOrStudentNo").value("20230001"));
    }

    @Test
    void teacherCanViewManagedStudentDetail() throws Exception {
        String teacherToken = loginAndGetToken("T2026001", "0000");

        mockMvc.perform(get("/api/users/4")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(4))
                .andExpect(jsonPath("$.data.roleCode").value("STUDENT"));
    }

    @Test
    void teacherCanDeleteManagedStudentAndDeletedStudentCannotLogin() throws Exception {
        String teacherToken = loginAndGetToken("T2026001", "0000");

        mockMvc.perform(delete("/api/users/students/4")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "20230001",
                                  "password": "0000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminCanDisableTeacherAndDisabledTeacherCannotLogin() throws Exception {
        String adminToken = loginAndGetToken("A001", "0000");

        mockMvc.perform(put("/api/users/3/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "T2026001",
                                  "password": "0000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void superAdminCanManageRolesAndPermissions() throws Exception {
        String superAdminToken = loginAndGetToken("SA001", "0000");

        mockMvc.perform(get("/api/roles")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].roleCode").value("SUPER_ADMIN"));

        String createResponse = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName": "Lab Assistant",
                                  "roleCode": "LAB_ASSISTANT",
                                  "remark": "Lab assistant role"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.roleCode").value("LAB_ASSISTANT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String roleId = extractField(createResponse, "\"roleId\":(\\d+)");

        mockMvc.perform(put("/api/roles/" + roleId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName": "Lab Manager",
                                  "roleCode": "LAB_ASSISTANT",
                                  "remark": "updated remark"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.roleName").value("Lab Manager"));

        mockMvc.perform(put("/api/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permissionIds": [1, 2],
                                  "menuIds": [201, 202]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.permissionIds[0]").value(1))
                .andExpect(jsonPath("$.data.menuIds[1]").value(202));

        mockMvc.perform(get("/api/roles/" + roleId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.roleCode").value("LAB_ASSISTANT"));

        mockMvc.perform(get("/api/permissions")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .param("type", "ACTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].type").value("ACTION"));
    }

    @Test
    void adminCannotAccessRolePermissionModule() throws Exception {
        String adminToken = loginAndGetToken("A001", "0000");

        mockMvc.perform(get("/api/roles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/api/permissions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void loggedInUserCanListAndViewDevices() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");

        mockMvc.perform(get("/api/devices")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("keyword", "Projector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].deviceCode").value("EQ-2026-0001"));

        mockMvc.perform(get("/api/devices/1001")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.deviceName").value("Projector A"))
                .andExpect(jsonPath("$.data.description").value("Portable projector"));
    }

    @Test
    void adminCanCreateUpdateAndDisableDevice() throws Exception {
        String adminToken = loginAndGetToken("A001", "0000");

        String createResponse = mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceName": "Tablet D",
                                  "deviceCode": "EQ-2026-0100",
                                  "category": "Mobile Device",
                                  "location": "Lab 201",
                                  "description": "Presentation tablet"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String deviceId = extractField(createResponse, "\"deviceId\":(\\d+)");

        mockMvc.perform(put("/api/devices/" + deviceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceName": "Tablet D Updated",
                                  "deviceCode": "EQ-2026-0100",
                                  "category": "Mobile Device",
                                  "location": "Lab 202",
                                  "description": "Updated presentation tablet"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.deviceName").value("Tablet D Updated"))
                .andExpect(jsonPath("$.data.location").value("Lab 202"));

        mockMvc.perform(put("/api/devices/" + deviceId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/devices/" + deviceId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }

    @Test
    void studentCannotCreateDeviceAndAdminCanImportDevice() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String adminToken = loginAndGetToken("A001", "0000");
        MockMultipartFile image = new MockMultipartFile("image", "device.png", "image/png", "fake-image".getBytes());

        mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceName": "Forbidden Device",
                                  "deviceCode": "EQ-2026-0999",
                                  "category": "Test Device",
                                  "location": "Lab 999",
                                  "description": "Students cannot create devices"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(multipart("/api/device-imports")
                        .file(image)
                        .param("deviceName", "Imported Camera")
                        .param("category", "Camera")
                        .param("location", "Lab 401")
                        .param("description", "Imported through form upload")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.deviceName").value("Imported Camera"))
                .andExpect(jsonPath("$.data.imageUrl").value("/uploads/devices/1004-device.png"));

        mockMvc.perform(multipart("/api/devices/import")
                        .file(new MockMultipartFile("image", "legacy.png", "image/png", "legacy-image".getBytes()))
                        .param("deviceName", "Legacy Imported Device")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.deviceCode").exists());
    }

    @Test
    void studentCanCreateReservationAndTeacherCanApproveIt() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");
        String adminToken = loginAndGetToken("A001", "0000");

        String createResponse = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": 1001,
                                  "startTime": "2026-03-29 09:00:00",
                                  "endTime": "2026-03-29 18:00:00",
                                  "purpose": "course demo"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.deviceId").value(1001))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String reservationId = extractField(createResponse, "\"reservationId\":(\\d+)");

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.list[0].applicantId").value(4));

        mockMvc.perform(put("/api/reservations/" + reservationId + "/approve")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "APPROVE",
                                  "comment": "approved"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewerId").value(3))
                .andExpect(jsonPath("$.data.reviewComment").value("approved"));

        mockMvc.perform(get("/api/devices/1001")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));

        mockMvc.perform(put("/api/reservations/" + reservationId + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "APPROVE",
                                  "comment": "final approved"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PICKUP_PENDING"));

        mockMvc.perform(get("/api/devices/1001")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESERVED"));
    }

    @Test
    void reservationConflictCancelAndExpireFlowWorks() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");
        String adminToken = loginAndGetToken("A001", "0000");

        String firstReservation = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": 1001,
                                  "startTime": "2026-03-30 09:00:00",
                                  "endTime": "2026-03-30 12:00:00",
                                  "purpose": "physics lab"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstReservationId = extractField(firstReservation, "\"reservationId\":(\\d+)");

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": 1001,
                                  "startTime": "2026-03-30 10:00:00",
                                  "endTime": "2026-03-30 11:00:00",
                                  "purpose": "conflict booking"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(put("/api/reservations/" + firstReservationId + "/cancel")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String secondReservation = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": 1001,
                                  "startTime": "2026-03-30 10:00:00",
                                  "endTime": "2026-03-30 11:00:00",
                                  "purpose": "retry booking"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondReservationId = extractField(secondReservation, "\"reservationId\":(\\d+)");

        mockMvc.perform(put("/api/reservations/" + secondReservationId + "/approve")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "APPROVE",
                                  "comment": "ok"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(put("/api/reservations/" + secondReservationId + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "APPROVE",
                                  "comment": "final ok"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PICKUP_PENDING"));

        mockMvc.perform(put("/api/reservations/" + secondReservationId + "/expire")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/reservations/" + secondReservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXPIRED"));

        mockMvc.perform(get("/api/devices/1001")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
    }

    @Test
    void studentVisibilityAndTeacherRejectRulesAreEnforced() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");
        String adminToken = loginAndGetToken("A001", "0000");

        String reservationResponse = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": 1001,
                                  "startTime": "2026-03-31 09:00:00",
                                  "endTime": "2026-03-31 10:00:00",
                                  "purpose": "seminar"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String reservationId = extractField(reservationResponse, "\"reservationId\":(\\d+)");

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("applicantId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(put("/api/reservations/" + reservationId + "/approve")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "REJECT",
                                  "comment": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(put("/api/reservations/" + reservationId + "/approve")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "REJECT",
                                  "comment": "time not suitable"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(put("/api/reservations/" + reservationId + "/expire")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void teacherApprovalStillRequiresAdminApprovalBeforePickup() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");
        String adminToken = loginAndGetToken("A001", "0000");

        String reservationResponse = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": 1001,
                                  "startTime": "2026-04-02 09:00",
                                  "endTime": "2026-04-02 18:00",
                                  "purpose": "two-step approval"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String reservationId = extractField(reservationResponse, "\"reservationId\":(\\d+)");

        mockMvc.perform(put("/api/reservations/" + reservationId + "/approve")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "APPROVE",
                                  "comment": "teacher approved"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/borrow-records")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(put("/api/reservations/" + reservationId + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "APPROVE",
                                  "comment": "admin approved"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PICKUP_PENDING"));
    }

    @Test
    void approvedReservationCreatesBorrowRecordAndStudentCanPickup() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");

        String reservationId = createAndApproveReservation(studentToken, teacherToken,
                "2026-04-02 09:00:00", "2026-04-02 18:00:00", "borrow test");

        String listResponse = mockMvc.perform(get("/api/borrow-records")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].reservationId").value(Integer.parseInt(reservationId)))
                .andExpect(jsonPath("$.data.list[0].status").value("PICKUP_PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String recordId = extractField(listResponse, "\"recordId\":(\\d+)");

        mockMvc.perform(put("/api/borrow-records/" + recordId + "/pickup")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("BORROWING"))
                .andExpect(jsonPath("$.data.pickupTime").isNotEmpty());

        mockMvc.perform(get("/api/devices/1001")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BORROWED"));
    }

    @Test
    void studentCanReturnBorrowedDeviceAndDeviceBecomesAvailable() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");

        createAndApproveReservation(studentToken, teacherToken,
                "2026-04-03 09:00:00", "2026-04-03 18:00:00", "return test");

        String recordId = extractField(mockMvc.perform(get("/api/borrow-records")
                        .header("Authorization", "Bearer " + studentToken))
                .andReturn()
                .getResponse()
                .getContentAsString(), "\"recordId\":(\\d+)");

        mockMvc.perform(put("/api/borrow-records/" + recordId + "/pickup")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BORROWING"));

        mockMvc.perform(put("/api/borrow-records/" + recordId + "/return")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "returnTime": "2026-04-03 17:30:00",
                                  "deviceCondition": "NORMAL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("RETURNED"))
                .andExpect(jsonPath("$.data.deviceCondition").value("NORMAL"));

        mockMvc.perform(get("/api/devices/1001")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
    }

    @Test
    void adminCanMarkBorrowRecordOverdueAndQueryReminderList() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");
        String adminToken = loginAndGetToken("A001", "0000");

        createAndApproveReservation(studentToken, teacherToken,
                "2026-04-04 09:00:00", "2026-04-04 18:00:00", "overdue test");

        String recordId = extractField(mockMvc.perform(get("/api/borrow-records")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn()
                .getResponse()
                .getContentAsString(), "\"recordId\":(\\d+)");

        mockMvc.perform(put("/api/borrow-records/" + recordId + "/pickup")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/borrow-records/" + recordId + "/overdue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/borrow-records")
                        .header("Authorization", "Bearer " + teacherToken)
                        .param("status", "OVERDUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].status").value("OVERDUE"));

        mockMvc.perform(get("/api/borrow-records/reminders")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("type", "OVERDUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].recordId").value(Integer.parseInt(recordId)))
                .andExpect(jsonPath("$.data[0].reminderType").value("OVERDUE"));
    }

    @Test
    void studentCanSubmitRepairAndTeacherCanSeeManagedRepairs() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");

        String repairResponse = mockMvc.perform(post("/api/repairs")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": 1001,
                                  "description": "device cannot start"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String repairId = extractField(repairResponse, "\"repairId\":(\\d+)");

        mockMvc.perform(get("/api/repairs")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].repairId").value(Integer.parseInt(repairId)))
                .andExpect(jsonPath("$.data.list[0].applicantId").value(4));

        mockMvc.perform(get("/api/repairs/" + repairId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.repairId").value(Integer.parseInt(repairId)))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(get("/api/devices/1001")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REPAIRING"));
    }

    @Test
    void adminCanUpdateRepairStatusAndDeviceStatusFollows() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String adminToken = loginAndGetToken("A001", "0000");

        String repairId = extractField(mockMvc.perform(post("/api/repairs")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": 1001,
                                  "description": "screen issue"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString(), "\"repairId\":(\\d+)");

        mockMvc.perform(put("/api/repairs/" + repairId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PROCESSING",
                                  "comment": "sent for repair"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.comment").value("sent for repair"));

        mockMvc.perform(put("/api/repairs/" + repairId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED",
                                  "comment": "fixed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/devices/1001")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
    }

    @Test
    void duplicateActiveRepairIsRejectedAndUnrepairableKeepsDamagedStatus() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String adminToken = loginAndGetToken("A001", "0000");

        String repairId = extractField(mockMvc.perform(post("/api/repairs")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": 1001,
                                  "description": "broken body"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString(), "\"repairId\":(\\d+)");

        mockMvc.perform(post("/api/repairs")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": 1001,
                                  "description": "duplicate request"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(put("/api/repairs/" + repairId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "UNREPAIRABLE",
                                  "comment": "cannot be fixed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNREPAIRABLE"));

        mockMvc.perform(get("/api/devices/1001")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DAMAGED"));
    }

    @Test
    void profileReturnsCurrentUserAndOwnBorrowRecords() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");

        createAndApproveReservation(studentToken, teacherToken,
                "2026-04-05 09:00:00", "2026-04-05 18:00:00", "profile test");

        String recordId = extractField(mockMvc.perform(get("/api/borrow-records")
                        .header("Authorization", "Bearer " + studentToken))
                .andReturn()
                .getResponse()
                .getContentAsString(), "\"recordId\":(\\d+)");

        mockMvc.perform(put("/api/borrow-records/" + recordId + "/pickup")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BORROWING"));

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(4))
                .andExpect(jsonPath("$.data.jobNoOrStudentNo").value("20230001"))
                .andExpect(jsonPath("$.data.creditScore").value(100));

        mockMvc.perform(get("/api/profile/borrow-records")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("status", "BORROWING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].userId").value(4))
                .andExpect(jsonPath("$.data.list[0].status").value("BORROWING"))
                .andExpect(jsonPath("$.data.list[0].deviceId").value(1001));
    }

    @Test
    void adminCanQueryStatisticsOverviewHotDamageAndViolations() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");
        String adminToken = loginAndGetToken("A001", "0000");

        createAndApproveReservation(studentToken, teacherToken,
                "2026-04-06 09:00:00", "2026-04-06 18:00:00", "stats borrow");

        String firstRecordId = extractField(mockMvc.perform(get("/api/borrow-records")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn()
                .getResponse()
                .getContentAsString(), "\"recordId\":(\\d+)");

        mockMvc.perform(put("/api/borrow-records/" + firstRecordId + "/pickup")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/borrow-records/" + firstRecordId + "/overdue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/borrow-records/" + firstRecordId + "/return")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "returnTime": "2026-04-07 10:00:00",
                                  "deviceCondition": "BROKEN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OVERDUE"));

        String newDeviceResponse = mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceName": "Stats Device",
                                  "deviceCode": "EQ-2026-0200",
                                  "category": "Testing",
                                  "location": "Lab 03",
                                  "description": "for statistics"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String newDeviceId = extractField(newDeviceResponse, "\"deviceId\":(\\d+)");

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": %s,
                                  "startTime": "2026-04-08 09:00:00",
                                  "endTime": "2026-04-08 10:00:00",
                                  "purpose": "pending reservation"
                                }
                                """.formatted(newDeviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(post("/api/repairs")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": 1001,
                                  "description": "repair for statistics"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(get("/api/statistics/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.deviceTotal").value(4))
                .andExpect(jsonPath("$.data.availableDeviceTotal").value(1))
                .andExpect(jsonPath("$.data.borrowingTotal").value(1))
                .andExpect(jsonPath("$.data.pendingReservationTotal").value(1))
                .andExpect(jsonPath("$.data.pendingRepairTotal").value(1));

        mockMvc.perform(get("/api/statistics/devices/hot")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-10")
                        .param("topN", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].deviceId").value(1001))
                .andExpect(jsonPath("$.data[0].borrowCount").value(1))
                .andExpect(jsonPath("$.data[0].rankScope").value("TOTAL"));

        mockMvc.perform(get("/api/statistics/devices/damage")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("rankScope", "TOTAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].deviceId").value(1001))
                .andExpect(jsonPath("$.data[0].status").value("REPAIRING"))
                .andExpect(jsonPath("$.data[0].rankScope").value("TOTAL"));

        mockMvc.perform(get("/api/statistics/users/violations")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("rankScope", "TOTAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].userId").value(4))
                .andExpect(jsonPath("$.data[0].overdueCount").value(1))
                .andExpect(jsonPath("$.data[0].damageCount").value(1))
                .andExpect(jsonPath("$.data[0].violationCount").value(2))
                .andExpect(jsonPath("$.data[0].rankScope").value("TOTAL"));
    }

    @Test
    void superAdminCanManageMenusAndIconsButAdminCannot() throws Exception {
        String superAdminToken = loginAndGetToken("SA001", "0000");
        String adminToken = loginAndGetToken("A001", "0000");

        mockMvc.perform(get("/api/icons")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0]").value("HomeFilled"));

        mockMvc.perform(get("/api/menus")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].menuId").value(201))
                .andExpect(jsonPath("$.data[1].menuName").value("Device Management"));

        mockMvc.perform(put("/api/menus/202")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "menuName": "Device Center",
                                  "path": "/device-center",
                                  "icon": "Setting",
                                  "permissionCode": "device:view"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.menuName").value("Device Center"))
                .andExpect(jsonPath("$.data.path").value("/device-center"))
                .andExpect(jsonPath("$.data.icon").value("Setting"));

        mockMvc.perform(get("/api/menus")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].menuName").value("Device Center"));

        mockMvc.perform(get("/api/icons")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void systemTaskCanExpireUnpickedReservations() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");
        String adminToken = loginAndGetToken("A001", "0000");

        String reservationId = createAndApproveReservation(studentToken, teacherToken,
                "2026-04-01 09:00:00", "2026-04-01 10:00:00", "task expire");

        Object expiredCount = systemTaskService.runAllTasks(java.time.LocalDateTime.parse("2026-04-02T10:00:00")).get("expiredReservationCount");
        if (!Integer.valueOf(1).equals(expiredCount)) {
            throw new IllegalStateException("Expected one expired reservation but got: " + expiredCount);
        }

        mockMvc.perform(get("/api/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXPIRED"));

        mockMvc.perform(get("/api/devices/1001")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
    }

    @Test
    void systemTaskCanMarkOverdueRecordsAndGenerateReminderSummary() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");

        createAndApproveReservation(studentToken, teacherToken,
                "2026-04-03 09:00:00", "2026-04-03 10:00:00", "task overdue");

        String overdueRecordId = extractField(mockMvc.perform(get("/api/borrow-records")
                        .header("Authorization", "Bearer " + studentToken))
                .andReturn()
                .getResponse()
                .getContentAsString(), "\"recordId\":(\\d+)");

        mockMvc.perform(put("/api/borrow-records/" + overdueRecordId + "/pickup")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BORROWING"));

        String adminToken = loginAndGetToken("A001", "0000");
        String newDeviceResponse = mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceName": "Task Device",
                                  "deviceCode": "EQ-2026-0300",
                                  "category": "Testing",
                                  "location": "Lab 05",
                                  "description": "for tasks"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondDeviceId = extractField(newDeviceResponse, "\"deviceId\":(\\d+)");
        createAndApproveReservation(studentToken, teacherToken,
                "2026-04-04 09:00:00", "2026-04-06 10:00:00", "task reminder", secondDeviceId);

        String allRecords = mockMvc.perform(get("/api/borrow-records")
                        .header("Authorization", "Bearer " + studentToken))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String aboutToExpireRecordId = extractSecondField(allRecords, "\"recordId\":(\\d+)");

        mockMvc.perform(put("/api/borrow-records/" + aboutToExpireRecordId + "/pickup")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BORROWING"));

        java.util.Map<String, Object> taskResult = systemTaskService.runAllTasks(java.time.LocalDateTime.parse("2026-04-05T10:00:00"));
        if (!Integer.valueOf(1).equals(taskResult.get("overdueRecordCount"))) {
            throw new IllegalStateException("Expected one overdue record but got: " + taskResult.get("overdueRecordCount"));
        }
        if (!Integer.valueOf(1).equals(taskResult.get("aboutToExpireReminderCount"))) {
            throw new IllegalStateException("Expected one about-to-expire reminder but got: " + taskResult.get("aboutToExpireReminderCount"));
        }
        if (!Integer.valueOf(1).equals(taskResult.get("overdueReminderCount"))) {
            throw new IllegalStateException("Expected one overdue reminder but got: " + taskResult.get("overdueReminderCount"));
        }

        mockMvc.perform(get("/api/borrow-records")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "OVERDUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void notificationCenterAndTaskLogsWorkAfterSystemTasks() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");
        String adminToken = loginAndGetToken("A001", "0000");

        createAndApproveReservation(studentToken, teacherToken,
                "2026-04-01 09:00:00", "2026-04-01 10:00:00", "notify task");

        systemTaskService.runAllTasks(java.time.LocalDateTime.parse("2026-04-02T10:00:00"));

        String summaryResponse = mockMvc.perform(get("/api/notifications/summary")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.unreadTotal").value(2))
                .andExpect(jsonPath("$.data.unreadByType.RESERVATION_EXPIRED").value(1))
                .andExpect(jsonPath("$.data.unreadByType.OVERDUE_REMINDER").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String notificationId = extractField(summaryResponse, "\"notificationId\":(\\d+)");

        mockMvc.perform(get("/api/profile/messages")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("confirmStatus", "UNCONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].confirmStatus").value("UNCONFIRMED"));

        mockMvc.perform(get("/api/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("userId", "4")
                        .param("confirmStatus", "UNCONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/messages/unconfirmed-summary")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.overdueCount").value(1));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("read", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(put("/api/profile/messages/" + notificationId + "/confirm")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true))
                .andExpect(jsonPath("$.data.confirmStatus").value("CONFIRMED"));

        mockMvc.perform(get("/api/notifications/summary")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadTotal").value(1));

        mockMvc.perform(get("/api/messages")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        String taskLogsResponse = mockMvc.perform(get("/api/task-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.list[0].status").value("SUCCESS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String logId = extractField(taskLogsResponse, "\"logId\":(\\d+)");

        mockMvc.perform(get("/api/task-logs/" + logId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logId").value(Long.parseLong(logId)));

        mockMvc.perform(get("/api/task-logs")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void directMessageConfirmEndpointWorks() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");

        createAndApproveReservation(studentToken, teacherToken,
                "2026-04-01 09:00:00", "2026-04-01 10:00:00", "direct confirm");

        systemTaskService.runAllTasks(java.time.LocalDateTime.parse("2026-04-02T10:00:00"));

        String messageList = mockMvc.perform(get("/api/profile/messages")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("confirmStatus", "UNCONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String messageId = extractField(messageList, "\"messageId\":(\\d+)");

        mockMvc.perform(put("/api/messages/" + messageId + "/confirm")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.messageId").value(Long.parseLong(messageId)))
                .andExpect(jsonPath("$.data.confirmStatus").value("CONFIRMED"));
    }

    @Test
    void statisticsSupportHalfYearAndMonthScopes() throws Exception {
        String studentToken = loginAndGetToken("20230001", "0000");
        String teacherToken = loginAndGetToken("T2026001", "0000");
        String adminToken = loginAndGetToken("A001", "0000");

        createAndApproveReservation(studentToken, teacherToken,
                "2026-03-29 09:00:00", "2026-03-29 10:00:00", "scope stats");

        String recordId = extractField(mockMvc.perform(get("/api/borrow-records")
                        .header("Authorization", "Bearer " + studentToken))
                .andReturn()
                .getResponse()
                .getContentAsString(), "\"recordId\":(\\d+)");

        mockMvc.perform(put("/api/borrow-records/" + recordId + "/pickup")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/borrow-records/" + recordId + "/overdue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/borrow-records/" + recordId + "/return")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "returnTime": "2026-03-30 10:00:00",
                                  "deviceCondition": "BROKEN"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/repairs")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": 1001,
                                  "description": "scope repair"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/statistics/devices/hot")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("rankScope", "HALF_YEAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].rankScope").value("HALF_YEAR"));

        mockMvc.perform(get("/api/statistics/devices/damage")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("rankScope", "MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].rankScope").value("MONTH"));

        mockMvc.perform(get("/api/statistics/users/violations")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("rankScope", "MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].rankScope").value("MONTH"));
    }

    private String loginAndGetToken(String loginId, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "%s",
                                  "password": "%s"
                                }
                                """.formatted(loginId, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractField(response, "\"token\":\"([^\"]+)\"");
    }

    private String createAndApproveReservation(String studentToken, String teacherToken, String startTime, String endTime, String purpose) throws Exception {
        return createAndApproveReservation(studentToken, teacherToken, startTime, endTime, purpose, "1001");
    }

    private String createAndApproveReservation(String studentToken, String teacherToken, String startTime, String endTime, String purpose, String deviceId) throws Exception {
        String adminToken = loginAndGetToken("A001", "0000");

        String reservationResponse = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": %s,
                                  "startTime": "%s",
                                  "endTime": "%s",
                                  "purpose": "%s"
                                }
                                """.formatted(deviceId, startTime, endTime, purpose)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String reservationId = extractField(reservationResponse, "\"reservationId\":(\\d+)");

        mockMvc.perform(put("/api/reservations/" + reservationId + "/approve")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "action": "APPROVE",
                                  "comment": "teacher approved"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(put("/api/reservations/" + reservationId + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "APPROVE",
                                  "comment": "admin approved"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PICKUP_PENDING"));

        return reservationId;
    }

    private String extractSecondField(String body, String pattern) {
        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(body);
        if (!matcher.find() || !matcher.find()) {
            throw new IllegalStateException("Failed to extract second field by pattern: " + pattern + ", body: " + body);
        }
        return matcher.group(1);
    }

    private String extractField(String body, String pattern) {
        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("Failed to extract field by pattern: " + pattern + ", body: " + body);
        }
        return matcher.group(1);
    }
}




