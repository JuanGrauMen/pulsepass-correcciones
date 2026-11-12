package com.pulsepass.exception;

import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que las excepciones del framework se traduzcan al codigo HTTP
 * correcto y con el cuerpo ErrorResponse, en lugar de caer en el 500 generico.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.StubController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.StubController.class})
class GlobalExceptionHandlerTest {

    @RestController
    static class StubController {

        @GetMapping("/stub/denied")
        void denied() {
            throw new AccessDeniedException("Access is denied");
        }

        @GetMapping("/stub/typed/{id}")
        void typed(@PathVariable Long id) {
        }

        @GetMapping("/stub/required")
        void required(@RequestParam String value) {
        }

        @PostMapping("/stub/post-only")
        void postOnly() {
        }

        @PostMapping(value = "/stub/json", consumes = MediaType.APPLICATION_JSON_VALUE)
        void json(@RequestBody Map<String, String> body) {
        }

        @GetMapping("/stub/boom")
        void boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @Test
    void accessDenied_returns403() throws Exception {
        mvc.perform(get("/stub/denied"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void unknownRoute_returns404() throws Exception {
        mvc.perform(get("/stub/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void pathVariableOfWrongType_returns400() throws Exception {
        mvc.perform(get("/stub/typed/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingRequiredParameter_returns400() throws Exception {
        mvc.perform(get("/stub/required"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void methodNotSupported_returns405() throws Exception {
        mvc.perform(get("/stub/post-only"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unsupportedMediaType_returns415() throws Exception {
        mvc.perform(post("/stub/json")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void unexpectedException_returns500WithoutLeakingDetails() throws Exception {
        mvc.perform(get("/stub/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected internal error occurred"));
    }
}
