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

package cn.taketoday.test.context.hierarchies.web;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.web.context.WebApplicationContext;

import jakarta.servlet.ServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@ContextHierarchy(@ContextConfiguration)
public class DispatcherWacRootWacEarTests extends RootWacEarTests {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private String ear;

	@Autowired
	private String root;

	@Autowired
	private String dispatcher;


	@Disabled("Superseded by verifyDispatcherWacConfig()")
	@Test
	@Override
	void verifyEarConfig() {
		/* no-op */
	}

	@Disabled("Superseded by verifyDispatcherWacConfig()")
	@Test
	@Override
	void verifyRootWacConfig() {
		/* no-op */
	}

	@Test
	void verifyDispatcherWacConfig() {
		ApplicationContext parent = wac.getParent();
		assertThat(parent).isNotNull();
		boolean condition = parent instanceof WebApplicationContext;
		assertThat(condition).isTrue();

		ApplicationContext grandParent = parent.getParent();
		assertThat(grandParent).isNotNull();
		boolean condition1 = grandParent instanceof WebApplicationContext;
		assertThat(condition1).isFalse();

		ServletContext dispatcherServletContext = wac.getServletContext();
		assertThat(dispatcherServletContext).isNotNull();
		ServletContext rootServletContext = ((WebApplicationContext) parent).getServletContext();
		assertThat(rootServletContext).isNotNull();
		assertThat(rootServletContext).isSameAs(dispatcherServletContext);

		assertThat(rootServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(parent);
		assertThat(dispatcherServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(parent);

		assertThat(ear).isEqualTo("ear");
		assertThat(root).isEqualTo("root");
		assertThat(dispatcher).isEqualTo("dispatcher");
	}

}
