package com.reelvault.service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.reelvault.repository.AppUserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:file:reelvault-auth-test?mode=memory&cache=shared",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@DirtiesContext
class AuthenticationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;

    @Test
    void registrationLoginCurrentUserAndLogoutUseHashedPasswordsAndSession() throws Exception {
        MvcResult csrfResult = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        String csrf = csrfCookie.getValue();
        String registration = """
                {"email":"Watcher@Example.com","displayName":"Watcher","password":"local-safe-pass-01"}
                """;
        mvc.perform(post("/api/auth/register").cookie(csrfCookie).header("X-XSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("watcher@example.com"));

        var user = users.findByNormalizedEmail("watcher@example.com").orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("local-safe-pass-01");

        String login = """
                {"email":"WATCHER@example.com","password":"local-safe-pass-01"}
                """;
        MockHttpSession anonymousSession = new MockHttpSession();
        String anonymousSessionId = anonymousSession.getId();
        MvcResult loginResult = mvc.perform(post("/api/auth/login").session(anonymousSession).cookie(csrfCookie).header("X-XSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Watcher")).andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getId()).isNotEqualTo(anonymousSessionId);

        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("watcher@example.com"));
        mvc.perform(get("/api/media")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").session(session).cookie(csrfCookie).header("X-XSRF-TOKEN", csrf))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }
}
