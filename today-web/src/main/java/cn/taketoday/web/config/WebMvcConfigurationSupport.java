/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.aware.ApplicationContextSupport;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.format.Formatter;
import cn.taketoday.format.FormatterRegistry;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.format.support.FormattingConversionService;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.AllEncompassingFormHttpMessageConverter;
import cn.taketoday.http.converter.ByteArrayHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.ResourceHttpMessageConverter;
import cn.taketoday.http.converter.ResourceRegionHttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import cn.taketoday.http.converter.feed.AtomFeedHttpMessageConverter;
import cn.taketoday.http.converter.feed.RssChannelHttpMessageConverter;
import cn.taketoday.http.converter.json.GsonHttpMessageConverter;
import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.http.converter.json.JsonbHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.validation.Errors;
import cn.taketoday.validation.MessageCodesResolver;
import cn.taketoday.validation.Validator;
import cn.taketoday.validation.beanvalidation.OptionalValidatorFactoryBean;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.bind.support.ConfigurableWebBindingInitializer;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.handler.CompositeHandlerExceptionHandler;
import cn.taketoday.web.handler.FunctionRequestAdapter;
import cn.taketoday.web.handler.HandlerExecutionChainHandlerAdapter;
import cn.taketoday.web.handler.NotFoundHandler;
import cn.taketoday.web.handler.ResponseStatusExceptionHandler;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.SimpleHandlerExceptionHandler;
import cn.taketoday.web.handler.ViewControllerHandlerAdapter;
import cn.taketoday.web.handler.method.AnnotationHandlerFactory;
import cn.taketoday.web.handler.method.ControllerAdviceBean;
import cn.taketoday.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import cn.taketoday.web.handler.method.JsonViewRequestBodyAdvice;
import cn.taketoday.web.handler.method.JsonViewResponseBodyAdvice;
import cn.taketoday.web.handler.method.ParameterResolvingRegistryResolvableParameterFactory;
import cn.taketoday.web.handler.method.RequestBodyAdvice;
import cn.taketoday.web.handler.method.RequestMappingHandlerAdapter;
import cn.taketoday.web.handler.method.RequestMappingHandlerMapping;
import cn.taketoday.web.handler.method.ResponseBodyAdvice;
import cn.taketoday.web.registry.AbstractHandlerMapping;
import cn.taketoday.web.registry.FunctionHandlerMapping;
import cn.taketoday.web.registry.SimpleUrlHandlerMapping;
import cn.taketoday.web.registry.ViewControllerHandlerMapping;
import cn.taketoday.web.resource.ResourceUrlProvider;
import cn.taketoday.web.servlet.ServletViewResolverComposite;
import cn.taketoday.web.servlet.WebServletApplicationContext;
import cn.taketoday.web.servlet.view.InternalResourceViewResolver;
import cn.taketoday.web.view.RedirectModelManager;
import cn.taketoday.web.view.ViewResolver;
import cn.taketoday.web.view.ViewResolverComposite;
import cn.taketoday.web.view.ViewReturnValueHandler;
import jakarta.servlet.ServletContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/27 23:43
 */
@DisableAllDependencyInjection
public class WebMvcConfigurationSupport extends ApplicationContextSupport {
  protected final Logger log = LoggerFactory.getLogger(getClass());
  static final String ENABLE_WEB_MVC_XML = "enable.webmvc.xml";
  static final String WEB_MVC_CONFIG_LOCATION = "WebMvcConfigLocation";

  private static final boolean gsonPresent = isPresent("com.google.gson.Gson");
  private static final boolean jsonbPresent = isPresent("jakarta.json.bind.Jsonb");
  private static final boolean romePresent = isPresent("com.rometools.rome.feed.WireFeed");
  private static final boolean jackson2Present = isPresent("com.fasterxml.jackson.databind.ObjectMapper")
          && isPresent("com.fasterxml.jackson.core.JsonGenerator");
  private static final boolean jackson2SmilePresent = isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory");
  private static final boolean jackson2CborPresent = isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory");

  private final List<Object> requestResponseBodyAdvice = new ArrayList<>();

