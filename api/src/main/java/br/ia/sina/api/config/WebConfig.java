package br.ia.sina.api.config;

import br.ia.sina.api.device.CurrentDeviceArgumentResolver;
import br.ia.sina.api.device.DeviceAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final DeviceAuthInterceptor authInterceptor;
    private final CurrentDeviceArgumentResolver currentDeviceResolver;
    private final String[] corsOrigins;

    public WebConfig(DeviceAuthInterceptor authInterceptor, CurrentDeviceArgumentResolver currentDeviceResolver,
                     @Value("${sina.cors-origins:}") String[] corsOrigins) {
        this.authInterceptor = authInterceptor;
        this.currentDeviceResolver = currentDeviceResolver;
        this.corsOrigins = corsOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (corsOrigins.length > 0) {
            registry.addMapping("/v1/**")
                    .allowedOrigins(corsOrigins)
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("Authorization", "Content-Type");
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/v1/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentDeviceResolver);
    }
}
