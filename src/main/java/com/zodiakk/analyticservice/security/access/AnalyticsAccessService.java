package com.zodiakk.analyticservice.security.access;

import com.zodiakk.analyticservice.common.constants.SecurityConstants;
import com.zodiakk.analyticservice.common.exception.TransientIntegrationException;
import com.zodiakk.analyticservice.integration.client.EmployeeClient;
import com.zodiakk.analyticservice.integration.dto.EmployeeAccessScopeDto;
import com.zodiakk.analyticservice.integration.dto.EmployeeLookupItemDto;
import com.zodiakk.analyticservice.integration.dto.SubordinateIdsDto;
import com.zodiakk.analyticservice.integration.support.IntegrationCalls;
import com.zodiakk.analyticservice.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component("analyticsAccess")
@RequiredArgsConstructor
public class AnalyticsAccessService {

    private final EmployeeClient employeeClient;

    public boolean isHr() {
        return SecurityUtils.hasRole(SecurityConstants.HR_AUTHORITY);
    }

    public boolean isAdmin() {
        return SecurityUtils.hasRole(SecurityConstants.ADMIN_AUTHORITY);
    }

    public boolean isHrOrAdmin() {
        return isHr() || isAdmin();
    }

    public boolean isManager() {
        return SecurityUtils.hasRole(SecurityConstants.MANAGER_AUTHORITY);
    }

    public void requireHrOnly() {
        if (!isHr()) {
            throw new AccessDeniedException("Недостаточно прав");
        }
    }

    public void requireHrOrAdmin() {
        requireHrOnly();
    }

    public void requireDashboardAccess() {
        if (!isHr() && !isAdmin() && !isManager()) {
            throw new AccessDeniedException("Недостаточно прав для просмотра аналитики");
        }
    }

    public UUID currentEmployeeId() {
        EmployeeLookupItemDto employee = IntegrationCalls.required(
                () -> employeeClient.employeeByUser(SecurityUtils.getCurrentUserId()),
                "Сотрудник текущего пользователя не найден"
        );
        return employee.id();
    }

    public void requireSelfOrManager(UUID employeeId) {
        if (isHr()) {
            return;
        }

        UUID actorEmployeeId = currentEmployeeId();

        if (actorEmployeeId.equals(employeeId)) {
            return;
        }

        if (!isManager()) {
            throw new AccessDeniedException("Нет доступа к данным выбранного сотрудника");
        }

        EmployeeAccessScopeDto scope;

        try {
            scope = employeeClient.accessScope(employeeId, actorEmployeeId);
        } catch (RuntimeException ex) {
            throw new TransientIntegrationException("Временная ошибка проверки доступа", ex);
        }

        if (scope == null || !scope.managerOfEmployee()) {
            throw new AccessDeniedException("Нет доступа к данным выбранного сотрудника");
        }
    }

    public boolean canAccessEmployee(UUID employeeId) {
        if (isHr()) {
            return true;
        }

        UUID actorEmployeeId = currentEmployeeId();

        if (actorEmployeeId.equals(employeeId)) {
            return true;
        }

        if (!isManager()) {
            return false;
        }

        try {
            EmployeeAccessScopeDto scope = employeeClient.accessScope(employeeId, actorEmployeeId);
            return scope != null && scope.managerOfEmployee();
        } catch (RuntimeException ex) {
            throw new TransientIntegrationException("Временная ошибка проверки доступа", ex);
        }
    }

    public List<UUID> visibleEmployeeIds() {
        if (isHr() || isAdmin()) {
            return null;
        }

        if (!isManager()) {
            throw new AccessDeniedException("Недостаточно прав для просмотра аналитики");
        }

        SubordinateIdsDto subordinates = IntegrationCalls.required(
                () -> employeeClient.subordinateIds(currentEmployeeId()),
                "Не удалось получить подчинённых"
        );

        return subordinates.subordinateIds() == null ? List.of() : subordinates.subordinateIds();
    }

    public boolean canSeeEmployee(UUID employeeId, List<UUID> visibleEmployeeIds) {
        return visibleEmployeeIds == null || visibleEmployeeIds.contains(employeeId);
    }
}
