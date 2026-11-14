package com.zodiakk.analyticservice.security.access;

import com.zodiakk.analyticservice.common.constants.SecurityConstants;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.dto.EmployeeAccessScopeDto;
import com.zodiakk.analyticservice.integration.dto.EmployeeLookupItemDto;
import com.zodiakk.analyticservice.integration.dto.SubordinateIdsDto;
import com.zodiakk.analyticservice.security.principal.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsAccessServiceTest {

    @Mock
    private EmployeeClient employeeClient;

    @InjectMocks
    private AnalyticsAccessService service;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hrCanSeeEveryoneAndEmployeesCannotOpenDashboards() {
        authenticate(SecurityConstants.HR_AUTHORITY);
        service.requireHrOrAdmin();
        assertNull(service.visibleEmployeeIds());
        assertTrue(service.canSeeEmployee(UUID.randomUUID(), null));

        authenticate(SecurityConstants.EMPLOYEE_AUTHORITY);
        assertThrows(AccessDeniedException.class, service::requireHrOrAdmin);
        assertThrows(AccessDeniedException.class, service::visibleEmployeeIds);
    }

    @Test
    void managerSeesOnlySubordinates() {
        UUID userId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        authenticate(SecurityConstants.MANAGER_AUTHORITY, userId);
        when(employeeClient.employeeByUser(userId)).thenReturn(new EmployeeLookupItemDto(managerId, "M", true, null, null, null));
        when(employeeClient.subordinateIds(managerId)).thenReturn(new SubordinateIdsDto(managerId, List.of(employeeId)));
        when(employeeClient.accessScope(employeeId, managerId))
                .thenReturn(new EmployeeAccessScopeDto(employeeId, managerId, false, true, true));

        assertTrue(service.visibleEmployeeIds().contains(employeeId));
        service.requireSelfOrManager(managerId);
        service.requireSelfOrManager(employeeId);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.requireSelfOrManager(UUID.randomUUID()));
    }

    @Test
    void dashboardAndEmployeeAccessDependOnRole() {
        authenticate(SecurityConstants.ADMIN_AUTHORITY);
        service.requireDashboardAccess();
        assertTrue(service.isAdmin());

        authenticate(SecurityConstants.HR_AUTHORITY);
        service.requireSelfOrManager(UUID.randomUUID());
        assertTrue(service.isHrOrAdmin());
        assertTrue(service.canAccessEmployee(UUID.randomUUID()));

        UUID userId = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        authenticate(SecurityConstants.EMPLOYEE_AUTHORITY, userId);
        when(employeeClient.employeeByUser(userId)).thenReturn(new EmployeeLookupItemDto(self, "E", true, null, null, null));
        assertThrows(AccessDeniedException.class, service::requireDashboardAccess);
        assertTrue(service.canAccessEmployee(self));
        assertFalse(service.canAccessEmployee(UUID.randomUUID()));
    }

    private void authenticate(String role) {
        authenticate(role, UUID.randomUUID());
    }

    private void authenticate(String role, UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CurrentUser.builder().userId(userId).role(role).email("a@b.c").build(),
                null
        ));
    }
}
