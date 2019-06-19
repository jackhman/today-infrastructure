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
package cn.taketoday.framework.bean;

import lombok.Getter;

/**
 * 
 * @author TODAY <br>
 *         2019-02-06 14:49
 */
@Getter
public class ErrorPage {

    private final int status;
    private final String path;
    private final Class<? extends Throwable> exception;

    public ErrorPage(String path) {
        this.status = 0;
        this.exception = null;
        this.path = path;
    }

    public ErrorPage(Integer status, String path) {
        this.status = status;
        this.exception = null;
        this.path = path;
    }

    public ErrorPage(Class<? extends Throwable> exception, String path) {
        this.status = 0;
        this.exception = exception;
        this.path = path;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof ErrorPage) {
            final ErrorPage err = ((ErrorPage) obj);
            if (err.exception != exception || (err.path != null && !err.path.equals(path)) || err.status != status) {
                return false;
            }
        }
        return true;
    }

}
