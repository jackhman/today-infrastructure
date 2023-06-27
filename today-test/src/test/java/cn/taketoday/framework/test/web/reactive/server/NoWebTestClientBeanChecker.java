/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.web.reactive.server;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.context.annotation.ImportSelector;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ImportSelector} to check no {@link WebTestClient} definition is registered when
 * config classes are processed.
 */
class NoWebTestClientBeanChecker implements ImportSelector, BeanFactoryAware {

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    assertThat(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, WebTestClient.class))
            .isEmpty();
  }

  @Override
  public String[] selectImports(AnnotationMetadata importingClassMetadata) {
    return new String[0];
  }

}
