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

package cn.taketoday.test.web.servlet.samples.standalone;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.MvcResult;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.RestController;

import java.time.Duration;

import reactor.core.publisher.Flux;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.request;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests with reactive return value types.
 *
 * @author Rossen Stoyanchev
 */
public class ReactiveReturnTypeTests {


	@Test // SPR-16869
	public void sseWithFlux() throws Exception {

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(ReactiveController.class).build();

		MvcResult mvcResult = mockMvc.perform(get("/spr16869"))
				.andExpect(request().asyncStarted())
				.andExpect(status().isOk())
				.andReturn();

		mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(content().string("data:event0\n\ndata:event1\n\ndata:event2\n\n"));
	}



	@RestController
	static class ReactiveController {

		@GetMapping(path = "/spr16869", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
		Flux<String> sseFlux() {
			return Flux.interval(Duration.ofSeconds(1)).take(3)
					.map(aLong -> String.format("event%d", aLong));
		}
	}

}
