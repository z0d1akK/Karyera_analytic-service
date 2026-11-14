package com.zodiakk.analyticservice.security.util;

import com.zodiakk.analyticservice.common.constants.SecurityConstants;
import com.zodiakk.analyticservice.security.principal.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityUtilsTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsTheCurrentUserAndRoles() {
        UUID userId = UUID.randomUUID();
        CurrentUser user = CurrentUser.builder().userId(userId).role(SecurityConstants.HR_AUTHORITY).email("a@b.c").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null)
        );

        assertEquals(userId, SecurityUtils.getCurrentUserId());
        assertFalse(SecurityUtils.isAdmin());
        assertTrue(SecurityUtils.hasRole(SecurityConstants.HR_AUTHORITY));
        assertTrue(SecurityUtils.hasAnyRole(SecurityConstants.ADMIN_AUTHORITY, SecurityConstants.HR_AUTHORITY));
        assertTrue(SecurityUtils.keysMatch("secret", "secret"));
        assertFalse(SecurityUtils.keysMatch("secret", "other"));
        assertFalse(SecurityUtils.keysMatch("", "secret"));
    }

    @Test
    void rejectsAMissingPrincipal() {
        assertThrows(IllegalStateException.class, SecurityUtils::getCurrentUser);
    }
}
