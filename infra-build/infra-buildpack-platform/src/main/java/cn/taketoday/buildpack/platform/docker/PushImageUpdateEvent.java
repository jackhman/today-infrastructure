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

package cn.taketoday.buildpack.platform.docker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A {@link ProgressUpdateEvent} fired as an image is pushed to a registry.
 *
 * @author Scott Frederick
 * @since 4.0
 */
public class PushImageUpdateEvent extends ImageProgressUpdateEvent {

  private final ErrorDetail errorDetail;

  @JsonCreator
  public PushImageUpdateEvent(String id, String status, ProgressDetail progressDetail, String progress,
          ErrorDetail errorDetail) {
    super(id, status, progressDetail, progress);
    this.errorDetail = errorDetail;
  }

  /**
   * Returns the details of any error encountered during processing.
   *
   * @return the error
   */
  public ErrorDetail getErrorDetail() {
    return this.errorDetail;
  }

  /**
   * Details of an error embedded in a response stream.
   */
  public static class ErrorDetail {

    private final String message;

    @JsonCreator
    public ErrorDetail(@JsonProperty("message") String message) {
      this.message = message;
    }

    /**
     * Returns the message field from the error detail.
     *
     * @return the message
     */
    public String getMessage() {
      return this.message;
    }

    @Override
    public String toString() {
      return this.message;
    }

  }

}
