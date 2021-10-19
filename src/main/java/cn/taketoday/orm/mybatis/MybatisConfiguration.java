/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.orm.mybatis;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.DefaultBeanDefinition;
import cn.taketoday.beans.factory.FactoryBeanDefinition;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.loader.BeanDefinitionCreationContext;
import cn.taketoday.context.loader.BeanDefinitionCreationStrategy;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.ClassMetaReader;
import cn.taketoday.core.bytecode.tree.ClassNode;
import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Env;
import cn.taketoday.lang.Repository;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2018-10-05 19:03
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MybatisConfiguration implements BeanDefinitionCreationStrategy {
  private final Logger log = LoggerFactory.getLogger(getClass());

  public static final String DEFAULT_CONFIG_LOCATION = "classpath:mybatis.xml";
  public static final Method[] initMethods =
          BeanDefinitionBuilder.computeInitMethod(null, MapperFactoryBean.class);

  @Override
  public Set<BeanDefinition> loadBeanDefinitions(
          ClassNode classNode, BeanDefinitionCreationContext creationContext) {
    log.info("Loading Mybatis Mapper Bean Definitions");

    if (!Modifier.isInterface(classNode.access)) {
      return null;
    }

    // must be an interface

    AnnotationAttributes attributes = ClassMetaReader.selectAttributes(classNode, Repository.class);
    if (attributes != null) {
      String className = classNode.name;
      log.debug("Found Mapper: [{}]", className);
      String[] names = attributes.getStringArray(Constant.VALUE);
      String name = ObjectUtils.isNotEmpty(names)
                    ? names[0] : creationContext.createBeanName(className);

      return Collections.singleton(createBeanDefinition(className, name));
    }
    return null;
  }

  protected FactoryBeanDefinition<?> createBeanDefinition(String className, String name) {
    DefaultBeanDefinition ret = new DefaultBeanDefinition(name, className);
    ret.setSynthetic(true);
    ret.setInitMethods(initMethods);
    ret.setDestroyMethods(Constant.EMPTY_STRING_ARRAY);
    ret.setRole(DefaultBeanDefinition.ROLE_INFRASTRUCTURE);
    return new FactoryBeanDefinition<>(ret, new MapperFactoryBean<>(className));
  }

  @MissingBean
  public SqlSession sqlSession(
          @Autowired DataSource dataSource,
          @Env("mybatis.env") String envId,
          @Env("mybatis.config") String configLocation,
          @Autowired(required = false) TransactionFactory transactionFactory,
          @Props(prefix = "mybatis.", replace = true) Properties properties) throws IOException //
  {

    if (StringUtils.isEmpty(envId)) {
      envId = "TODAY-MYBATIS";
    }

    if (StringUtils.isEmpty(configLocation)) {
      configLocation = DEFAULT_CONFIG_LOCATION;
    }

    Configuration configuration = new XMLConfigBuilder(
            ResourceUtils.getResourceAsStream(configLocation), envId, properties).parse();

    if (transactionFactory == null) {
      transactionFactory = new MybatisTransactionFactory();
    }
    configuration.setEnvironment(new Environment(envId, transactionFactory, dataSource));
    return new SessionTemplate(new DefaultSqlSessionFactory(configuration));
  }

}
