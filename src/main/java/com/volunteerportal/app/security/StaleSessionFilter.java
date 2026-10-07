package com.volunteerportal.app.security;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.volunteerportal.app.repository.UserRepository;

/**
 * Ends the website session of a user who has been deactivated (Admin → Manage Users) or whose password
 * has changed since they logged in (changed in another browser or the app, set by an admin, or reset by
 * email), instead of letting it run until they log out. The session's own password change updates the
 * hash it holds, so only the other sessions end. One primary-key lookup per page request; it works
 * across several servers, unlike tracking sessions in one server's memory.
 */
public class StaleSessionFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public StaleSessionFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/css/") || path.startsWith("/webjars/") || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            Optional<String> hash = userRepository.findActivePasswordHash(principal.getUser().getId());
            if (hash.isEmpty() || !hash.get().equals(principal.getUser().getPassword())) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath()
                        + (hash.isEmpty() ? "/login?disabled" : "/login?passwordChanged"));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
