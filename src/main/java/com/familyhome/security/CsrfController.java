package com.familyhome.security;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Security", description = "CSRF token bootstrap endpoint")
public class CsrfController {

    @GetMapping("/csrf")
    public CsrfToken getCsrf(CsrfToken token) {
        return token;
    }
}
