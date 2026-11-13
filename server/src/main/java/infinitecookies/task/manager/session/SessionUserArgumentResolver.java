package infinitecookies.task.manager.session;

import infinitecookies.task.manager.exceptions.ApiErrorCode;
import infinitecookies.task.manager.exceptions.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SessionUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == SessionUser.class;
    }

    @Override
    public @Nullable Object resolveArgument(@NonNull MethodParameter parameter,
                                            @Nullable ModelAndViewContainer mavContainer,
                                            NativeWebRequest webRequest,
                                            @Nullable WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        HttpSession session = request == null ? null : request.getSession(false);

        Object userId = session == null ? null : session.getAttribute("userId");

        if (!(userId instanceof Long id)) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.USER_SESSION_NOT_INITIALIZED,
                "User session not initialized"
            );
        }

        return new SessionUser(id);
    }
}
