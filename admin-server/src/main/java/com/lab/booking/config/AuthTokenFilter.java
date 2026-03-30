package com.lab.booking.config;

import com.lab.booking.model.UserEntity;
import com.lab.booking.repository.AuthRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private final AuthRepository authRepository;

    public AuthTokenFilter(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            authRepository.findByToken(token).ifPresent(user -> {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        token,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRoleCode().name()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserEntity user
                && user.isFirstLoginRequired()
                && isBlockedBeforePasswordChange(request.getRequestURI())) {
            writeFirstLoginResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlockedBeforePasswordChange(String requestUri) {
        return !"/api/auth/me".equals(requestUri)
                && !"/api/auth/password".equals(requestUri)
                && !"/api/health".equals(requestUri);
    }

    private void writeFirstLoginResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write("""
                {"code":403,"message":"首次登录请先修改密码","data":null}
                """.getBytes(StandardCharsets.UTF_8));
    }
}
