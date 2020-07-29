/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.aop.proxy;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.BeanPostProcessor;

/**
 * Auto create proxy
 *
 * @author TODAY <br>
 *         2018-11-10 13:13
 */
public class AutoProxyCreator extends OrderedSupport implements BeanPostProcessor {

    private final ApplicationContext context;

    @Autowired
    public AutoProxyCreator(ApplicationContext context) {
        super(Ordered.LOWEST_PRECEDENCE - Ordered.HIGHEST_PRECEDENCE);
        this.context = context;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, BeanDefinition beanDefinition) throws Exception {
        return new DefaultProxyFactory(new TargetSource(bean, bean.getClass()), context).getProxy();
    }

}
