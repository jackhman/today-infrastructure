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

package cn.taketoday.test.context.groovy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.testfixture.beans.Employee;
import cn.taketoday.beans.testfixture.beans.Pet;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test class that verifies proper support for mixing XML
 * configuration files and Groovy scripts to load an {@code ApplicationContext}
 * using the TestContext framework.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@ExtendWith(ApplicationExtension.class)
@ContextConfiguration({ "contextA.groovy", "contextB.xml" })
class MixedXmlAndGroovySpringContextTests {

	@Autowired
	Employee employee;

	@Autowired
	Pet pet;

	@Autowired
	String foo;

	@Autowired
	String bar;


	@Test
	void verifyAnnotationAutowiredFields() {
		assertThat(this.employee).as("The employee field should have been autowired.").isNotNull();
		assertThat(this.employee.getName()).isEqualTo("Dilbert");

		assertThat(this.pet).as("The pet field should have been autowired.").isNotNull();
		assertThat(this.pet.getName()).isEqualTo("Dogbert");

		assertThat(this.foo).as("The foo field should have been autowired.").isEqualTo("Groovy Foo");
		assertThat(this.bar).as("The bar field should have been autowired.").isEqualTo("XML Bar");
	}

}