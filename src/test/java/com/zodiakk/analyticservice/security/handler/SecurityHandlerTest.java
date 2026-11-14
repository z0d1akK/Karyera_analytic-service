package com.zodiakk.analyticservice.security.handler;

import com.zodiakk.analyticservice.common.exception.ErrorMessages;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityHandlerTest {

    @Test
    void entryPointUsesTheCredentialMessageWhenPresent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new CustomAuthenticationEntryPoint().commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("bad key")
        );

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("bad key"));
    }

    @Test
    void entryPointAndDeniedHandlerUseGenericMessages() throws Exception {
        MockHttpServletResponse unauthorized = new MockHttpServletResponse();
        new CustomAuthenticationEntryPoint().commence(
                new MockHttpServletRequest(),
                unauthorized,
                new InsufficientAuthenticationException("x")
        );
        assertTrue(unauthorized.getContentAsString().contains(ErrorMessages.UNAUTHORIZED));

        MockHttpServletResponse forbidden = new MockHttpServletResponse();
        new CustomAccessDeniedHandler().handle(
                new MockHttpServletRequest(),
                forbidden,
                new AccessDeniedException("no")
        );
        assertEquals(403, forbidden.getStatus());
        assertTrue(forbidden.getContentAsString().contains(ErrorMessages.ACCESS_DENIED));
    }
}
