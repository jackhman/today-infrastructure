/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.extensions.compactnotation.CompactConstructor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.beans.BeanNameCreator;
import cn.taketoday.beans.DefaultBeanNameCreator;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.core.Assert;
import cn.taketoday.core.ConcurrentProperties;
import cn.taketoday.core.Constant;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceFilter;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.expression.ExpressionProcessor;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;

/**
 * Standard implementation of {@link Environment}
 *
 * @author TODAY <br>
 * 2018-11-14 21:23
 */
public class StandardEnvironment implements ConfigurableEnvironment {
  private static final Logger log = LoggerFactory.getLogger(StandardEnvironment.class);
  static boolean snakeyamlIsPresent = ClassUtils.isPresent("org.yaml.snakeyaml.Yaml");

  private final HashSet<String> activeProfiles = new HashSet<>(4);
  private final ConcurrentProperties properties = new ConcurrentProperties();
  private BeanNameCreator beanNameCreator;

  /** resolve beanDefinition which It is marked annotation */
  private BeanDefinitionLoader beanDefinitionLoader;
  /** storage BeanDefinition */
  private BeanDefinitionRegistry beanDefinitionRegistry;

  private String propertiesLocation = Constant.BLANK; // default ""

  private ExpressionProcessor expressionProcessor;

