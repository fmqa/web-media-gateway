package com.github.fmqa.spu.config;

import com.github.fmqa.spu.support.Announcer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Provides an {@link Announcer} controller argument that may be used to announce response media resources.
 * <p></p>
 * If the duration is finite and greater than zero, it is returned within a {@code Content-Duration} header.
 * <p></p>
 * Note that calling the announcement method within a HEAD request results in a {@link AnnouncementException} being
 * thrown, resulting in 200 OK if left to be handled with the default {@link AnnouncementHandler}.
 */
@Configuration
public class Announcers implements WebMvcConfigurer, HandlerMethodArgumentResolver {
    static final String DURATION_HEADER = "Content-Duration";

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Announcer.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Announcer resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        final var response = webRequest.getNativeResponse(HttpServletResponse.class);
        if (response == null) {
            throw new IllegalStateException("No HttpServletResponse available in current request context");
        }
        final var request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("No HttpServletRequest available in current request context");
        }
        return (content, seconds) -> {
            if (HttpMethod.HEAD.matches(request.getMethod())) {
                throw new AnnouncementException(content, seconds);
            } else {
                response.setContentType(content);
                if (seconds >= 0 && Double.isFinite(seconds)) {
                    response.setHeader(DURATION_HEADER, Double.toString(seconds));
                }
            }
        };
    }
}
