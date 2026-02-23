package com.cuttypaws.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    private final JwtUtils jwtUtils;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                parameter.getParameterType().equals(UUID.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String token = jwtUtils.extractJwtFromRequest(request);

        if (token == null) {
            throw new AuthenticationServiceException("Unauthorized: Missing token");
        }

        UUID userId = jwtUtils.getUserIdFromToken(token);

        if (userId == null) {
            throw new RuntimeException("Unauthorized: Invalid token");
        }
        return userId;
    }
}
