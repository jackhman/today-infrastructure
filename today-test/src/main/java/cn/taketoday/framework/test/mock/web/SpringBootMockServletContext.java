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

package cn.taketoday.framework.test.mock.web;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

import cn.taketoday.core.io.FileSystemResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.mock.web.MockServletContext;

/**
 * {@link MockServletContext} implementation for Spring Boot. Respects well-known Spring
 * Boot resource locations and uses an empty directory for "/" if no locations can be
 * found.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class SpringBootMockServletContext extends MockServletContext {

  private static final String[] SPRING_BOOT_RESOURCE_LOCATIONS = new String[] { "classpath:META-INF/resources",
          "classpath:resources", "classpath:static", "classpath:public" };

  private final ResourceLoader resourceLoader;

  private File emptyRootDirectory;

  public SpringBootMockServletContext(String resourceBasePath) {
    this(resourceBasePath, new FileSystemResourceLoader());
  }

  public SpringBootMockServletContext(String resourceBasePath, ResourceLoader resourceLoader) {
    super(resourceBasePath, resourceLoader);
    this.resourceLoader = resourceLoader;
  }

  @Override
  protected String getResourceLocation(String path) {
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    String resourceLocation = getResourceBasePathLocation(path);
    if (exists(resourceLocation)) {
      return resourceLocation;
    }
    for (String prefix : SPRING_BOOT_RESOURCE_LOCATIONS) {
      resourceLocation = prefix + path;
      if (exists(resourceLocation)) {
        return resourceLocation;
      }
    }
    return super.getResourceLocation(path);
  }

  protected final String getResourceBasePathLocation(String path) {
    return super.getResourceLocation(path);
  }

  private boolean exists(String resourceLocation) {
    try {
      Resource resource = this.resourceLoader.getResource(resourceLocation);
      return resource.exists();
    }
    catch (Exception ex) {
      return false;
    }
  }

  @Override
  public URL getResource(String path) throws MalformedURLException {
    URL resource = super.getResource(path);
    if (resource == null && "/".equals(path)) {
      // Liquibase assumes that "/" always exists, if we don't have a directory
      // use a temporary location.
      try {
        if (this.emptyRootDirectory == null) {
          synchronized(this) {
            File tempDirectory = Files.createTempDirectory("spr-servlet").toFile();
            tempDirectory.deleteOnExit();
            this.emptyRootDirectory = tempDirectory;
          }
        }
        return this.emptyRootDirectory.toURI().toURL();
      }
      catch (IOException ex) {
        // Ignore
      }
    }
    return resource;
  }

}
