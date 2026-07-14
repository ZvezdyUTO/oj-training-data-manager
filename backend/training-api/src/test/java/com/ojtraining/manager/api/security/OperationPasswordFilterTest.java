package com.ojtraining.manager.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtraining.manager.api.config.OperationPasswordProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationPasswordFilterTest {
    private final OperationPasswordFilter filter = new OperationPasswordFilter(
            new OperationPasswordProperties("correct horse battery staple"),
            new ObjectMapper()
    );

    @Test
    void readMethodsRemainPublic() throws Exception {
        for (String method : new String[]{"GET", "HEAD", "OPTIONS"}) {
            MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/training-data/users");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertEquals(200, response.getStatus());
            assertEquals(request, chain.getRequest());
        }
    }

    @Test
    void everyWriteMethodRejectsMissingPasswordWithSameResponse() throws Exception {
        for (String method : new String[]{"POST", "PUT", "PATCH", "DELETE", "TRACE"}) {
            MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/members/demo");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, new MockFilterChain());

            assertEquals(401, response.getStatus());
            assertEquals("no-store", response.getHeader("Cache-Control"));
            assertTrue(response.getContentAsString().contains("\"code\":401"));
            assertTrue(response.getContentAsString().contains("操作密码错误"));
        }
    }

    @Test
    void correctPasswordAllowsWriteWithoutPersistingItInResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/members/batch");
        request.addHeader(OperationPasswordFilter.HEADER_NAME, "correct horse battery staple");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(request, chain.getRequest());
        assertEquals("", response.getContentAsString());
    }
}
