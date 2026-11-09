package br.ia.sina.api.device;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class DeviceAuthInterceptor implements HandlerInterceptor {

    public static final String DEVICE_ATTRIBUTE = DeviceAuthInterceptor.class.getName() + ".device";
    static final String UNAUTHORIZED_MESSAGE = "Token do dispositivo inválido ou ausente";
    private static final String BEARER = "Bearer ";

    private final DeviceRepository repository;
    private final TokenService tokenService;

    public DeviceAuthInterceptor(DeviceRepository repository, TokenService tokenService) {
        this.repository = repository;
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method) || isRegistration(request)) {
            return true;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER) || header.length() == BEARER.length()) {
            throw unauthorized();
        }
        Device device = repository.findByTokenHash(tokenService.hash(header.substring(BEARER.length()).trim()))
                .orElseThrow(DeviceAuthInterceptor::unauthorized);
        RequiresRole required = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequiresRole.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequiresRole.class);
        }
        if (required != null && Arrays.stream(required.value()).noneMatch(r -> r == device.getRole())) {
            throw new ApiException(ErrorCode.FORBIDDEN_ROLE, "Papel do dispositivo não permite esta operação");
        }
        request.setAttribute(DEVICE_ATTRIBUTE, device);
        return true;
    }

    private static boolean isRegistration(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && "/v1/devices".equals(request.getRequestURI());
    }

    static ApiException unauthorized() {
        return new ApiException(ErrorCode.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
    }
}
