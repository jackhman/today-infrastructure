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

package cn.taketoday.beans.dependency;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.lang.Nullable;

import java.lang.reflect.Executable;
import java.util.Arrays;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/16 22:42</a>
 * @since 4.0
 */
public class DependencyResolvingContext {

  @Nullable
  private final Object[] providedArgs;

  private final Executable executable;

  @Nullable
  private final BeanFactory beanFactory;

  // dependency instance
  @Nullable
  private Object dependency;

  public DependencyResolvingContext(
          Executable executable, @Nullable BeanFactory beanFactory, @Nullable Object[] providedArgs) {
    this.providedArgs = providedArgs;
    this.executable = executable;
    this.beanFactory = beanFactory;
  }

  public Executable getExecutable() {
    return executable;
  }

  @Nullable
  public Object[] getProvidedArgs() {
    return providedArgs;
  }

  @Nullable
  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public boolean hasBeanFactory() {
    return beanFactory != null;
  }

  @Nullable
  public Object getDependency() {
    return dependency;
  }

  public void setDependency(@Nullable Object dependency) {
    this.dependency = dependency;
  }


  @Override
  public String toString() {
    return "DependencyResolvingContext{" +
            "providedArgs=" + Arrays.toString(providedArgs) +
            ", executable=" + executable +
            ", beanFactory=" + beanFactory +
            ", dependency=" + dependency +
            '}';
  }
}
