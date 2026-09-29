package com.zovira.security;

import com.zovira.common.exception.ApiException;
import com.zovira.common.exception.ErrorCodes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && AuthUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        AuthUser user = current();
        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        if (user == null && annotation != null && annotation.required()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Authentication is required");
        }
        return user;
    }

    public static AuthUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken token)) {
            return null;
        }
        Jwt jwt = token.getToken();
        List<String> roles = jwt.getClaimAsStringList(JwtConfig.CLAIM_ROLES);
        Set<String> roleSet = roles == null ? Set.of() : new HashSet<>(roles);
        return new AuthUser(Long.valueOf(jwt.getSubject()), jwt.getClaimAsString(JwtConfig.CLAIM_EMAIL),
                jwt.getClaimAsString(JwtConfig.CLAIM_NAME), Set.copyOf(roleSet));
    }
}
