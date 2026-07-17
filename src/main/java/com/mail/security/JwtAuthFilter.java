package com.mail.security;

import com.mail.mapper.UserMapper;
import com.mail.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    /** 用户信息缓存：userId -> CacheEntry */
    private final ConcurrentHashMap<Long, CacheEntry> userCache = new ConcurrentHashMap<>();

    /** 缓存过期时间：5分钟 */
    private static final long CACHE_EXPIRE_MS = 5 * 60 * 1000L;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                HttpServletResponse response,
                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && jwtUtil.isValid(token)) {
            Long userId = jwtUtil.parseUserId(token);
            User user = getCachedUser(userId);
            if (user != null) {
                UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername(), user.getEmail());
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 从缓存获取用户，缓存未命中或过期时查询数据库
     */
    private User getCachedUser(Long userId) {
        CacheEntry entry = userCache.get(userId);
        long now = System.currentTimeMillis();
        if (entry != null && !entry.isExpired(now)) {
            return entry.getUser();
        }
        User user = userMapper.findById(userId);
        if (user != null) {
            userCache.put(userId, new CacheEntry(user, now + CACHE_EXPIRE_MS));
        } else {
            userCache.remove(userId);
        }
        return user;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /** 缓存条目，包含用户数据和过期时间 */
    private static class CacheEntry {
        private final User user;
        private final long expireAt;

        CacheEntry(User user, long expireAt) {
            this.user = user;
            this.expireAt = expireAt;
        }

        User getUser() { return user; }

        boolean isExpired(long now) { return now >= expireAt; }
    }
}
