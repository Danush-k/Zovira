package com.zovira.security.ratelimit;

import com.zovira.common.exception.ErrorCodes;
import com.zovira.security.ProblemResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-client-IP throttling. Sensitive endpoints (login, registration, password reset) get tight
 * budgets to blunt credential stuffing and enumeration; everything else shares a generous
 * global budget.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    record Rule(String name, String method, String pattern, int limit, Duration window) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("login", "POST", "/api/v1/auth/login", 10, Duration.ofMinutes(1)),
            new Rule("register", "POST", "/api/v1/auth/register", 20, Duration.ofHours(1)),
            new Rule("forgot", "POST", "/api/v1/auth/forgot-password", 5, Duration.ofMinutes(15)),
            new Rule("reset", "POST", "/api/v1/auth/reset-password", 10, Duration.ofMinutes(15)),
            new Rule("verify-resend", "POST", "/api/v1/auth/resend-verification", 3, Duration.ofMinutes(15)),
            new Rule("refresh", "POST", "/api/v1/auth/refresh", 60, Duration.ofMinutes(1)),
            new Rule("checkout", "POST", "/api/v1/orders", 20, Duration.ofMinutes(1)),
            new Rule("reviews", "POST", "/api/v1/products/*/reviews", 10, Duration.ofMinutes(10)),
            new Rule("uploads", "POST", "/api/v1/**/images", 60, Duration.ofMinutes(10)));

    private static final Rule GLOBAL = new Rule("api", null, "/api/**", 600, Duration.ofMinutes(1));

    private final RateLimiter limiter;
    private final ProblemResponseWriter problemWriter;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public RateLimitFilter(RateLimiter limiter, ProblemResponseWriter problemWriter) {
        this.limiter = limiter;
        this.problemWriter = problemWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();

        for (Rule rule : RULES) {
            if (rule.method().equals(request.getMethod()) && matcher.match(rule.pattern(), path)
                    && !admit(rule, ip, response)) {
                return;
            }
        }
        if (!admit(GLOBAL, ip, response)) {
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean admit(Rule rule, String ip, HttpServletResponse response) throws IOException {
        RateLimiter.Decision decision = limiter.tryConsume(rule.name() + ':' + ip, rule.limit(), rule.window());
        if (decision.allowed()) {
            if (rule != GLOBAL) {
                response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
            }
            return true;
        }
        response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
        problemWriter.write(response, HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.RATE_LIMITED,
                "Too many requests. Please wait a moment and try again.");
        return false;
    }
}