  @Nullable
  private ContentNegotiationManager contentNegotiationManager;

  @Nullable
  private PathMatchConfigurer pathMatchConfigurer;

  @Nullable
  private List<HttpMessageConverter<?>> messageConverters;

  @Nullable
  private Map<String, CorsConfiguration> corsConfigurations;

  @Nullable
  private List<Object> interceptors;

  @Nullable
  private AsyncSupportConfigurer asyncSupportConfigurer;

  @Override
  protected void initApplicationContext() {
    initControllerAdviceCache();
  }

  private void initControllerAdviceCache() {
    List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext(),
            RequestBodyAdvice.class, ResponseBodyAdvice.class);

    if (!adviceBeans.isEmpty()) {
      requestResponseBodyAdvice.addAll(0, adviceBeans);
    }

    if (jackson2Present) {
      requestResponseBodyAdvice.add(new JsonViewRequestBodyAdvice());
      requestResponseBodyAdvice.add(new JsonViewResponseBodyAdvice());
    }

  }

  //---------------------------------------------------------------------
  // HttpMessageConverter
  //---------------------------------------------------------------------

  /**
   * Provides access to the shared {@link HttpMessageConverter HttpMessageConverters}
   * used by the {@link ParameterResolvingStrategy} and the
   * {@link ReturnValueHandlerManager}.
   * <p>This method cannot be overridden; use {@link #configureMessageConverters} instead.
   * Also see {@link #addDefaultHttpMessageConverters} for adding default message converters.
   */
  public final List<HttpMessageConverter<?>> getMessageConverters() {
    if (this.messageConverters == null) {
      this.messageConverters = new ArrayList<>();
      configureMessageConverters(this.messageConverters);
      if (this.messageConverters.isEmpty()) {
        addDefaultHttpMessageConverters(this.messageConverters);
      }
      extendMessageConverters(this.messageConverters);
    }
    return this.messageConverters;
  }

  /**
   * Override this method to add custom {@link HttpMessageConverter HttpMessageConverters}
   * to use with the {@link ParameterResolvingStrategy} and the
   * {@link ReturnValueHandlerManager}.
   * <p>Adding converters to the list turns off the default converters that would
   * otherwise be registered by default. Also see {@link #addDefaultHttpMessageConverters}
   * for adding default message converters.
   *
   * @param converters a list to add message converters to (initially an empty list)
   * @since 4.0
   */
  protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) { }

  /**
   * Override this method to extend or modify the list of converters after it has
   * been configured. This may be useful for example to allow default converters
   * to be registered and then insert a custom converter through this method.
   *
   * @param converters the list of configured converters to extend
   * @since 4.0
   */
  protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) { }

  /**
   * Adds a set of default HttpMessageConverter instances to the given list.
   * Subclasses can call this method from {@link #configureMessageConverters}.
   *
   * @param messageConverters the list to add the default message converters to
   * @since 4.0
   */
  protected final void addDefaultHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
    messageConverters.add(new ByteArrayHttpMessageConverter());
    messageConverters.add(new StringHttpMessageConverter());
    messageConverters.add(new ResourceHttpMessageConverter());
    messageConverters.add(new ResourceRegionHttpMessageConverter());
    messageConverters.add(new AllEncompassingFormHttpMessageConverter());

    if (romePresent) {
      messageConverters.add(new AtomFeedHttpMessageConverter());
      messageConverters.add(new RssChannelHttpMessageConverter());
    }

    ApplicationContext applicationContext = getApplicationContext();

    if (jackson2Present) {
      Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
      if (applicationContext != null) {
        builder.applicationContext(applicationContext);
      }
      messageConverters.add(new MappingJackson2HttpMessageConverter(builder.build()));
    }
    else if (gsonPresent) {
      messageConverters.add(new GsonHttpMessageConverter());
    }
    else if (jsonbPresent) {
      messageConverters.add(new JsonbHttpMessageConverter());
    }

    if (jackson2SmilePresent) {
      Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.smile();
      if (applicationContext != null) {
        builder.applicationContext(applicationContext);
      }
      messageConverters.add(new MappingJackson2SmileHttpMessageConverter(builder.build()));
    }
    if (jackson2CborPresent) {
      Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.cbor();
      if (applicationContext != null) {
        builder.applicationContext(applicationContext);
      }
      messageConverters.add(new MappingJackson2CborHttpMessageConverter(builder.build()));
    }
  }

  //---------------------------------------------------------------------
  // ContentNegotiation
  //---------------------------------------------------------------------

  /**
   * Return a {@link ContentNegotiationManager} instance to use to determine
   * requested {@linkplain MediaType media types} in a given request.
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public ContentNegotiationManager contentNegotiationManager() {
    if (this.contentNegotiationManager == null) {
      ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();
      configurer.mediaTypes(getDefaultMediaTypes());
      configureContentNegotiation(configurer);
      this.contentNegotiationManager = configurer.buildContentNegotiationManager();
    }
    return this.contentNegotiationManager;
  }

  protected Map<String, MediaType> getDefaultMediaTypes() {
    Map<String, MediaType> map = new HashMap<>(4);
    if (romePresent) {
      map.put("atom", MediaType.APPLICATION_ATOM_XML);
      map.put("rss", MediaType.APPLICATION_RSS_XML);
    }
    if (jackson2Present || gsonPresent || jsonbPresent) {
      map.put("json", MediaType.APPLICATION_JSON);
    }
    if (jackson2SmilePresent) {
      map.put("smile", MediaType.valueOf("application/x-jackson-smile"));
    }
    if (jackson2CborPresent) {
      map.put("cbor", MediaType.APPLICATION_CBOR);
    }
    return map;
  }

  /**
   * Override this method to configure content negotiation.
   */
  protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) { }

  //---------------------------------------------------------------------
  // PathMatchConfigurer
  //---------------------------------------------------------------------

  /**
   * Callback for building the {@link PathMatchConfigurer}.
   * Delegates to {@link #configurePathMatch}.
   */
  protected PathMatchConfigurer getPathMatchConfigurer() {
    if (this.pathMatchConfigurer == null) {
      this.pathMatchConfigurer = new PathMatchConfigurer();
      configurePathMatch(this.pathMatchConfigurer);
    }
    return this.pathMatchConfigurer;
  }

  /**
   * Override this method to configure path matching options.
   *
   * @see PathMatchConfigurer
   */
  protected void configurePathMatch(PathMatchConfigurer configurer) { }

  /**
   * Register a {@link ViewResolverComposite} that contains a chain of view resolvers
   * to use for view resolution.
   * By default this resolver is ordered at 0 unless content negotiation view
   * resolution is used in which case the order is raised to
   * {@link cn.taketoday.core.Ordered#HIGHEST_PRECEDENCE Ordered.HIGHEST_PRECEDENCE}.
   * <p>If no other resolvers are configured,
   * {@link ViewResolverComposite#resolveViewName(String, Locale)} returns null in order
   * to allow other potential {@link ViewResolver} beans to resolve views.
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public ViewResolver webViewResolver(ContentNegotiationManager contentNegotiationManager) {
    ViewResolverRegistry registry =
            new ViewResolverRegistry(contentNegotiationManager, applicationContext);
    configureViewResolvers(registry);

    List<ViewResolver> viewResolvers = registry.getViewResolvers();
    if (viewResolvers.isEmpty() && applicationContext != null) {
      Set<String> names = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
              applicationContext, ViewResolver.class, true, false);
      if (names.size() == 1) {
        if (ServletDetector.isPresent) {
          viewResolvers.add(new InternalResourceViewResolver());
        }
        else {
          // add default
          configureDefaultViewResolvers(viewResolvers);
        }
      }
    }

    ViewResolverComposite composite;
    if (ServletDetector.isPresent) {
      composite = new ServletViewResolverComposite();
    }
    else {
      composite = new ViewResolverComposite();
    }

    composite.setOrder(registry.getOrder());
    composite.setViewResolvers(viewResolvers);
    if (applicationContext != null) {
      composite.setApplicationContext(applicationContext);
    }

    if (ServletDetector.isPresent && applicationContext instanceof WebServletApplicationContext servletApp) {
      ServletViewResolverComposite viewResolverComposite = (ServletViewResolverComposite) composite;
      viewResolverComposite.setServletContext(servletApp.getServletContext());
    }

    return composite;
  }

  /**
   * Override this method to configure view resolution.
   *
   * @see ViewResolverRegistry
   */
  protected void configureDefaultViewResolvers(List<ViewResolver> viewResolvers) {

  }

  /**
   * Override this method to configure view resolution.
   *
   * @see ViewResolverRegistry
   */
  protected void configureViewResolvers(ViewResolverRegistry registry) { }

  /**
   * default {@link ReturnValueHandler} registry
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  ReturnValueHandlerManager returnValueHandlerManager(
          @Nullable RedirectModelManager redirectModelManager,
          @Qualifier("webViewResolver") ViewResolver webViewResolver) {

    ReturnValueHandlerManager manager = new ReturnValueHandlerManager(getMessageConverters());

    manager.setApplicationContext(applicationContext);
    manager.setRedirectModelManager(redirectModelManager);
    manager.setViewResolver(webViewResolver);

    ViewReturnValueHandler handler = new ViewReturnValueHandler(webViewResolver);
    handler.setModelManager(redirectModelManager);

    AsyncSupportConfigurer configurer = getAsyncSupportConfigurer();
    if (configurer.getTaskExecutor() != null) {
      manager.setTaskExecutor(configurer.getTaskExecutor());
    }

    manager.setViewReturnValueHandler(handler);
    manager.addRequestResponseBodyAdvice(requestResponseBodyAdvice);
    manager.registerDefaultHandlers();

    modifyReturnValueHandlerManager(manager);

    return manager;
  }

  protected void modifyReturnValueHandlerManager(ReturnValueHandlerManager manager) { }

  /**
   * default {@link ParameterResolvingStrategy} registry
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  ParameterResolvingRegistry parameterResolvingRegistry(
          ParameterResolvingStrategy[] resolvingStrategies,
          @Nullable RedirectModelManager redirectModelManager) {

    ParameterResolvingRegistry registry = new ParameterResolvingRegistry();
    registry.setApplicationContext(getApplicationContext());
    registry.setRedirectModelManager(redirectModelManager);

    registry.setMessageConverters(getMessageConverters());
    registry.addRequestResponseBodyAdvice(requestResponseBodyAdvice);

    registry.registerDefaultStrategies();
    registry.addCustomizedStrategies(resolvingStrategies);

    modifyParameterResolvingRegistry(registry);
    return registry;
  }

  protected void modifyParameterResolvingRegistry(ParameterResolvingRegistry registry) { }

  /**
   * default {@link NotFoundHandler} to handle request-url not found
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(NotFoundHandler.class)
  NotFoundHandler notFoundHandler() {
    return new NotFoundHandler();
  }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(ViewControllerHandlerMapping.class)
  ViewControllerHandlerMapping viewControllerHandlerRegistry(
          ParameterResolvingRegistry resolvingRegistry, Environment environment) throws Exception {
    if (TodayStrategies.getFlag(ENABLE_WEB_MVC_XML, true)) {
      // find the configure file
      log.info("Framework is looking for ViewController configuration file");
      var registry = new ViewControllerHandlerMapping(
              new ParameterResolvingRegistryResolvableParameterFactory(resolvingRegistry));

      String webMvcConfigLocation = environment.getProperty(WEB_MVC_CONFIG_LOCATION);
      if (StringUtils.isEmpty(webMvcConfigLocation)) {
        webMvcConfigLocation = "classpath:web-mvc.xml";
        if (new ClassPathResource(webMvcConfigLocation).exists()) {
          log.info("web mvc configuration file does not exist, using default '{}'", webMvcConfigLocation);
        }
        else {
          webMvcConfigLocation = null;
        }
      }

      if (webMvcConfigLocation != null) {
        registry.configure(webMvcConfigLocation);
        registry.setInterceptors(getInterceptors());

        configureViewController(registry);
        return registry;
      }
    }
    return null;
  }

  protected void configureViewController(ViewControllerHandlerMapping registry) { }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  ViewControllerHandlerAdapter viewControllerHandlerAdapter() {
    return new ViewControllerHandlerAdapter(Ordered.HIGHEST_PRECEDENCE + 2);
  }

  // HandlerExceptionHandler

  /**
   * Returns a {@link CompositeHandlerExceptionHandler} containing a list of exception
   * resolvers obtained either through {@link #configureExceptionHandlers} or
   * through {@link #addDefaultHandlerExceptionHandlers}.
   * <p><strong>Note:</strong> This method cannot be made final due to CGLIB constraints.
   * Rather than overriding it, consider overriding {@link #configureExceptionHandlers}
   * which allows for providing a list of resolvers.
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public HandlerExceptionHandler handlerExceptionHandler() {
    ArrayList<HandlerExceptionHandler> handlers = new ArrayList<>();
    configureExceptionHandlers(handlers);
    if (handlers.isEmpty()) {
      addDefaultHandlerExceptionHandlers(handlers);
    }
    extendExceptionHandlers(handlers);
    CompositeHandlerExceptionHandler composite = new CompositeHandlerExceptionHandler();
    composite.setOrder(0);
    composite.setExceptionHandlers(handlers);
    return composite;
  }

  /**
   * Override this method to configure the list of
   * {@link HandlerExceptionHandler HandlerExceptionHandlers} to use.
   * <p>Adding resolvers to the list turns off the default resolvers that would otherwise
   * be registered by default. Also see {@link #addDefaultHandlerExceptionHandlers}
   * that can be used to add the default exception resolvers.
   *
   * @param handlers a list to add exception handlers to (initially an empty list)
   */
  protected void configureExceptionHandlers(List<HandlerExceptionHandler> handlers) { }

  /**
   * Override this method to extend or modify the list of
   * {@link HandlerExceptionHandler HandlerExceptionHandlers} after it has been configured.
   * <p>This may be useful for example to allow default resolvers to be registered
   * and then insert a custom one through this method.
   *
   * @param handlers the list of configured resolvers to extend.
   */
  protected void extendExceptionHandlers(List<HandlerExceptionHandler> handlers) { }

  /**
   * A method available to subclasses for adding default
   * {@link HandlerExceptionHandler HandlerExceptionHandlers}.
   * <p>Adds the following exception resolvers:
   * <ul>
   * <li>{@link ExceptionHandlerAnnotationExceptionHandler} for handling exceptions through
   * {@link cn.taketoday.web.annotation.ExceptionHandler} methods.
   * <li>{@link ResponseStatusExceptionHandler} for exceptions annotated with
   * {@link cn.taketoday.web.annotation.ResponseStatus}.
   * <li>{@link SimpleHandlerExceptionHandler} for resolving known Framework exception types
   * </ul>
   */
  protected final void addDefaultHandlerExceptionHandlers(List<HandlerExceptionHandler> handlers) {
    ExceptionHandlerAnnotationExceptionHandler handler = createExceptionHandlerAnnotationExceptionHandler();

    if (this.applicationContext != null) {
      handler.setApplicationContext(this.applicationContext);
    }
    handler.afterPropertiesSet();
    handlers.add(handler);

    ResponseStatusExceptionHandler responseStatusResolver = new ResponseStatusExceptionHandler();
    responseStatusResolver.setMessageSource(this.applicationContext);
    handlers.add(responseStatusResolver);

    handlers.add(new SimpleHandlerExceptionHandler());
  }

  /**
   * Protected method for plugging in a custom subclass of
   * {@link ExceptionHandlerAnnotationExceptionHandler}.
   */
  protected ExceptionHandlerAnnotationExceptionHandler createExceptionHandlerAnnotationExceptionHandler() {
    return new ExceptionHandlerAnnotationExceptionHandler();
  }

  // HandlerRegistry

  /**
   * core {@link HandlerMapping} to register handler
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(RequestMappingHandlerMapping.class)
  RequestMappingHandlerMapping requestMappingHandlerMapping(
          ContentNegotiationManager contentNegotiationManager
  ) {
    RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();

    PathMatchConfigurer configurer = getPathMatchConfigurer();

    Boolean useTrailingSlashMatch = configurer.isUseTrailingSlashMatch();
    if (useTrailingSlashMatch != null) {
      handlerMapping.setUseTrailingSlashMatch(useTrailingSlashMatch);
    }

    Boolean useCaseSensitiveMatch = configurer.isUseCaseSensitiveMatch();
    if (useCaseSensitiveMatch != null) {
      handlerMapping.setUseCaseSensitiveMatch(useCaseSensitiveMatch);
    }
    handlerMapping.setInterceptors(getInterceptors());

    handlerMapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
    handlerMapping.setContentNegotiationManager(contentNegotiationManager);
    return handlerMapping;
  }

  /**
   * core {@link cn.taketoday.web.handler.method.AnnotationHandlerFactory} to create annotation-handler
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public AnnotationHandlerFactory annotationHandlerFactory(
          BeanFactory beanFactory, ParameterResolvingRegistry registry, ReturnValueHandlerManager manager) {
    AnnotationHandlerFactory handlerFactory = new AnnotationHandlerFactory(beanFactory);
    handlerFactory.setReturnValueHandlerManager(manager);
    handlerFactory.setParameterResolvingRegistry(registry);
    return handlerFactory;
  }

  /**
   * Return a handler mapping ordered at Integer.MAX_VALUE-1 with mapped
   * resource handlers. To configure resource handling, override
   * {@link #addResourceHandlers}.
   */
  @Nullable
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public HandlerMapping resourceHandlerMapping(
          @Nullable ContentNegotiationManager contentNegotiationManager) {

    Assert.state(this.applicationContext != null, "No ApplicationContext set");
    PathMatchConfigurer pathConfig = getPathMatchConfigurer();

    ResourceHandlerRegistry registry = new ResourceHandlerRegistry(this.applicationContext, contentNegotiationManager);
    addResourceHandlers(registry);

    SimpleUrlHandlerMapping handlerRegistry = registry.getHandlerMapping();
    if (handlerRegistry == null) {
      return null;
    }

    handlerRegistry.setCorsConfigurations(getCorsConfigurations());
    handlerRegistry.setInterceptors(getInterceptors());
    handlerRegistry.setUseCaseSensitiveMatch(Boolean.TRUE.equals(pathConfig.isUseCaseSensitiveMatch()));
    handlerRegistry.setUseTrailingSlashMatch(Boolean.TRUE.equals(pathConfig.isUseTrailingSlashMatch()));

    return handlerRegistry;
  }

  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public HandlerMapping webFunctionHandlerRegistry() {
    FunctionHandlerMapping functionHandlerRegistry = new FunctionHandlerMapping();
    functionHandlerRegistry.setApplicationContext(getApplicationContext());
    functionHandlerRegistry.setCorsConfigurations(getCorsConfigurations());

    configureFunctionHandler(functionHandlerRegistry);
    return functionHandlerRegistry;
  }

  protected void configureFunctionHandler(FunctionHandlerMapping functionHandlerRegistry) { }

  /**
   * Return a handler mapping ordered at 1 to map URL paths directly to
   * view names. To configure view controllers, override
   * {@link #addViewControllers}.
   */
  @Component
  @Nullable
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public HandlerMapping viewControllerHandlerRegistry() {
    ViewControllerRegistry registry = new ViewControllerRegistry(this.applicationContext);
    addViewControllers(registry);

    AbstractHandlerMapping mapping = registry.buildHandlerMapping();
    if (mapping == null) {
      return null;
    }
    PathMatchConfigurer pathConfig = getPathMatchConfigurer();

    mapping.setUseCaseSensitiveMatch(Boolean.TRUE.equals(pathConfig.isUseCaseSensitiveMatch()));
    mapping.setUseTrailingSlashMatch(Boolean.TRUE.equals(pathConfig.isUseTrailingSlashMatch()));

    mapping.setInterceptors(getInterceptors());
    mapping.setCorsConfigurations(getCorsConfigurations());
    return mapping;
  }

  /**
   * Override this method to add view controllers.
   *
   * @see ViewControllerRegistry
   */
  protected void addViewControllers(ViewControllerRegistry registry) { }

  /**
   * Return a handler mapping ordered at Integer.MAX_VALUE with a mapped
   * default servlet handler. To configure "default" Servlet handling,
   * override {@link #configureDefaultServletHandling}.
   */
  @Component
  @Nullable
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public HandlerMapping defaultServletHandlerMapping() {
    if (ServletDetector.isPresent) {
      if (getApplicationContext() instanceof WebServletApplicationContext context) {
        ServletContext servletContext = context.getServletContext();
        if (servletContext != null) {
          DefaultServletHandlerConfigurer configurer = new DefaultServletHandlerConfigurer(servletContext);
          configureDefaultServletHandling(configurer);

          return configurer.buildHandlerMapping();
        }
      }
    }
    return null;
  }

  /**
   * Override this method to configure "default" Servlet handling.
   *
   * @see DefaultServletHandlerConfigurer
   */
  protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {

  }

  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  FunctionRequestAdapter functionRequestAdapter() {
    return new FunctionRequestAdapter(Ordered.HIGHEST_PRECEDENCE + 1);
  }

  /**
   * Override this method to add resource handlers for serving static resources.
   *
   * @see ResourceHandlerRegistry
   */
  protected void addResourceHandlers(ResourceHandlerRegistry registry) { }

  /**
   * A {@link ResourceUrlProvider} bean for use with the MVC dispatcher.
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public ResourceUrlProvider mvcResourceUrlProvider() {
    return new ResourceUrlProvider();
  }

  /**
   * Return the registered {@link CorsConfiguration} objects,
   * keyed by path pattern.
   */
  protected final Map<String, CorsConfiguration> getCorsConfigurations() {
    if (this.corsConfigurations == null) {
      CorsRegistry registry = new CorsRegistry();
      addCorsMappings(registry);
      this.corsConfigurations = registry.getCorsConfigurations();
    }
    return this.corsConfigurations;
  }

  /**
   * Override this method to configure cross origin requests processing.
   *
   * @see CorsRegistry
   */
  protected void addCorsMappings(CorsRegistry registry) { }

  /**
   * Provide access to the shared handler interceptors used to configure
   * {@link HandlerMapping} instances with.
   * <p>This method cannot be overridden; use {@link #addInterceptors} instead.
   */
  protected final Object[] getInterceptors() {
    if (this.interceptors == null) {
      InterceptorRegistry registry = new InterceptorRegistry();
      addInterceptors(registry);
      this.interceptors = registry.getInterceptors();
    }
    return this.interceptors.toArray();
  }

  /**
   * Override this method to add Framework MVC interceptors for
   * pre- and post-processing of controller invocation.
   *
   * @see InterceptorRegistry
   */
  protected void addInterceptors(InterceptorRegistry registry) { }

  /**
   * Return a {@link FormattingConversionService} for use with annotated controllers.
   * <p>See {@link #addFormatters} as an alternative to overriding this method.
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public FormattingConversionService mvcConversionService() {
    FormattingConversionService conversionService = new DefaultFormattingConversionService();
    addFormatters(conversionService);
    return conversionService;
  }

  /**
   * Override this method to add custom {@link Converter} and/or {@link Formatter}
   * delegates to the common {@link FormattingConversionService}.
   *
   * @see #mvcConversionService()
   */
  protected void addFormatters(FormatterRegistry registry) { }

  /**
   * Returns a {@link RequestMappingHandlerAdapter} for processing requests
   * through annotated controller methods. Consider overriding one of these
   * other more fine-grained methods:
   * <ul>
   * <li>{@link #modifyParameterResolvingRegistry(ParameterResolvingRegistry)} for adding custom argument resolvers.
   * <li>{@link #modifyReturnValueHandlerManager(ReturnValueHandlerManager)} for adding custom return value handlers.
   * <li>{@link #configureMessageConverters} for adding custom message converters.
   * </ul>
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public RequestMappingHandlerAdapter requestMappingHandlerAdapter(
          @Qualifier("mvcConversionService") FormattingConversionService conversionService,
          ParameterResolvingRegistry parameterResolvingRegistry,
          ReturnValueHandlerManager returnValueHandlerManager,
          @Qualifier("mvcValidator") Validator validator) {

    RequestMappingHandlerAdapter adapter = createRequestMappingHandlerAdapter();
    adapter.setWebBindingInitializer(getConfigurableWebBindingInitializer(conversionService, validator));
    adapter.setResolvingRegistry(parameterResolvingRegistry);
    adapter.setReturnValueHandlerManager(returnValueHandlerManager);

    AsyncSupportConfigurer configurer = getAsyncSupportConfigurer();
    if (configurer.getTaskExecutor() != null) {
      adapter.setTaskExecutor(configurer.getTaskExecutor());
    }

    if (configurer.getTimeout() != null) {
      adapter.setAsyncRequestTimeout(configurer.getTimeout());
    }

    adapter.setCallableInterceptors(configurer.getCallableInterceptors());
    adapter.setDeferredResultInterceptors(configurer.getDeferredResultInterceptors());

    return adapter;
  }

  /**
   * Protected method for plugging in a custom subclass of
   * {@link RequestMappingHandlerAdapter}.
   */
  protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
    return new RequestMappingHandlerAdapter();
  }

  /**
   * Return the {@link ConfigurableWebBindingInitializer} to use for
   * initializing all {@link WebDataBinder} instances.
   */
  protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer(
          FormattingConversionService mvcConversionService, Validator mvcValidator) {

    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(mvcConversionService);
    initializer.setValidator(mvcValidator);
    MessageCodesResolver messageCodesResolver = getMessageCodesResolver();
    if (messageCodesResolver != null) {
      initializer.setMessageCodesResolver(messageCodesResolver);
    }
    return initializer;
  }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  HandlerExecutionChainHandlerAdapter handlerExecutionChainHandlerAdapter(ApplicationContext context) {
    return new HandlerExecutionChainHandlerAdapter(context);
  }

  /**
   * Return a global {@link Validator} instance for example for validating
   * {@code @ModelAttribute} and {@code @RequestBody} method arguments.
   * Delegates to {@link #getValidator()} first and if that returns {@code null}
   * checks the classpath for the presence of a JSR-303 implementations
   * before creating a {@code OptionalValidatorFactoryBean}.If a JSR-303
   * implementation is not available, a no-op {@link Validator} is returned.
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public Validator mvcValidator() {
    Validator validator = getValidator();
    if (validator == null) {
      if (ClassUtils.isPresent("jakarta.validation.Validator", getClass().getClassLoader())) {
        validator = new OptionalValidatorFactoryBean();
      }
      else {
        validator = new NoOpValidator();
      }
    }
    return validator;
  }

  /**
   * Override this method to provide a custom {@link MessageCodesResolver}.
   */
  @Nullable
  protected MessageCodesResolver getMessageCodesResolver() {
    return null;
  }

  /**
   * Override this method to provide a custom {@link Validator}.
   */
  @Nullable
  protected Validator getValidator() {
    return null;
  }

  /**
   * Callback for building the {@link AsyncSupportConfigurer}.
   * Delegates to {@link #configureAsyncSupport(AsyncSupportConfigurer)}.
   */
  protected AsyncSupportConfigurer getAsyncSupportConfigurer() {
    if (asyncSupportConfigurer == null) {
      this.asyncSupportConfigurer = new AsyncSupportConfigurer();
      configureAsyncSupport(asyncSupportConfigurer);
    }
    return asyncSupportConfigurer;
  }

  /**
   * Override this method to configure asynchronous request processing options.
   *
   * @see AsyncSupportConfigurer
   */
  protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {

  }

  static boolean isPresent(String name) {
    ClassLoader classLoader = WebMvcAutoConfiguration.class.getClassLoader();
    return ClassUtils.isPresent(name, classLoader);
  }

  private static final class NoOpValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
      return false;
    }

    @Override
    public void validate(@Nullable Object target, Errors errors) { }

  }

}