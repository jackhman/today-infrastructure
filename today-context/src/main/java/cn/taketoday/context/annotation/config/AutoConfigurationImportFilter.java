/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.annotation.config;

import cn.taketoday.beans.factory.Aware;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.context.BootstrapContextAware;
import cn.taketoday.context.EnvironmentAware;
import cn.taketoday.context.ResourceLoaderAware;

/**
 * Filter that can be registered in {@code today.strategies} to limit the
 * auto-configuration classes considered. This interface is designed to allow fast removal
 * of auto-configuration classes before their bytecode is even read.
 * <p>
 * An {@link AutoConfigurationImportFilter} may implement any of the following
 * {@link Aware Aware} interfaces, and their respective
 * methods will be called prior to {@link #match}:
 * <ul>
 * <li>{@link EnvironmentAware}</li>
 * <li>{@link BeanFactoryAware}</li>
 * <li>{@link BeanClassLoaderAware}</li>
 * <li>{@link ResourceLoaderAware}</li>
 * <li>{@link BootstrapContextAware}</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 02:41
 */
@FunctionalInterface
public interface AutoConfigurationImportFilter {

  /**
   * Apply the filter to the given auto-configuration class candidates.
   *
   * @param autoConfigurationClasses the auto-configuration classes being considered.
   * This array may contain {@code null} elements. Implementations should not change the
   * values in this array.
   * @param autoConfigurationMetadata access to the meta-data generated by the
   * auto-configure annotation processor
   * @return a boolean array indicating which of the auto-configuration classes should
   * be imported. The returned array must be the same size as the incoming
   * {@code autoConfigurationClasses} parameter. Entries containing {@code false} will
   * not be imported.
   */
  boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata);

}
