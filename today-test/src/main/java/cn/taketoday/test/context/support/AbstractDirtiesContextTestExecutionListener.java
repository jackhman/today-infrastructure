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

package cn.taketoday.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Method;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.annotation.DirtiesContext.ClassMode;
import cn.taketoday.test.annotation.DirtiesContext.HierarchyMode;
import cn.taketoday.test.annotation.DirtiesContext.MethodMode;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestContextAnnotationUtils;

/**
 * Abstract base class for {@code TestExecutionListener} implementations that
 * provide support for marking the {@code ApplicationContext} associated with
 * a test as <em>dirty</em> for both test classes and test methods annotated
 * with the {@link DirtiesContext @DirtiesContext} annotation.
 *
 * <p>The core functionality for this class was extracted from
 * {@link DirtiesContextTestExecutionListener} in Spring Framework 4.2.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see DirtiesContext
 * @since 4.0
 */
public abstract class AbstractDirtiesContextTestExecutionListener extends AbstractTestExecutionListener {

  private static final Log logger = LogFactory.getLog(AbstractDirtiesContextTestExecutionListener.class);

  @Override
  public abstract int getOrder();

  /**
   * Mark the {@linkplain ApplicationContext application context} of the supplied
   * {@linkplain TestContext test context} as
   * {@linkplain TestContext#markApplicationContextDirty(DirtiesContext.HierarchyMode) dirty}
   * and set {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
   * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context to {@code true}.
   *
   * @param testContext the test context whose application context should
   * be marked as dirty
   * @param hierarchyMode the context cache clearing mode to be applied if the
   * context is part of a hierarchy; may be {@code null}
   * @since 4.0
   */
  protected void dirtyContext(TestContext testContext, @Nullable HierarchyMode hierarchyMode) {
    testContext.markApplicationContextDirty(hierarchyMode);
    testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE, Boolean.TRUE);
  }

  /**
   * Perform the actual work for {@link #beforeTestMethod} and {@link #afterTestMethod}
   * by dirtying the context if appropriate (i.e., according to the required modes).
   *
   * @param testContext the test context whose application context should
   * potentially be marked as dirty; never {@code null}
   * @param requiredMethodMode the method mode required for a context to
   * be marked dirty in the current phase; never {@code null}
   * @param requiredClassMode the class mode required for a context to
   * be marked dirty in the current phase; never {@code null}
   * @throws Exception allows any exception to propagate
   * @see #dirtyContext
   * @since 4.0
   */
  protected void beforeOrAfterTestMethod(TestContext testContext, MethodMode requiredMethodMode,
          ClassMode requiredClassMode) throws Exception {

    Assert.notNull(testContext, "TestContext must not be null");
    Assert.notNull(requiredMethodMode, "requiredMethodMode must not be null");
    Assert.notNull(requiredClassMode, "requiredClassMode must not be null");

    Class<?> testClass = testContext.getTestClass();
    Method testMethod = testContext.getTestMethod();
    Assert.notNull(testClass, "The test class of the supplied TestContext must not be null");
    Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

    DirtiesContext methodAnn = AnnotatedElementUtils.findMergedAnnotation(testMethod, DirtiesContext.class);
    DirtiesContext classAnn = TestContextAnnotationUtils.findMergedAnnotation(testClass, DirtiesContext.class);
    boolean methodAnnotated = (methodAnn != null);
    boolean classAnnotated = (classAnn != null);
    MethodMode methodMode = (methodAnnotated ? methodAnn.methodMode() : null);
    ClassMode classMode = (classAnnotated ? classAnn.classMode() : null);

    if (logger.isDebugEnabled()) {
      String phase = (requiredClassMode.name().startsWith("BEFORE") ? "Before" : "After");
      logger.debug(String.format("%s test method: context %s, class annotated with @DirtiesContext [%s] "
                      + "with mode [%s], method annotated with @DirtiesContext [%s] with mode [%s].", phase, testContext,
              classAnnotated, classMode, methodAnnotated, methodMode));
    }

    if ((methodMode == requiredMethodMode) || (classMode == requiredClassMode)) {
      HierarchyMode hierarchyMode = (methodAnnotated ? methodAnn.hierarchyMode() : classAnn.hierarchyMode());
      dirtyContext(testContext, hierarchyMode);
    }
  }

  /**
   * Perform the actual work for {@link #beforeTestClass} and {@link #afterTestClass}
   * by dirtying the context if appropriate (i.e., according to the required mode).
   *
   * @param testContext the test context whose application context should
   * potentially be marked as dirty; never {@code null}
   * @param requiredClassMode the class mode required for a context to
   * be marked dirty in the current phase; never {@code null}
   * @throws Exception allows any exception to propagate
   * @see #dirtyContext
   * @since 4.0
   */
  protected void beforeOrAfterTestClass(TestContext testContext, ClassMode requiredClassMode) throws Exception {
    Assert.notNull(testContext, "TestContext must not be null");
    Assert.notNull(requiredClassMode, "requiredClassMode must not be null");

    Class<?> testClass = testContext.getTestClass();
    Assert.notNull(testClass, "The test class of the supplied TestContext must not be null");

    DirtiesContext dirtiesContext = TestContextAnnotationUtils.findMergedAnnotation(testClass, DirtiesContext.class);
    boolean classAnnotated = (dirtiesContext != null);
    ClassMode classMode = (classAnnotated ? dirtiesContext.classMode() : null);

    if (logger.isDebugEnabled()) {
      String phase = (requiredClassMode.name().startsWith("BEFORE") ? "Before" : "After");
      logger.debug(String.format(
              "%s test class: context %s, class annotated with @DirtiesContext [%s] with mode [%s].", phase,
              testContext, classAnnotated, classMode));
    }

    if (classMode == requiredClassMode) {
      dirtyContext(testContext, dirtiesContext.hierarchyMode());
    }
  }

}