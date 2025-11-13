package com.quma.quma_shopify_backend.filters;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.quma.quma_shopify_backend.utilities.UserContext;

@Component
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String username = request.getHeader("X-Client-Username");
            String ip = request.getHeader("X-Client-IP");
            String mac = request.getHeader("X-Client-MAC");

            // Optional defaults
            if (username == null)
                username = "GUEST";
            if (ip == null)
                ip = request.getRemoteAddr();
            if (mac == null)
                mac = "UNKNOWN";

            // Set context
            UserContext.set(username, ip, mac);

            // Continue filter chain
            filterChain.doFilter(request, response);
        } finally {
            // Clear context after request
            UserContext.clear();
        }
    }
}
