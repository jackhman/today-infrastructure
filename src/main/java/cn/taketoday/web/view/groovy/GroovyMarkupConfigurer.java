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

package cn.taketoday.web.view.groovy;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.MarkupTemplateEngine.TemplateResource;
import groovy.text.markup.TemplateConfiguration;
import groovy.text.markup.TemplateResolver;

/**
 * An extension of Groovy's {@link groovy.text.markup.TemplateConfiguration} and
 * an implementation of Framework MVC's {@link GroovyMarkupConfig} for creating
 * a {@code MarkupTemplateEngine} for use in a web application. The most basic
 * way to configure this class is to set the "resourceLoaderPath". For example:
 *
 * <pre class="code">
 *
 * // Add the following to an &#64;Configuration class
 *
 * &#64;Bean
 * public GroovyMarkupConfig groovyMarkupConfigurer() {
 *     GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
 *     configurer.setResourceLoaderPath("classpath:/WEB-INF/groovymarkup/");
 *     return configurer;
 * }
 * </pre>
 *
 * By default this bean will create a {@link MarkupTemplateEngine} with:
 * <ul>
 * <li>a parent ClassLoader for loading Groovy templates with their references
 * <li>the default configuration in the base class {@link TemplateConfiguration}
 * <li>a {@link groovy.text.markup.TemplateResolver} for resolving template files
 * </ul>
 *
 * You can provide the {@link MarkupTemplateEngine} instance directly to this bean
 * in which case all other properties will not be effectively ignored.
 *
 * <p>This bean must be included in the application context of any application
 * using the Framework MVC {@link GroovyMarkupView} for rendering. It exists purely
 * for the purpose of configuring Groovy's Markup templates. It is not meant to be
 * referenced by application components directly. It implements GroovyMarkupConfig
 * to be found by GroovyMarkupView without depending on a bean name. Each
 * DispatcherServlet can define its own GroovyMarkupConfigurer if desired.
 *
 * <p>Note that resource caching is enabled by default in {@link MarkupTemplateEngine}.
 * Use the {@link #setCacheTemplates(boolean)} to configure that as necessary.
 *
 * <p>Framework's Groovy Markup template support requires Groovy 2.3.1 or higher.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see GroovyMarkupView
 * @see <a href="http://groovy-lang.org/templating.html#_the_markuptemplateengine">
 * Groovy Markup Template engine documentation</a>
 * @since 4.0
 */
public class GroovyMarkupConfigurer extends TemplateConfiguration
        implements GroovyMarkupConfig, ApplicationContextAware, InitializingBean {

  private String resourceLoaderPath = "classpath:";

  @Nullable
  private MarkupTemplateEngine templateEngine;

  @Nullable
  private ApplicationContext applicationContext;

  /**
   * Set the Groovy Markup Template resource loader path(s) via a Framework resource
   * location. Accepts multiple locations as a comma-separated list of paths.
   * Standard URLs like "file:" and "classpath:" and pseudo URLs are supported
   * as understood by Framework's {@link cn.taketoday.core.io.ResourceLoader}.
   * Relative paths are allowed when running in an ApplicationContext.
   */
  public void setResourceLoaderPath(String resourceLoaderPath) {
    this.resourceLoaderPath = resourceLoaderPath;
  }

  public String getResourceLoaderPath() {
    return this.resourceLoaderPath;
  }

  /**
   * Set a pre-configured MarkupTemplateEngine to use for the Groovy Markup
   * Template web configuration.
   * <p>Note that this engine instance has to be manually configured, since all
   * other bean properties of this configurer will be ignored.
   */
  public void setTemplateEngine(MarkupTemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  @Override
  public MarkupTemplateEngine getTemplateEngine() {
    Assert.state(this.templateEngine != null, "No MarkupTemplateEngine set");
    return this.templateEngine;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  protected ApplicationContext getApplicationContext() {
    Assert.state(this.applicationContext != null, "No ApplicationContext set");
    return this.applicationContext;
  }

  /**
   * This method should not be used, since the considered Locale for resolving
   * templates is the Locale for the current HTTP request.
   */
  @Override
  public void setLocale(Locale locale) {
    super.setLocale(locale);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.templateEngine == null) {
      this.templateEngine = createTemplateEngine();
    }
  }

  protected MarkupTemplateEngine createTemplateEngine() throws IOException {
    if (this.templateEngine == null) {
      ClassLoader templateClassLoader = createTemplateClassLoader();
      this.templateEngine = new MarkupTemplateEngine(templateClassLoader, this, new LocaleTemplateResolver());
    }
    return this.templateEngine;
  }

  /**
   * Create a parent ClassLoader for Groovy to use as parent ClassLoader
   * when loading and compiling templates.
   */
  protected ClassLoader createTemplateClassLoader() throws IOException {
    String[] paths = StringUtils.commaDelimitedListToStringArray(getResourceLoaderPath());
    ArrayList<URL> urls = new ArrayList<>();
    ApplicationContext context = getApplicationContext();
    for (String path : paths) {
      Set<Resource> resources = context.getResources(path);
      if (!resources.isEmpty()) {
        for (Resource resource : resources) {
          if (resource.exists()) {
            urls.add(resource.getLocation());
          }
        }
      }
    }
    ClassLoader classLoader = context.getClassLoader();
    Assert.state(classLoader != null, "No ClassLoader");
    return !urls.isEmpty()
           ? new URLClassLoader(urls.toArray(new URL[0]), classLoader)
           : classLoader;
  }

  /**
   * Resolve a template from the given template path.
   * <p>The default implementation uses the Locale associated with the current request,
   * as obtained through {@link cn.taketoday.core.i18n.LocaleContextHolder LocaleContextHolder},
   * to find the template file. Effectively the locale configured at the engine level is ignored.
   *
   * @see LocaleContextHolder
   * @see #setLocale
   */
  protected URL resolveTemplate(ClassLoader classLoader, String templatePath) throws IOException {
    TemplateResource resource = TemplateResource.parse(templatePath);
    Locale locale = LocaleContextHolder.getLocale();
    URL url = classLoader.getResource(resource.withLocale(StringUtils.replace(locale.toString(), "-", "_")).toString());
    if (url == null) {
      url = classLoader.getResource(resource.withLocale(locale.getLanguage()).toString());
    }
    if (url == null) {
      url = classLoader.getResource(resource.withLocale(null).toString());
    }
    if (url == null) {
      throw new IOException("Unable to load template:" + templatePath);
    }
    return url;
  }

  /**
   * Custom {@link TemplateResolver template resolver} that simply delegates to
   * {@link #resolveTemplate(ClassLoader, String)}..
   */
  private class LocaleTemplateResolver implements TemplateResolver {

    @Nullable
    private ClassLoader classLoader;

    @Override
    public void configure(ClassLoader templateClassLoader, TemplateConfiguration configuration) {
      this.classLoader = templateClassLoader;
    }

    @Override
    public URL resolveTemplate(String templatePath) throws IOException {
      Assert.state(this.classLoader != null, "No template ClassLoader available");
      return GroovyMarkupConfigurer.this.resolveTemplate(this.classLoader, templatePath);
    }
  }

}