  public StandardEnvironment() {
    if (System.getSecurityManager() != null) {
      AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
        properties.putAll(System.getProperties());
        System.setProperties(properties);
        return null;
      });
    }
    else {
      properties.putAll(System.getProperties());
      System.setProperties(properties);
    }
  }

  @Override
  public Properties getProperties() {
    return properties;
  }

  @Override
  public boolean containsProperty(String key) {
    return properties.containsKey(key);
  }

  @Override
  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }

  @Override
  public BeanDefinitionRegistry getBeanDefinitionRegistry() {
    return beanDefinitionRegistry;
  }

  @Override
  public BeanDefinitionLoader getBeanDefinitionLoader() {
    return beanDefinitionLoader;
  }

  @Override
  public String[] getActiveProfiles() {
    return StringUtils.toStringArray(activeProfiles);
  }

  // ---ConfigurableEnvironment

  @Override
  public void setProperty(String key, String value) {
    properties.setProperty(key, value);
  }

  @Override
  public void setActiveProfiles(String... profiles) {
    activeProfiles.clear();
    Collections.addAll(activeProfiles, profiles);
    log.info("Set new active profiles: {}", activeProfiles);
  }

  @Override
  @Deprecated
  public void addActiveProfile(String profile) {
    log.info("Add active profile: {}", profile);
    activeProfiles.add(profile);
  }

  @Override
  public void addActiveProfile(String... profiles) {
    log.info("Add active profile: {}", Arrays.toString(profiles));
    Collections.addAll(activeProfiles, profiles);
  }

  /**
   * Load properties from {@link Resource}
   *
   * @param propertiesResource
   *         {@link Resource}
   *
   * @throws IOException
   *         When access to the resource if any {@link IOException} occurred
   */
  protected void loadProperties(final Resource propertiesResource) throws IOException {
    if (isYamlProperties(propertiesResource.getName())) {
      if (snakeyamlIsPresent) {
        // load yaml files
        loadFromYmal(getProperties(), propertiesResource);
      }
      else {
        log.warn("'org.yaml.snakeyaml.Yaml' does not exist in your classpath, yaml config file will be ignored");
      }
    }
    else {
      // load properties files
      if (!propertiesResource.exists()) {
        log.warn("The resource: [{}] you provided that doesn't exist", propertiesResource);
        return;
      }
      if (propertiesResource.isDirectory()) {
        log.debug("Start scanning properties resource.");
        final ResourceFilter propertiesFileFilter = (final Resource file) -> {
          if (file.isDirectory()) {
            return true;
          }
          final String name = file.getName();
          return name.endsWith(Constant.PROPERTIES_SUFFIX) && !name.startsWith("pom"); // pom.properties
        };
        doLoadFromDirectory(propertiesResource, this.properties, propertiesFileFilter);
      }
      else {
        doLoad(this.properties, propertiesResource);
      }
    }
  }

  /**
   * Is yaml?
   *
   * @param propertiesLocation
   *         location
   */
  private boolean isYamlProperties(String propertiesLocation) {
    return propertiesLocation.endsWith(".yaml") || propertiesLocation.endsWith(".yml");
  }

  private void loadFromYmal(final Properties properties, final Resource yamlResource) throws IOException {
    log.info("Found Yaml Properties Resource: [{}]", yamlResource.getLocation());
    SnakeyamlDelegate.doMapping(properties, yamlResource);
  }

  @Override
  public void loadProperties(String propertiesLocation) throws IOException {
    Assert.notNull(propertiesLocation, "Properties dir can't be null");
    Resource resource = ResourceUtils.getResource(propertiesLocation);
    loadProperties(resource);
  }

  /**
   * Load properties file with given path
   */
  @Override
  public void loadProperties() throws IOException {
    // load default properties source : application.yaml or application.properties
    LinkedHashSet<String> locations = new LinkedHashSet<>(8); // loaded locations
    loadDefaultResources(locations);

    if (locations.isEmpty()) {
      // scan class path properties files
      for (final String propertiesLocation : StringUtils.splitAsList(propertiesLocation)) {
        loadProperties(propertiesLocation);
      }
    }
    // load other files
    postLoadingProperties(locations);

    // refresh active profiles
    refreshActiveProfiles();
    // load
    replaceProperties(locations);
  }

  /**
   * subclasses load other files
   *
   * @param locations
   *         loaded file locations
   *
   * @throws IOException
   *         if any io exception occurred when loading properties files
   */
  protected void postLoadingProperties(Set<String> locations) throws IOException { }

  /**
   * load default properties files
   *
   * @param locations
   *         loaded files
   *
   * @throws IOException
   *         If load error
   */
  protected void loadDefaultResources(final Set<String> locations) throws IOException {
    final String[] defaultLocations = new String[] {
            DEFAULT_YML_FILE,
            DEFAULT_YAML_FILE,
            DEFAULT_PROPERTIES_FILE
    };

    for (final String location : defaultLocations) {
      final Resource propertiesResource = ResourceUtils.getResource(location);
      if (propertiesResource.exists()) {
        loadProperties(propertiesResource); // loading
        setPropertiesLocation(location);// can override
        locations.add(location);
      }
    }
  }

  /**
   * Replace the properties from current active profiles
   *
   * @param locations
   *         loaded properties locations
   *
   * @throws IOException
   *         When access to the resource if any {@link IOException} occurred
   */
  protected void replaceProperties(Set<String> locations) throws IOException {
    // replace
    final String[] activeProfiles = getActiveProfiles();
    for (final String profile : activeProfiles) {

      for (final String location : locations) {
        final StringBuilder builder = new StringBuilder(location);
        builder.insert(builder.indexOf("."), '-' + profile);

        try {
          loadProperties(builder.toString());
        }
        catch (FileNotFoundException ignored) { }
      }
    }
  }

  /**
   * Set active profiles from properties
   */
  protected void refreshActiveProfiles() {
    final String profiles = getProperty(Constant.KEY_ACTIVE_PROFILES);

    if (StringUtils.isNotEmpty(profiles)) {
      activeProfiles.addAll(StringUtils.splitAsList(profiles));
    }
  }

  /**
   * Do load
   *
   * @param directory
   *         base dir
   * @param properties
   *         properties
   *
   * @throws IOException
   *         if the resource is not available
   */
  public static void doLoadFromDirectory(final Resource directory,
                                         final Properties properties,
                                         final ResourceFilter propertiesFileFilter) throws IOException //
  {
    final Resource[] listResources = directory.list(propertiesFileFilter);
    for (final Resource resource : listResources) {
      if (resource.isDirectory()) { // recursive
        doLoadFromDirectory(resource, properties, propertiesFileFilter);
        continue;
      }
      doLoad(properties, resource);
    }
  }

  /**
   * @param properties
   *         Target properties to store
   * @param resource
   *         Resource to load
   *
   * @throws IOException
   *         if the resource is not available
   */
  public static void doLoad(Properties properties, final Resource resource) throws IOException {
    if (log.isInfoEnabled()) {
      log.info("Found Properties Resource: [{}]", resource.getLocation());
    }

    try (final InputStream inputStream = resource.getInputStream()) {
      properties.load(inputStream);
    }
  }

  @Override
  public ConfigurableEnvironment setBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) {
    this.beanDefinitionRegistry = beanDefinitionRegistry;
    return this;
  }

  @Override
  public ConfigurableEnvironment setBeanDefinitionLoader(BeanDefinitionLoader beanDefinitionLoader) {
    this.beanDefinitionLoader = beanDefinitionLoader;
    return this;
  }

  @Override
  public boolean acceptsProfiles(String... profiles) {

    final Set<String> activeProfiles = this.activeProfiles;
    for (final String profile : Objects.requireNonNull(profiles)) {
      if (StringUtils.isNotEmpty(profile) && profile.charAt(0) == '!') {
        if (!activeProfiles.contains(profile.substring(1))) {
          return true;
        }
      }
      else if (activeProfiles.contains(profile)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public ConfigurableEnvironment setBeanNameCreator(BeanNameCreator beanNameCreator) {
    this.beanNameCreator = beanNameCreator;
    return this;
  }

  /**
   * Get a bean name creator
   *
   * @return {@link BeanNameCreator}
   */
  @Override
  public BeanNameCreator getBeanNameCreator() {
    final BeanNameCreator ret = this.beanNameCreator;
    if (ret == null) {
      return this.beanNameCreator = createBeanNameCreator();
    }
    return ret;
  }

  /**
   * create {@link BeanNameCreator}
   *
   * @return a default {@link BeanNameCreator}
   */
  protected BeanNameCreator createBeanNameCreator() {
    return new DefaultBeanNameCreator(this);
  }

  @Override
  public ExpressionProcessor getExpressionProcessor() {
    return expressionProcessor;
  }

  @Override
  public ConfigurableEnvironment setExpressionProcessor(ExpressionProcessor expressionProcessor) {
    this.expressionProcessor = expressionProcessor;
    return this;
  }

  @Override
  public ConfigurableEnvironment setPropertiesLocation(String propertiesLocation) {
    this.propertiesLocation = propertiesLocation;
    return this;
  }

  public String getPropertiesLocation() {
    return propertiesLocation;
  }

  static class SnakeyamlDelegate {

    protected static void doMapping(final Properties properties, Resource yamlResource) throws IOException {
      final Map<String, Object> base = new Yaml(new CompactConstructor()).load(yamlResource.getInputStream());
      SnakeyamlDelegate.doMapping(properties, base, null);
    }

    @SuppressWarnings("unchecked")
    protected static void doMapping(final Properties properties, final Map<String, Object> base, final String prefix) {
      for (final Map.Entry<String, Object> entry : base.entrySet()) {
        String key = entry.getKey();
        final Object value = entry.getValue();
        key = prefix == null ? key : (prefix + '.' + key);
        if (value instanceof Map) {
          doMapping(properties, (Map<String, Object>) value, key);
        }
        else {
          properties.put(key, value);
        }
      }
    }
  }

}
