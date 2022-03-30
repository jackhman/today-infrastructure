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

package cn.taketoday.test.web.servlet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.junit.jupiter.web.ApplicationJUnitWebConfig;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.RestController;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.Matchers.nullValue;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.request;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Integration tests that verify that {@link MockMvc} can be reused multiple
 * times within the same test method without side effects between independent
 * requests.
 * <p>See <a href="https://jira.spring.io/browse/SPR-13260" target="_blank">SPR-13260</a>.
 *
 * @author Sam Brannen
 * @author Rob Winch
 * @since 4.2
 */
@ApplicationJUnitWebConfig
@TestInstance(Lifecycle.PER_CLASS)
class MockMvcReuseTests {

	private static final String HELLO = "hello";
	private static final String ENIGMA = "enigma";
	private static final String FOO = "foo";
	private static final String BAR = "bar";

	private final MockMvc mvc;


	MockMvcReuseTests(WebApplicationContext wac) {
		this.mvc = webAppContextSetup(wac).build();
	}


	@Test
	void sessionAttributesAreClearedBetweenInvocations() throws Exception {

		this.mvc.perform(get("/"))
			.andExpect(content().string(HELLO))
			.andExpect(request().sessionAttribute(FOO, nullValue()));

		this.mvc.perform(get("/").sessionAttr(FOO, BAR))
			.andExpect(content().string(HELLO))
			.andExpect(request().sessionAttribute(FOO, BAR));

		this.mvc.perform(get("/"))
			.andExpect(content().string(HELLO))
			.andExpect(request().sessionAttribute(FOO, nullValue()));
	}

	@Test
	void requestParametersAreClearedBetweenInvocations() throws Exception {
		this.mvc.perform(get("/"))
			.andExpect(content().string(HELLO));

		this.mvc.perform(get("/").param(ENIGMA, ""))
			.andExpect(content().string(ENIGMA));

		this.mvc.perform(get("/"))
			.andExpect(content().string(HELLO));
	}


	@Configuration
	@EnableWebMvc
	static class Config {

		@Bean
		MyController myController() {
			return new MyController();
		}
	}

	@RestController
	static class MyController {

		@GetMapping("/")
		String hello() {
			return HELLO;
		}

		@GetMapping(path = "/", params = ENIGMA)
		String enigma() {
			return ENIGMA;
		}
	}

}
