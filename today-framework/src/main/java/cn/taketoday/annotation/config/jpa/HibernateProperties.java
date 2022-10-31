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

package cn.taketoday.annotation.config.jpa;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.lang.Assert;
import cn.taketoday.orm.hibernate5.support.HibernateImplicitNamingStrategy;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Configuration properties for Hibernate.
 *
 * @author Stephane Nicoll
 * @author Chris Bono
 * @see JpaProperties
 * @since 4.0
 */
@ConfigurationProperties("jpa.hibernate")
public class HibernateProperties {

  private static final String DISABLED_SCANNER_CLASS = "org.hibernate.boot.archive.scan.internal.DisabledScanner";

  private final Naming naming = new Naming();

  /**
   * DDL mode. This is actually a shortcut for the "hibernate.hbm2ddl.auto" property.
   * Defaults to "create-drop" when using an embedded database and no schema manager was
   * detected. Otherwise, defaults to "none".
   */
  private String ddlAuto;

  public String getDdlAuto() {
    return this.ddlAuto;
  }

  public void setDdlAuto(String ddlAuto) {
    this.ddlAuto = ddlAuto;
  }

  public Naming getNaming() {
    return this.naming;
  }

  /**
   * Determine the configuration properties for the initialization of the main Hibernate
   * EntityManagerFactory based on standard JPA properties and
   * {@link HibernateSettings}.
   *
   * @param jpaProperties standard JPA properties
   * @param settings the settings to apply when determining the configuration properties
   * @return the Hibernate properties to use
   */
  public Map<String, Object> determineHibernateProperties(Map<String, String> jpaProperties,
          HibernateSettings settings) {
    Assert.notNull(jpaProperties, "JpaProperties must not be null");
    Assert.notNull(settings, "Settings must not be null");
    return getAdditionalProperties(jpaProperties, settings);
  }

  private Map<String, Object> getAdditionalProperties(Map<String, String> existing, HibernateSettings settings) {
    Map<String, Object> result = new HashMap<>(existing);
    applyScanner(result);
    getNaming().applyNamingStrategies(result);
    String ddlAuto = determineDdlAuto(existing, settings::getDdlAuto);
    if (StringUtils.hasText(ddlAuto) && !"none".equals(ddlAuto)) {
      result.put(AvailableSettings.HBM2DDL_AUTO, ddlAuto);
    }
    else {
      result.remove(AvailableSettings.HBM2DDL_AUTO);
    }
    Collection<HibernatePropertiesCustomizer> customizers = settings.getHibernatePropertiesCustomizers();
    if (!ObjectUtils.isEmpty(customizers)) {
      customizers.forEach((customizer) -> customizer.customize(result));
    }
    return result;
  }

  private void applyScanner(Map<String, Object> result) {
    if (!result.containsKey(AvailableSettings.SCANNER) && ClassUtils.isPresent(DISABLED_SCANNER_CLASS, null)) {
      result.put(AvailableSettings.SCANNER, DISABLED_SCANNER_CLASS);
    }
  }

  private String determineDdlAuto(Map<String, String> existing, Supplier<String> defaultDdlAuto) {
    String ddlAuto = existing.get(AvailableSettings.HBM2DDL_AUTO);
    if (ddlAuto != null) {
      return ddlAuto;
    }
    if (this.ddlAuto != null) {
      return this.ddlAuto;
    }
    if (existing.get(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION) != null) {
      return null;
    }
    return defaultDdlAuto.get();
  }

  public static class Naming {

    /**
     * Fully qualified name of the implicit naming strategy.
     */
    private String implicitStrategy;

    /**
     * Fully qualified name of the physical naming strategy.
     */
    private String physicalStrategy;

    public String getImplicitStrategy() {
      return this.implicitStrategy;
    }

    public void setImplicitStrategy(String implicitStrategy) {
      this.implicitStrategy = implicitStrategy;
    }

    public String getPhysicalStrategy() {
      return this.physicalStrategy;
    }

    public void setPhysicalStrategy(String physicalStrategy) {
      this.physicalStrategy = physicalStrategy;
    }

    private void applyNamingStrategies(Map<String, Object> properties) {
      applyNamingStrategy(properties, AvailableSettings.IMPLICIT_NAMING_STRATEGY, this.implicitStrategy,
              HibernateImplicitNamingStrategy.class::getName);
      applyNamingStrategy(properties, AvailableSettings.PHYSICAL_NAMING_STRATEGY, this.physicalStrategy,
              CamelCaseToUnderscoresNamingStrategy.class::getName);
    }

    private void applyNamingStrategy(Map<String, Object> properties, String key, Object strategy,
            Supplier<String> defaultStrategy) {
      if (strategy != null) {
        properties.put(key, strategy);
      }
      else {
        properties.computeIfAbsent(key, (k) -> defaultStrategy.get());
      }
    }

  }

}