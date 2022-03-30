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

package cn.taketoday.test.context.junit4.spr6128;

import org.junit.Test;
import org.junit.runner.RunWith;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.ApplicationJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to verify claims made in <a
 * href="https://jira.springframework.org/browse/SPR-6128"
 * target="_blank">SPR-6128</a>.
 *
 * @author Sam Brannen
 * @author Chris Beams
 * @since 3.0
 */
@ContextConfiguration
@RunWith(ApplicationJUnit4ClassRunner.class)
public class AutowiredQualifierTests {

	@Autowired
	private String foo;

	@Autowired
	@Qualifier("customFoo")
	private String customFoo;


	@Test
	public void test() {
		assertThat(foo).isEqualTo("normal");
		assertThat(customFoo).isEqualTo("custom");
	}

}