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

package cn.taketoday.test.web.servlet.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.reactive.server.EntityExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.ResultActions;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.web.bind.annotation.PostMapping;
import cn.taketoday.web.servlet.mvc.support.RedirectAttributes;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.flash;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.standalone.resultmatchers.FlashAttributeAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class FlashAttributeAssertionTests {

	private final WebTestClient client =
			MockMvcWebTestClient.bindToController(new PersonController())
					.alwaysExpect(status().isFound())
					.alwaysExpect(flash().attributeCount(3))
					.build();


	@Test
	void attributeCountWithWrongCount() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> performRequest().andExpect(flash().attributeCount(1)))
			.withMessage("FlashMap size expected:<1> but was:<3>");
	}

	@Test
	void attributeExists() throws Exception {
		performRequest().andExpect(flash().attributeExists("one", "two", "three"));
	}

	@Test
	void attributeEqualTo() throws Exception {
		performRequest()
				.andExpect(flash().attribute("one", "1"))
				.andExpect(flash().attribute("two", 2.222))
				.andExpect(flash().attribute("three", new URL("https://example.com")));
	}

	@Test
	void attributeMatchers() throws Exception {
		performRequest()
				.andExpect(flash().attribute("one", containsString("1")))
				.andExpect(flash().attribute("two", closeTo(2, 0.5)))
				.andExpect(flash().attribute("three", notNullValue()))
				.andExpect(flash().attribute("one", equalTo("1")))
				.andExpect(flash().attribute("two", equalTo(2.222)))
				.andExpect(flash().attribute("three", equalTo(new URL("https://example.com"))));
	}

	private ResultActions performRequest() {
		EntityExchangeResult<Void> result = client.post().uri("/persons").exchange().expectBody().isEmpty();
		return MockMvcWebTestClient.resultActionsFor(result);
	}


	@Controller
	private static class PersonController {

		@PostMapping("/persons")
		String save(RedirectAttributes redirectAttrs) throws Exception {
			redirectAttrs.addFlashAttribute("one", "1");
			redirectAttrs.addFlashAttribute("two", 2.222);
			redirectAttrs.addFlashAttribute("three", new URL("https://example.com"));
			return "redirect:/person/1";
		}
	}

}