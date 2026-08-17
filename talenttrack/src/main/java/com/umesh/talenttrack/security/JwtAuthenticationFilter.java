package com.umesh.talenttrack.security;

import com.umesh.talenttrack.domain.UserType;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (jwtProvider.validateToken(token)) {
            Claims claims = jwtProvider.getClaims(token);
            String email = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            String userTypeStr = claims.get("userType", String.class);
            Long companyId = claims.get("companyId", Long.class);
            
            @SuppressWarnings("unchecked")
            List<String> auths = claims.get("authorities", List.class);
            List<SimpleGrantedAuthority> authorities = auths.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            CustomUserDetails userDetails = new CustomUserDetails(
                    userId,
                    email,
                    "", // blank password for token authentication
                    UserType.valueOf(userTypeStr),
                    companyId,
                    authorities
            );

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
