package com.laundromat.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Public
        if (path.equals("/api/auth/register") || path.equals("/api/auth/login")) {
            return true;
        }
        if (method.equals("GET") && path.startsWith("/api/machines")) {
            return true;
        }

        // Must be signed in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
        }

        // Admin only
        boolean adminOnly = path.startsWith("/api/admin") || path.startsWith("/api/machines");
        if (adminOnly && !"ADMIN".equals(session.getAttribute("role"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins only");
        }

        return true;
    }
}
