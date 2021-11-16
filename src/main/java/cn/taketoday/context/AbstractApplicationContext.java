/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.factory.AbstractBeanFactory;
import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.BeanPostProcessor;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.beans.support.BeanFactoryAwareBeanInstantiator;
import cn.taketoday.context.autowire.AutowiredDependencyResolvingStrategy;
import cn.taketoday.context.aware.ApplicationContextAwareProcessor;
import cn.taketoday.context.event.ApplicationEventPublisher;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.context.event.ContextClosedEvent;
import cn.taketoday.context.event.ContextPreRefreshEvent;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.context.event.ContextStartedEvent;
import cn.taketoday.context.event.ContextStoppedEvent;
import cn.taketoday.context.event.DefaultApplicationEventPublisher;
import cn.taketoday.context.event.EventListener;
import cn.taketoday.context.expression.ExpressionEvaluator;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Abstract implementation of the {@link ApplicationContext}
 * interface. Doesn't mandate the type of storage used for configuration; simply
 * implements common context functionality. Uses the Template Method design pattern,
 * requiring concrete subclasses to implement abstract methods.
 *
 * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed
 * to detect special beans defined in its internal bean factory:
 * Therefore, this class automatically registers
 * {@link BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * {@link BeanPostProcessor BeanPostProcessors}, and
 * {@link ApplicationListener ApplicationListeners} which are defined as beans in the context.
 *
 * <p>Implements resource loading by extending {@link PathMatchingPatternResourceLoader}.
 * Consequently, treats non-URL resource paths as class path resources
 * (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat")
 *
 * @author TODAY 2018-09-09 22:02
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractApplicationContext
        extends DefaultResourceLoader implements ConfigurableApplicationContext, Lifecycle {
  private static final Logger log = LoggerFactory.getLogger(AbstractApplicationContext.class);

  /**
   * Name of the LifecycleProcessor bean in the factory.
   * If none is supplied, a DefaultLifecycleProcessor is used.
   *
   * @see LifecycleProcessor
   * @see DefaultLifecycleProcessor
   */
  public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

  private long startupDate;

  private ConfigurableEnvironment environment;

  // @since 2.1.5
  private State state = State.NONE;

  private ArrayList<BeanFactoryPostProcessor> factoryPostProcessors;

  /** Unique id for this context, if any. @since 4.0 */
  private String id = ObjectUtils.identityToString(this);

  /** Parent context. @since 4.0 */
  @Nullable
  private ApplicationContext parent;

  /** Display name. */
  private String applicationName = ObjectUtils.identityToString(this);

  /** @since 4.0 */
  private ApplicationEventPublisher eventPublisher;

  /** @since 4.0 */
  private BeanFactoryAwareBeanInstantiator beanInstantiator;

  /** @since 4.0 */
  private final PathMatchingPatternResourceLoader patternResourceLoader
          = new PathMatchingPatternResourceLoader(this);

  /** @since 4.0 */
  private boolean refreshable;

  /** @since 4.0 */
  private ExpressionEvaluator expressionEvaluator;

  /** Flag that indicates whether this context has been closed already.  @since 4.0 */
  private final AtomicBoolean closed = new AtomicBoolean();

  /** Reference to the JVM shutdown hook, if registered. */
  @Nullable
  private Thread shutdownHook;

  /** LifecycleProcessor for managing the lifecycle of beans within this context. @since 4.0 */
  @Nullable
  private LifecycleProcessor lifecycleProcessor;

  public AbstractApplicationContext() { }

  /**
   * Create a new AbstractApplicationContext with the given parent context.
   *
   * @param parent the parent context
   */
  public AbstractApplicationContext(@Nullable ApplicationContext parent) {
    this();
    setParent(parent);
  }

  //---------------------------------------------------------------------
  // Implementation of PatternResourceLoader interface
  //---------------------------------------------------------------------

  @Override
  public Set<Resource> getResources(String locationPattern) throws IOException {
    return patternResourceLoader.getResources(locationPattern);
  }

  //---------------------------------------------------------------------
  // Implementation of ApplicationContext interface
  //---------------------------------------------------------------------

  /**
   * Set the unique id of this application context.
   * <p>Default is the object id of the context instance, or the name
   * of the context bean if the context is itself defined as a bean.
   *
   * @param id the unique id of the context
   */
  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return this.id;
  }

  /**
   * Set a friendly name for this context.
   * Typically, done during initialization of concrete context implementations.
   * <p>Default is the object id of the context instance.
   */
  public void setApplicationName(String applicationName) {
    Assert.hasLength(applicationName, "Application name must not be empty");
    ApplicationContextHolder.remove(this);
    this.applicationName = applicationName;
    ApplicationContextHolder.register(this); // @since 4.0
  }

  /**
   * Return this application name for this context.
   *
   * @return a display name for this context (never {@code null})
   */
  @Override
  public String getApplicationName() {
    return applicationName;
  }

  /**
   * Return the parent context, or {@code null} if there is no parent
   * (that is, this context is the root of the context hierarchy).
   */
  @Override
  @Nullable
  public ApplicationContext getParent() {
    return this.parent;
  }

  @Override
  public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
    return getBeanFactory();
  }

  @Override
  public ExpressionEvaluator getExpressionEvaluator() {
    if (expressionEvaluator == null) {
      expressionEvaluator = new ExpressionEvaluator(this);
    }
    return expressionEvaluator;
  }

  //---------------------------------------------------------------------
  // Implementation of HierarchicalBeanFactory interface
  //---------------------------------------------------------------------

  @Override
  @Nullable
  public BeanFactory getParentBeanFactory() {
    return getParent();
  }

  @Override
  public boolean containsLocalBean(String name) {
    return getBeanFactory().containsLocalBean(name);
  }

  /**
   * Return the internal bean factory of the parent context if it implements
   * ConfigurableApplicationContext; else, return the parent context itself.
   *
   * @see ConfigurableApplicationContext#unwrapFactory
   */
  @Nullable
  protected BeanFactory getInternalParentBeanFactory() {
    return (getParent() instanceof ConfigurableApplicationContext ?
            ((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
  }

  /**
   * Reset reflection metadata caches, in particular the
   * {@link ReflectionUtils}, {@link AnnotationUtils}, {@link ResolvableType}
   *
   * @see ReflectionUtils#clearCache()
   * @see AnnotationUtils#clearCache()
   * @see ResolvableType#clearCache()
   * @since 4.0
   */
  protected void resetCommonCaches() {
    ReflectionUtils.clearCache();
    AnnotationUtils.clearCache();
    ResolvableType.clearCache();
  }

  /**
   * Register a shutdown hook {@linkplain Thread#getName() named}
   * {@code ContextShutdownHook} with the JVM runtime, closing this
   * context on JVM shutdown unless it has already been closed at that time.
   * <p>Delegates to {@code doClose()} for the actual closing procedure.
   *
   * @see Runtime#addShutdownHook
   * @see ConfigurableApplicationContext#SHUTDOWN_HOOK_THREAD_NAME
   * @see #close()
   * @see #doClose()
   */
  @Override
  public void registerShutdownHook() {
    if (this.shutdownHook == null) {
      // No shutdown hook registered yet.
      this.shutdownHook = new Thread(SHUTDOWN_HOOK_THREAD_NAME) {
        @Override
        public void run() {
          doClose();
        }
      };
      Runtime.getRuntime().addShutdownHook(this.shutdownHook);
    }
  }

  /**
   * Prepare to load context
   */
  protected void prepareRefresh() {
    this.startupDate = System.currentTimeMillis();
    log.info("Starting Application Context at [{}].", formatStartupDate());

    applyState(State.STARTING);
    ConfigurableEnvironment environment = getEnvironment();

    // Initialize any placeholder property sources in the context environment.
    initPropertySources(environment);
    environment.validateRequiredProperties();

    ConfigurableBeanFactory beanFactory = getBeanFactory();

    // @since 2.1.6
    if (environment.getFlag(ENABLE_FULL_PROTOTYPE)) {
      beanFactory.setFullPrototype(true);
    }
    if (environment.getFlag(ENABLE_FULL_LIFECYCLE)) {
      beanFactory.setFullLifecycle(true);
    }
    // @since 4.0
    String appName = environment.getProperty(APPLICATION_NAME);
    if (StringUtils.hasText(appName)) {
      setApplicationName(appName);
    }
    ApplicationContextHolder.register(this); // @since 4.0
  }

  /**
   * Finish the initialization of this context's bean factory,
   * initializing all remaining singleton beans.
   */
  protected void finishBeanFactoryInitialization(ConfigurableBeanFactory beanFactory) {
    // Initialize conversion service for this context.
    if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
            beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
      beanFactory.setConversionService(
              beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
    }

    // Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.

    // Stop using the temporary ClassLoader for type matching.
    beanFactory.setTempClassLoader(null);

    // Instantiate all remaining (non-lazy-init) singletons.
    beanFactory.initializeSingletons();
  }

  /**
   * Context start success
   */
  protected void finishRefresh() {
    clearResourceCaches();

    // Initialize lifecycle processor for this context.
    initLifecycleProcessor();

    // Propagate refresh to lifecycle processor first.
    getLifecycleProcessor().onRefresh();

    // Publish the final event.
    publishEvent(new ContextRefreshedEvent(this));

    applyState(State.STARTED);
    log.info("Application Context Startup in {}ms", System.currentTimeMillis() - getStartupDate());
  }

  //---------------------------------------------------------------------
  // Implementation of Lifecycle interface
  //---------------------------------------------------------------------

  @Override
  public void start() {
    getLifecycleProcessor().start();
    publishEvent(new ContextStartedEvent(this));
  }

  @Override
  public void stop() {
    getLifecycleProcessor().stop();
    publishEvent(new ContextStoppedEvent(this));
  }

  @Override
  public boolean isRunning() {
    return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
  }

  // lifecycleProcessor

  // @since 4.0
  public void setLifecycleProcessor(@Nullable LifecycleProcessor lifecycleProcessor) {
    this.lifecycleProcessor = lifecycleProcessor;
  }

  /**
   * Initialize the LifecycleProcessor.
   * Uses DefaultLifecycleProcessor if none defined in the context.
   *
   * @see DefaultLifecycleProcessor
   */
  protected void initLifecycleProcessor() {
    if (lifecycleProcessor == null) {
      ConfigurableBeanFactory beanFactory = getBeanFactory();
      if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
        this.lifecycleProcessor = beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
        if (log.isTraceEnabled()) {
          log.trace("Using LifecycleProcessor [{}]", lifecycleProcessor);
        }
      }
      else {
        DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
        defaultProcessor.setBeanFactory(beanFactory);
        this.lifecycleProcessor = defaultProcessor;
        beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
        if (log.isTraceEnabled()) {
          log.trace("No '{}' bean, using [{}]", LIFECYCLE_PROCESSOR_BEAN_NAME, lifecycleProcessor.getClass().getSimpleName());
        }
      }
    }
  }

  /**
   * Return the internal LifecycleProcessor used by the context.
   *
   * @return the internal LifecycleProcessor (never {@code null})
   * @throws IllegalStateException if the context has not been initialized yet
   */
  LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
    if (this.lifecycleProcessor == null) {
      throw new IllegalStateException("LifecycleProcessor not initialized - " +
                                              "call 'refresh' before invoking lifecycle methods via the context: " + this);
    }
    return this.lifecycleProcessor;
  }

  /**
   * Initialization singletons that has already in context
   */
  protected void onRefresh() {
    publishEvent(new ContextPreRefreshEvent(this));
    // fix: #1 some singletons could not be initialized.
    getBeanFactory().preInitialization();
  }

  public void prepareBeanFactory(ConfigurableBeanFactory beanFactory) {
    log.info("Preparing internal bean-factory");
    // Tell the internal bean factory to use the context's class loader etc.
    beanFactory.setBeanClassLoader(getClassLoader());

    // Detect a LoadTimeWeaver and prepare for weaving, if found.
    if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
      // Set a temporary ClassLoader for type matching.
      beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
    }

    // register bean post processors
    beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
    beanFactory.addDependencyResolvingStrategies(new AutowiredDependencyResolvingStrategy(this));

    beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
    beanFactory.registerResolvableDependency(ApplicationContext.class, this);
  }

  /**
   * register Framework Beans
   */
  public void registerFrameworkComponents() {
    registerFrameworkComponents(getBeanFactory());
  }

  /**
   * Register Framework Beans
   */
  protected void registerFrameworkComponents(ConfigurableBeanFactory beanFactory) {
    log.info("Registering framework beans");
    // register Environment
    beanFactory.registerSingleton(Environment.ENVIRONMENT_BEAN_NAME, getEnvironment());
    // @since 4.0 ArgumentsResolver
    beanFactory.registerSingleton(getArgumentsResolver());

    ExpressionEvaluator.register(beanFactory, getEnvironment());
  }

  public String createBeanName(Class<?> clazz) {
    return BeanDefinitionBuilder.defaultBeanName(clazz);
  }

  // post-processor

  /**
   * Modify the application context's internal bean factory after its standard
   * initialization. All bean definitions will have been loaded, but no beans
   * will have been instantiated yet. This allows for registering special
   * BeanPostProcessors etc in certain ApplicationContext implementations.
   *
   * @param beanFactory the bean factory used by the application context
   */
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    log.info("Loading BeanFactoryPostProcessor.");
    List<BeanFactoryPostProcessor> postProcessors = getBeans(BeanFactoryPostProcessor.class);
    if (!postProcessors.isEmpty()) {
      getFactoryPostProcessors().addAll(postProcessors);
      AnnotationAwareOrderComparator.sort(factoryPostProcessors);
    }
  }

  /**
   * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
   * respecting explicit order if given.
   * <p>Must be called before singleton instantiation.
   */
  protected void invokeBeanFactoryPostProcessors(ConfigurableBeanFactory beanFactory) {
    if (CollectionUtils.isNotEmpty(factoryPostProcessors)) {
      for (BeanFactoryPostProcessor postProcessor : factoryPostProcessors) {
        postProcessor.postProcessBeanFactory(beanFactory);
      }
    }
    // Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
    // (e.g. through an @Component method registered by ConfigurationBeanReader)
    if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
      beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
    }
  }

  /**
   * Instantiate and register all BeanPostProcessor beans,
   * respecting explicit order if given.
   * <p>Must be called before any instantiation of application beans.
   */
  protected void registerBeanPostProcessors(ConfigurableBeanFactory beanFactory) {
    log.info("Loading BeanPostProcessor.");
    List<BeanPostProcessor> postProcessors = beanFactory.getBeans(BeanPostProcessor.class);
    if (beanFactory instanceof AbstractBeanFactory) {
      ((AbstractBeanFactory) beanFactory).addBeanPostProcessors(postProcessors);
    }
    else {
      for (BeanPostProcessor postProcessor : postProcessors) {
        beanFactory.addBeanPostProcessor(postProcessor);
      }
    }
  }

  //---------------------------------------------------------------------
  // Implementation of ApplicationContext interface
  //---------------------------------------------------------------------

  @Override
  public void refresh() throws IllegalStateException {
    assertRefreshable();
    // Prepare refresh
    prepareRefresh();

    ConfigurableBeanFactory beanFactory = getBeanFactory();

    // register framework beans
    registerFrameworkComponents(beanFactory);

    // Prepare BeanFactory
    prepareBeanFactory(beanFactory);

    try {
      // Allows post-processing of the bean factory in context subclasses.
      postProcessBeanFactory(beanFactory);

      // Invoke factory processors registered as beans in the context.
      invokeBeanFactoryPostProcessors(beanFactory);

      // Register bean processors that intercept bean creation.
      registerBeanPostProcessors(beanFactory);

      // Initialization singletons that has already in context
      // Initialize other special beans in specific context subclasses.
      // for example a Web Server
      onRefresh();

      // Check for listener beans and register them.
      registerApplicationListeners();

      // Instantiate all remaining (non-lazy-init) singletons.
      finishBeanFactoryInitialization(beanFactory);

      // Finish refresh
      finishRefresh();
    }
    catch (Exception ex) {
      if (log.isWarnEnabled()) {
        log.warn("Exception encountered during context initialization - cancelling refresh attempt: " + ex);
      }

      applyState(State.FAILED);
      cancelRefresh(ex);
      throw new ApplicationContextException("context refresh failed", ex);
    }
    finally {
      resetCommonCaches();
    }
  }

  /**
   * Cancel this context's refresh attempt, after an exception got thrown.
   *
   * @param ex the exception that led to the cancellation
   */
  protected void cancelRefresh(Exception ex) {
    close();
  }

  private void assertRefreshable() {
    if (!refreshable &&
            (state == State.STARTED || state == State.STARTING || state == State.CLOSING)) {
      throw new IllegalStateException("this context not supports refresh again");
    }
  }

  /**
   * <p>
   * load properties files or itself strategies
   *
   * @param environment ConfigurableEnvironment
   */
  protected void initPropertySources(ConfigurableEnvironment environment) throws ApplicationContextException {
    // for sub-class loading properties or prepare property-source
  }

  @Override
  public abstract AbstractBeanFactory getBeanFactory();

  @Override
  public void close() {
    applyState(State.CLOSING);
    doClose();
    applyState(State.CLOSED);
    ApplicationContextHolder.remove(this);
  }

  /**
   * Actually performs context closing: publishes a ContextClosedEvent and
   * destroys the singletons in the bean factory of this application context.
   * <p>Called by both {@code close()} and a JVM shutdown hook, if any.
   *
   * @see ContextClosedEvent
   * @see #destroyBeans()
   * @see #close()
   * @since 4.0
   */
  protected void doClose() {
    // Check whether an actual close attempt is necessary...
    if (this.closed.compareAndSet(false, true)) {
      log.info("Closing: [{}] at [{}]", this,
               new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(System.currentTimeMillis()));

      try {
        // Publish shutdown event.
        publishEvent(new ContextClosedEvent(this));
      }
      catch (Throwable ex) {
        log.warn("Exception thrown from ApplicationListener handling ContextCloseEvent", ex);
      }
      // Stop all Lifecycle beans, to avoid delays during individual destruction.
      if (this.lifecycleProcessor != null) {
        try {
          this.lifecycleProcessor.onClose();
        }
        catch (Throwable ex) {
          log.warn("Exception thrown from LifecycleProcessor on context close", ex);
        }
      }
      // Destroy all cached singletons in the context's BeanFactory.
      destroyBeans();

      // Close the state of this context itself.
      closeBeanFactory();

      // Let subclasses do some final clean-up if they wish...
      onClose();
    }
  }

  /**
   * Template method which can be overridden to add context-specific shutdown work.
   * The default implementation is empty.
   * <p>Called at the end of {@link #doClose}'s shutdown procedure, after
   * this context's BeanFactory has been closed. If custom shutdown logic
   * needs to execute while the BeanFactory is still active, override
   * the {@link #destroyBeans()} method instead.
   *
   * @since 4.0
   */
  protected void onClose() {
    // For subclasses: do nothing by default.
  }

  /**
   * Subclasses must implement this method to release their internal bean factory.
   * This method gets invoked by {@link #close()} after all other shutdown work.
   * <p>Should never throw an exception but rather log shutdown failures.
   *
   * @since 4.0
   */
  protected void closeBeanFactory() { }

  /**
   * Template method for destroying all beans that this context manages.
   * The default implementation destroy all cached singletons in this context,
   * invoking {@code DisposableBean.destroy()} and/or the specified
   * "destroy-method".
   * <p>Can be overridden to add context-specific bean destruction steps
   * right before or right after standard singleton destruction,
   * while the context's BeanFactory is still active.
   *
   * @see #getBeanFactory()
   * @see ConfigurableBeanFactory#destroySingletons()
   * @since 4.0
   */
  protected void destroyBeans() {
    getBeanFactory().destroySingletons();
  }

  @NonNull
  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrapFactory(Class<T> requiredType) {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    if (requiredType.isInstance(beanFactory)) {
      return (T) beanFactory;
    }
    throw new IllegalArgumentException("bean factory must be a " + requiredType);
  }

  @NonNull
  @Override
  public <T> T unwrap(Class<T> requiredType) {
    if (requiredType.isInstance(this)) {
      return (T) this;
    }
    throw new IllegalArgumentException("bean factory must be a " + requiredType);
  }

  @Override
  public boolean hasStarted() {
    return state == State.STARTED;
  }

  @Override
  public State getState() {
    return state;
  }

  protected void applyState(State state) {
    this.state = state;
  }

  @Override
  public long getStartupDate() {
    return startupDate;
  }

  //---------------------------------------------------------------------
  // Implementation of ConfigurableApplicationContext interface
  //---------------------------------------------------------------------

  /**
   * Set the parent of this application context.
   * <p>The parent {@linkplain ApplicationContext#getEnvironment() environment} is
   * {@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} with
   * this (child) application context environment if the parent is non-{@code null} and
   * its environment is an instance of {@link ConfigurableEnvironment}.
   *
   * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
   */
  @Override
  public void setParent(@Nullable ApplicationContext parent) {
    this.parent = parent;
    if (parent != null) {
      Environment parentEnvironment = parent.getEnvironment();
      if (parentEnvironment instanceof ConfigurableEnvironment) {
        getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
      }
    }
  }

  @Override
  public ConfigurableEnvironment getEnvironment() {
    if (environment == null) {
      environment = createEnvironment();
    }
    return environment;
  }

  /**
   * Create and return a new {@link StandardEnvironment}.
   * <p>Subclasses may override this method in order to supply
   * a custom {@link ConfigurableEnvironment} implementation.
   */
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardEnvironment();
  }

  @Override
  public void setEnvironment(ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  @Override
  public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
    Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");

    getFactoryPostProcessors().add(postProcessor);
  }

  @Override
  public void setRefreshable(boolean refreshable) {
    this.refreshable = refreshable;
  }

  //---------------------------------------------------------------------
  // Implementation of BeanFactory interface
  //---------------------------------------------------------------------

  /**
   * Assert that this context's BeanFactory is currently active,
   * throwing an {@link IllegalStateException} if it isn't.
   * <p>Invoked by all {@link BeanFactory} delegation methods that depend
   * on an active context, i.e. in particular all bean accessor methods.
   */
  protected void assertBeanFactoryActive() {
    if (!refreshable && !(state == State.STARTING || state == State.STARTED)) {
      if (this.closed.get()) {
        throw new IllegalStateException(getApplicationName() + " has been closed already");
      }
      else {
        throw new IllegalStateException(getApplicationName() + " has not been refreshed yet");
      }
    }
  }

  @Override
  public Object getBean(String name) {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(name);
  }

  @Override
  public Object getBean(BeanDefinition def) {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(def);
  }

  @Override
  public <T> T getBean(Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(requiredType);
  }

  @Override
  public <T> T getBean(String name, Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(name, requiredType);
  }

  @Override
  public <T> Supplier<T> getObjectSupplier(String beanName) {
    assertBeanFactoryActive();
    return getBeanFactory().getObjectSupplier(beanName);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(BeanDefinition def) {
    assertBeanFactoryActive();
    return getBeanFactory().getObjectSupplier(def);
  }

  @Override
  public Object getScopeBean(BeanDefinition def, Scope scope) {
    assertBeanFactoryActive();
    return getBeanFactory().getScopeBean(def, scope);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getObjectSupplier(requiredType);
  }

  @Override
  public <A extends Annotation> A getAnnotationOnBean(String beanName, Class<A> annotationType) {
    assertBeanFactoryActive();
    return getBeanFactory().getAnnotationOnBean(beanName, annotationType);
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> getMergedAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().getMergedAnnotationOnBean(beanName, annotationType);
  }

  @Override
  public <T> List<T> getAnnotatedBeans(Class<? extends Annotation> annotationType) {
    assertBeanFactoryActive();
    return getBeanFactory().getAnnotatedBeans(annotationType);
  }

  @Override
  public Map<String, Object> getBeansOfAnnotation(Class<? extends Annotation> annotationType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansOfAnnotation(annotationType);
  }

  @Override
  public Map<String, Object> getBeansOfAnnotation(Class<? extends Annotation> annotationType, boolean includeNonSingletons) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansOfAnnotation(annotationType, includeNonSingletons);
  }

  @Override
  public Map<String, BeanDefinition> getBeanDefinitions() {
    return getBeanFactory().getBeanDefinitions();
  }

  @Override
  public boolean isSingleton(String name) {
    assertBeanFactoryActive();
    return getBeanFactory().isSingleton(name);
  }

  @Override
  public boolean isPrototype(String name) {
    assertBeanFactoryActive();
    return getBeanFactory().isPrototype(name);
  }

  @Override
  public Class<?> getType(String name) {
    assertBeanFactoryActive();
    return getBeanFactory().getType(name);
  }

  @Override
  public String getBeanName(Class<?> targetClass) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanName(targetClass);
  }

  @Override
  public boolean isFullLifecycle() {
    return getBeanFactory().isFullLifecycle();
  }

  @Override
  public boolean isFullPrototype() {
    return getBeanFactory().isFullPrototype();
  }

  @Override
  public Set<String> getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForAnnotation(annotationType);
  }

  @Override
  public boolean containsBean(String name) {
    return getBeanFactory().containsBean(name);
  }

  @Override
  public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().isTypeMatch(name, typeToMatch);
  }

  @Override
  public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().isTypeMatch(name, typeToMatch);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(ResolvableType requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getObjectSupplier(requiredType);
  }

  // type lookup

  @Override
  public <T> List<T> getBeans(Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeans(requiredType);
  }

  @Override
  public Set<String> getBeanNamesForType(Class<?> requiredType, boolean includeNonSingletons) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(requiredType, includeNonSingletons);
  }

  @Override
  public Set<String> getBeanNamesForType(
          Class<?> requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(requiredType, includeNonSingletons, allowEagerInit);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansOfType(requiredType);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(
          Class<T> requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansOfType(requiredType, includeNonSingletons, allowEagerInit);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansOfType(requiredType, includeNonSingletons, allowEagerInit);
  }

  @Override
  public Set<String> getBeanNamesForType(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(requiredType, includeNonSingletons, allowEagerInit);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getObjectSupplier(requiredType, includeNonSingletons, allowEagerInit);
  }

  // ArgumentsResolverProvider

  @NonNull
  @Override
  public ArgumentsResolver getArgumentsResolver() {
    return getBeanFactory().getArgumentsResolver();
  }

  // @since 2.1.7
  // ---------------------------

  public List<BeanFactoryPostProcessor> getFactoryPostProcessors() {
    ArrayList<BeanFactoryPostProcessor> processors = this.factoryPostProcessors;
    if (processors == null) {
      return this.factoryPostProcessors = new ArrayList<>();
    }
    return processors;
  }

  // since 4.0
  public void addFactoryPostProcessors(BeanFactoryPostProcessor... postProcessors) {
    CollectionUtils.addAll(getFactoryPostProcessors(), postProcessors);
  }

  //---------------------------------------------------------------------
  // Implementation of ApplicationEventPublisher interface
  //---------------------------------------------------------------------

  @Override
  public void publishEvent(Object event) {
    getEventPublisher().publishEvent(event);
  }

  public final BeanFactoryAwareBeanInstantiator getBeanInstantiator() {
    if (beanInstantiator == null) {
      beanInstantiator = new BeanFactoryAwareBeanInstantiator(getBeanFactory());
    }
    return beanInstantiator;
  }

  protected void registerApplicationListeners() {
    log.info("Loading Application Listeners.");
    ConfigurableBeanFactory beanFactory = getBeanFactory();

    Set<String> beanNamesOfType = beanFactory.getBeanNamesForType(
            ApplicationListener.class, true, true);

    for (String beanName : beanNamesOfType) {
      addApplicationListener(beanName);
    }

    Set<String> beanNames = beanFactory.getBeanNamesForAnnotation(EventListener.class);
    for (String beanName : beanNames) {
      if (!beanNamesOfType.contains(beanName)) {
        addApplicationListener(beanName);
      }
    }

    // fixed #9 Some listener in a jar can't be load
    log.info("Loading META-INF/listeners");

    // Load the META-INF/listeners
    // ---------------------------------------------------
    Set<Class<?>> listeners = ContextUtils.loadFromMetaInfo(Constant.META_INFO_listeners);
    BeanFactoryAwareBeanInstantiator instantiator = getBeanInstantiator();
    for (Class<?> listener : listeners) {
      ApplicationListener applicationListener = (ApplicationListener) instantiator.instantiate(listener);
      addApplicationListener(applicationListener);
    }

    // load from strategy files
    TodayStrategies detector = TodayStrategies.getDetector();
    log.info("Loading listeners from strategies files: {}", detector.getStrategiesLocation());
    for (ApplicationListener listener : detector.getStrategies(ApplicationListener.class, this)) {
      addApplicationListener(listener);
    }

  }

  @Override
  public void addApplicationListener(ApplicationListener<?> listener) {
    getEventPublisher().addApplicationListener(listener);
  }

  @Override
  public void addApplicationListener(String listenerBeanName) {
    getEventPublisher().addApplicationListener(listenerBeanName);
  }

  @Override
  public void removeApplicationListener(String listenerBeanName) {
    getEventPublisher().removeApplicationListener(listenerBeanName);

  }

  @Override
  public void removeApplicationListener(ApplicationListener<?> listener) {
    getEventPublisher().removeApplicationListener(listener);
  }

  @Override
  public void removeAllListeners() {
    getEventPublisher().removeAllListeners();
  }

  @Override
  public Collection<ApplicationListener<?>> getApplicationListeners() {
    return getEventPublisher().getApplicationListeners();
  }

  /** @since 4.0 */
  @Override
  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    Assert.notNull(eventPublisher, "event-publisher must not be nul");
    this.eventPublisher = eventPublisher;
  }

  /** @since 4.0 */
  public ApplicationEventPublisher getEventPublisher() {
    if (eventPublisher == null) {
      eventPublisher = new DefaultApplicationEventPublisher(getBeanFactory());
    }
    return eventPublisher;
  }

  // Object

  /**
   * Return information about this context.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getApplicationName());
    sb.append(": state: [")
            .append(state)
            .append("], on startup date: ")
            .append(formatStartupDate());
    ApplicationContext parent = getParent();
    if (parent != null) {
      sb.append(", parent: ").append(parent.getApplicationName());
    }
    return sb.toString();
  }

  public String formatStartupDate() {
    return new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(startupDate);
  }

}
