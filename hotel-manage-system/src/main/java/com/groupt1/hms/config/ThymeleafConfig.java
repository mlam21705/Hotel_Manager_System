package com.groupt1.hms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.File;

/**
 * Registers Thymeleaf template resolvers that let the controllers render the
 * shared public front-end living in {@code free-hotel-website-template/} as
 * server-side Thymeleaf views, in addition to the classpath manager templates.
 *
 * <p>The public template folder sits outside the Maven module, so a
 * {@link FileTemplateResolver} is used. Two candidate paths are probed to
 * support both running from the Maven module directory (IntelliJ default:
 * {@code ../free-hotel-website-template/}) and from the repository root
 * ({@code free-hotel-website-template/}). Each file resolver uses
 * {@code checkExistence=true} so that, when a view is not found there, Thymeleaf
 * falls through to the classpath resolver used by the manager views.
 */
@Configuration
public class ThymeleafConfig {

    /** Resolves public views when the app is started from the Maven module folder. */
    @Bean
    public FileTemplateResolver publicModuleTemplateResolver() {
        return buildFileResolver("../free-hotel-website-template/", 1);
    }

    /** Resolves public views when the app is started from the repository root. */
    @Bean
    public FileTemplateResolver publicRootTemplateResolver() {
        return buildFileResolver("free-hotel-website-template/", 2);
    }

    private FileTemplateResolver buildFileResolver(String prefix, int order) {
        FileTemplateResolver resolver = new FileTemplateResolver();
        // Normalize to an absolute path so it works regardless of the JVM working dir.
        resolver.setPrefix(new File(prefix).getAbsolutePath() + File.separator);
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        resolver.setCheckExistence(true);
        resolver.setOrder(order);
        return resolver;
    }

    /**
     * Classpath resolver for the manager views under {@code templates/}. Declared
     * explicitly (with a higher order value → lower priority) so it is consulted
     * only after the file resolvers above.
     *
     * <p>Named {@code defaultTemplateResolver} on purpose: Spring Boot's
     * {@code ThymeleafAutoConfiguration} only creates its own default resolver
     * when no bean with this name exists ({@code @ConditionalOnMissingBean}).
     * Overriding it here guarantees the file resolvers take precedence, so a view
     * name such as {@code "index"} resolves to the public template file rather
     * than the empty {@code templates/index.html} stub.
     */
    @Bean
    public SpringResourceTemplateResolver defaultTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        resolver.setCheckExistence(true);
        resolver.setOrder(3);
        return resolver;
    }
}
