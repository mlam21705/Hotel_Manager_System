package com.groupt1.hms.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

/**
 * Exposes the currently logged-in user (stored in the HTTP session under
 * {@code "user"} by {@link AuthController#login}) to every Thymeleaf view as the
 * {@code currentUser} model attribute. This lets the shared navbar fragment
 * render its logged-in / signed-out state entirely on the server, replacing the
 * former {@code sessionStorage}-driven logic in {@code auth.js}.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("currentUser")
    public Map<String, Object> currentUser(HttpSession session) {
        Object user = session.getAttribute("user");
        if (user instanceof Map<?, ?> userMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) userMap;
            return typed;
        }
        return null;
    }
}
