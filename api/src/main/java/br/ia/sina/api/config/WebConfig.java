package br.ia.sina.api.config;

import br.ia.sina.api.device.CurrentDeviceArgumentResolver;
import br.ia.sina.api.device.DeviceAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final DeviceAuthInterceptor authInterceptor;
    private final CurrentDeviceArgumentResolver currentDeviceResolver;

    public WebConfig(DeviceAuthInterceptor authInterceptor, CurrentDeviceArgumentResolver currentDeviceResolver) {
        this.authInterceptor = authInterceptor;
        this.currentDeviceResolver = currentDeviceResolver;
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
