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

package cn.taketoday.framework.web.servlet.server;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.Locale;

import cn.taketoday.logging.Logger;

/**
 * Manages a {@link ServletWebServerFactory} document root.
 *
 * @author Phillip Webb
 * @see AbstractServletWebServerFactory
 */
class DocumentRoot {

  private static final String[] COMMON_DOC_ROOTS = { "src/main/webapp", "public", "static" };

  private final Logger logger;

  private File directory;

  DocumentRoot(Logger logger) {
    this.logger = logger;
  }

  File getDirectory() {
    return this.directory;
  }

  void setDirectory(File directory) {
    this.directory = directory;
  }

  /**
   * Returns the absolute document root when it points to a valid directory, logging a
   * warning and returning {@code null} otherwise.
   *
   * @return the valid document root
   */
  final File getValidDirectory() {
    File file = this.directory;
    file = (file != null) ? file : getWarFileDocumentRoot();
    file = (file != null) ? file : getExplodedWarFileDocumentRoot();
    file = (file != null) ? file : getCommonDocumentRoot();
    if (file == null && this.logger.isDebugEnabled()) {
      logNoDocumentRoots();
    }
    else if (this.logger.isDebugEnabled()) {
      this.logger.debug("Document root: " + file);
    }
    return file;
  }

  private File getWarFileDocumentRoot() {
    return getArchiveFileDocumentRoot(".war");
  }

  private File getArchiveFileDocumentRoot(String extension) {
    File file = getCodeSourceArchive();
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Code archive: " + file);
    }
    if (file != null && file.exists() && !file.isDirectory()
            && file.getName().toLowerCase(Locale.ENGLISH).endsWith(extension)) {
      return file.getAbsoluteFile();
    }
    return null;
  }

  private File getExplodedWarFileDocumentRoot() {
    return getExplodedWarFileDocumentRoot(getCodeSourceArchive());
  }

  private File getCodeSourceArchive() {
    return getCodeSourceArchive(getClass().getProtectionDomain().getCodeSource());
  }

  File getCodeSourceArchive(CodeSource codeSource) {
    try {
      URL location = (codeSource != null) ? codeSource.getLocation() : null;
      if (location == null) {
        return null;
      }
      String path;
      URLConnection connection = location.openConnection();
      if (connection instanceof JarURLConnection) {
        path = ((JarURLConnection) connection).getJarFile().getName();
      }
      else {
        path = location.toURI().getPath();
      }
      int index = path.indexOf("!/");
      if (index != -1) {
        path = path.substring(0, index);
      }
      return new File(path);
    }
    catch (Exception ex) {
      return null;
    }
  }

  final File getExplodedWarFileDocumentRoot(File codeSourceFile) {
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Code archive: " + codeSourceFile);
    }
    if (codeSourceFile != null && codeSourceFile.exists()) {
      String path = codeSourceFile.getAbsolutePath();
      int webInfPathIndex = path.indexOf(File.separatorChar + "WEB-INF" + File.separatorChar);
      if (webInfPathIndex >= 0) {
        path = path.substring(0, webInfPathIndex);
        return new File(path);
      }
    }
    return null;
  }

  private File getCommonDocumentRoot() {
    for (String commonDocRoot : COMMON_DOC_ROOTS) {
      File root = new File(commonDocRoot);
      if (root.exists() && root.isDirectory()) {
        return root.getAbsoluteFile();
      }
    }
    return null;
  }

  private void logNoDocumentRoots() {
    this.logger.debug("None of the document roots " + Arrays.asList(COMMON_DOC_ROOTS)
            + " point to a directory and will be ignored.");
  }

}
