/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.context.loader;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Objects;

import org.slf4j.LoggerFactory;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.DefaultBeanDefinition;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * Default Bean Definition Loader implements
 * 
 * @author TODAY <br>
 *         2018-06-23 11:18:22
 */
public class DefaultBeanDefinitionLoader implements BeanDefinitionLoader {

    /** bean definition registry */
    private final BeanDefinitionRegistry registry;
    /** bean name creator */
    private final BeanNameCreator beanNameCreator;
    private final ConfigurableApplicationContext applicationContext;

    public DefaultBeanDefinitionLoader(ConfigurableApplicationContext applicationContext) {

        this.applicationContext = //
                Objects.requireNonNull(applicationContext, "applicationContext can't be null");

        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        this.registry = environment.getBeanDefinitionRegistry();
        this.beanNameCreator = environment.getBeanNameCreator();
    }

    @Override
    public BeanDefinitionRegistry getRegistry() {
        return this.registry;
    }

    @Override
    public void loadBeanDefinition(Class<?> beanClass) throws BeanDefinitionStoreException {

        // don't load abstract class
        if (!Modifier.isAbstract(beanClass.getModifiers()) && ContextUtils.conditional(beanClass)) {
            register(beanClass);
        }
    }

    @Override
    public void loadBeanDefinitions(Collection<Class<?>> beans) throws BeanDefinitionStoreException {
        for (Class<?> clazz : beans) {
            loadBeanDefinition(clazz);
        }
    }

    @Override
    public void loadBeanDefinition(String name, Class<?> beanClass) throws BeanDefinitionStoreException {

        // register

        final AnnotationAttributes[] annotationAttributes = //
                ClassUtils.getAnnotationAttributesArray(beanClass, Component.class);

        if (ObjectUtils.isEmpty(annotationAttributes)) {
            register(name, build(beanClass, null, name));
        }
        else {
            for (final AnnotationAttributes attributes : annotationAttributes) {
                register(name, build(beanClass, attributes, name));
            }
        }
    }

    /**
     * Register with given class
     * 
     * @param beanClass
     *            Bean class
     * @throws BeanDefinitionStoreException
     *             If {@link BeanDefinition} can't store
     */
    @Override
    public void register(Class<?> beanClass) throws BeanDefinitionStoreException {

        final AnnotationAttributes[] annotationAttributes = //
                ClassUtils.getAnnotationAttributesArray(beanClass, Component.class);

        if (ObjectUtils.isNotEmpty(annotationAttributes)) {

            final String defaultBeanName = beanNameCreator.create(beanClass);
            for (final AnnotationAttributes attributes : annotationAttributes) {
                for (final String name : ContextUtils.findNames(defaultBeanName, attributes.getStringArray(Constant.VALUE))) {
                    register(name, build(beanClass, attributes, name));
                }
            }
        }
    }

    /**
     * Build a bean definition
     * 
     * @param beanClass
     *            Given bean class
     * @param attributes
     *            {@link AnnotationAttributes}
     * @param beanName
     *            Bean name
     * @return A default {@link BeanDefinition}
     * @throws Throwable
     *             If any {@link Exception} occurred
     */
    protected BeanDefinition build(Class<?> beanClass, AnnotationAttributes attributes, String beanName) {
        return ContextUtils.buildBeanDefinition(beanClass, attributes, beanName);
    }

    /**
     * Register bean definition with given name
     * 
     * @param name
     *            Bean name
     * @param beanDefinition
     *            Bean definition
     * @throws BeanDefinitionStoreException
     *             If can't store bean
     */
    @Override
    public void register(final String name, final BeanDefinition beanDefinition) throws BeanDefinitionStoreException {

        ContextUtils.validateBeanDefinition(beanDefinition);

        try {

            final Class<?> beanClass = beanDefinition.getBeanClass();

            if (applicationContext.containsBeanDefinition(name)) {
                final BeanDefinition existBeanDefinition = applicationContext.getBeanDefinition(name);
                if (beanClass.equals(existBeanDefinition.getBeanClass())) {
                    // TODO 处理该情况
                    LoggerFactory.getLogger(DefaultBeanDefinitionLoader.class)//
                            .warn("There is already a bean called: [{}], its bean class: [{}]", //
                                  name, beanClass);
                }
            }

            if (FactoryBean.class.isAssignableFrom(beanClass)) { // process FactoryBean
                registerFactoryBean(name, beanDefinition);
            }
            else {
                registry.registerBeanDefinition(name, beanDefinition);
            }
        }
        catch (Throwable ex) {

            ex = ExceptionUtils.unwrapThrowable(ex);
            throw new BeanDefinitionStoreException("An Exception Occurred When Register Bean Definition: [" + //
                    name + "], With Msg: [" + ex + "]", ex);
        }
    }

    /**
     * If bean definition is a {@link FactoryBean} register its factory's instance
     * 
     * @param beanName
     *            Old bean name
     * @param beanDefinition
     *            Bean definition
     * @return returns a new bean name
     * @throws Throwable
     *             If any {@link Exception} occurred
     */
    protected void registerFactoryBean(final String oldBeanName, final BeanDefinition beanDefinition) throws Throwable {

        FactoryBean<?> $factoryBean = //
                (FactoryBean<?>) applicationContext.getSingleton(BeanFactory.FACTORY_BEAN_PREFIX + oldBeanName);

        boolean register = false;
        if ($factoryBean == null) { // If not exist declaring instance, create it
            // declaring object not registed
            $factoryBean = (FactoryBean<?>) ClassUtils.newInstance(beanDefinition.getBeanClass());
            register = true;
        }

        // build a new name
        String beanName = $factoryBean.getBeanName(); // use new name
        if (StringUtils.isEmpty(beanName)) {
            // Fix FactoryBean name problem
            final AnnotationAttributes attr = //
                    ClassUtils.getAnnotationAttributes(Component.class, beanDefinition.getBeanClass());
            if (attr != null) {
                beanName = ContextUtils.findNames(oldBeanName, attr.getStringArray(Constant.VALUE))[0];
                if (oldBeanName.equals(beanName)) {
                    beanName = beanNameCreator.create($factoryBean.getBeanClass());
                }
            }
            else {
                beanName = oldBeanName; // use old name, that the name from Annotation or class default name
            }
        }
        else {
            register = true;
        }

        if (register) {// register it
            applicationContext.registerSingleton(BeanFactory.FACTORY_BEAN_PREFIX + beanName, $factoryBean);
        }

        final DefaultBeanDefinition def = new DefaultBeanDefinition(beanName, $factoryBean.getBeanClass());

        def.setFactoryBean(true)//
                .setScope(beanDefinition.getScope())//
                .setInitMethods(beanDefinition.getInitMethods())//
                .setDestroyMethods(beanDefinition.getDestroyMethods())//
                .setPropertyValues(beanDefinition.getPropertyValues());

        registry.registerBeanDefinition(beanName, def);

    }

    @Override
    public BeanDefinition createBeanDefinition(Class<?> beanClass) {
        return build(beanClass, //
                     ClassUtils.getAnnotationAttributes(Component.class, beanClass), //
                     beanNameCreator.create(beanClass)//
        );
    }

}
