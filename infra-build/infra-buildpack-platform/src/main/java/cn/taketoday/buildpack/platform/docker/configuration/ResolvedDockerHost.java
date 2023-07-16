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

package cn.taketoday.buildpack.platform.docker.configuration;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.sun.jna.Platform;

import cn.taketoday.buildpack.platform.system.Environment;

/**
 * Resolves a {@link DockerHost} from the environment, configuration, or using defaults.
 *
 * @author Scott Frederick
 * @since 4.0
 */
public class ResolvedDockerHost extends DockerHost {

  private static final String UNIX_SOCKET_PREFIX = "unix://";

  private static final String DOMAIN_SOCKET_PATH = "/var/run/docker.sock";

  private static final String WINDOWS_NAMED_PIPE_PATH = "//./pipe/docker_engine";

  private static final String DOCKER_HOST = "DOCKER_HOST";

  private static final String DOCKER_TLS_VERIFY = "DOCKER_TLS_VERIFY";

  private static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";

  ResolvedDockerHost(String address, boolean secure, String certificatePath) {
    super(address, secure, certificatePath);
  }

  @Override
  public String getAddress() {
    return super.getAddress().startsWith(UNIX_SOCKET_PREFIX)
           ? super.getAddress().substring(UNIX_SOCKET_PREFIX.length()) : super.getAddress();
  }

  public boolean isRemote() {
    return getAddress().startsWith("http") || getAddress().startsWith("tcp");
  }

  public boolean isLocalFileReference() {
    try {
      return Files.exists(Paths.get(getAddress()));
    }
    catch (Exception ex) {
      return false;
    }
  }

  public static ResolvedDockerHost from(DockerHost dockerHost) {
    return from(Environment.SYSTEM, dockerHost);
  }

  static ResolvedDockerHost from(Environment environment, DockerHost dockerHost) {
    if (environment.get(DOCKER_HOST) != null) {
      return new ResolvedDockerHost(environment.get(DOCKER_HOST), isTrue(environment.get(DOCKER_TLS_VERIFY)),
              environment.get(DOCKER_CERT_PATH));
    }
    if (dockerHost != null && dockerHost.getAddress() != null) {
      return new ResolvedDockerHost(dockerHost.getAddress(), dockerHost.isSecure(),
              dockerHost.getCertificatePath());
    }
    return new ResolvedDockerHost(Platform.isWindows() ? WINDOWS_NAMED_PIPE_PATH : DOMAIN_SOCKET_PATH, false, null);
  }

  private static boolean isTrue(String value) {
    try {
      return (value != null) && (Integer.parseInt(value) == 1);
    }
    catch (NumberFormatException ex) {
      return false;
    }
  }

}
