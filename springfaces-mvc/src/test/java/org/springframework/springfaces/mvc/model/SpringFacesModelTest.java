/*
 * Copyright 2010-2012 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.springfaces.mvc.model;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;

/**
 * Tests for {@link SpringFacesModel}.
 * 
 * @author Phillip Webb
 */
public class SpringFacesModelTest {

	@Rule
	public ExpectedException thown = ExpectedException.none();

	@Test
	public void shouldRequireExistingModel() throws Exception {
		this.thown.expect(IllegalArgumentException.class);
		this.thown.expectMessage("Source must not be null");
		new SpringFacesModel(null);
	}

	@Test
	public void shouldCreateFromExistingModel() throws Exception {
		Map<String, String> source = new HashMap<String, String>();
		source.put("k", "v");
		SpringFacesModel model = new SpringFacesModel(source);
		assertThat(model.get("k"), is(equalTo((Object) "v")));
	}

	@Test
	public void shouldSupportSpringTypes() throws Exception {
		SpringFacesModel model = new SpringFacesModel();
		assertThat(model, is(instanceOf(ExtendedModelMap.class)));
		assertThat(model, is(instanceOf(ModelMap.class)));
		assertThat(model, is(instanceOf(Model.class)));
		assertThat(model, is(instanceOf(Map.class)));
	}
}
