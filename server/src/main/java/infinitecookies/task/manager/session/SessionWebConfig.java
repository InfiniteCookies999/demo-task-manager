package infinitecookies.task.manager.session;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class SessionWebConfig implements WebMvcConfigurer {

    private final SessionUserArgumentResolver sessionUserArgumentResolver;

    public SessionWebConfig(SessionUserArgumentResolver sessionUserArgumentResolver) {
        this.sessionUserArgumentResolver = sessionUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(sessionUserArgumentResolver);
    }
}
