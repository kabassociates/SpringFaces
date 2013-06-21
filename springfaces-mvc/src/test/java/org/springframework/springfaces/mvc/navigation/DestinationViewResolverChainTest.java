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
package org.springframework.springfaces.mvc.navigation;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.faces.context.FacesContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.springfaces.mvc.model.SpringFacesModel;
import org.springframework.web.servlet.ModelAndView;

/**
 * Tests for {@link DestinationViewResolverChain}.
 * 
 * @author Phillip Webb
 */
@RunWith(MockitoJUnitRunner.class)
public class DestinationViewResolverChainTest {

	private DestinationViewResolverChain chain = new DestinationViewResolverChain();

	@Mock
	private FacesContext context;

	private Locale locale = Locale.FRANCE;

	private Object destination = new Object();

	private SpringFacesModel model = new SpringFacesModel();

	@Test
	public void shouldReturnNullWhenNullResolvers() throws Exception {
		assertThat(this.chain.resolveDestination(this.context, this.destination, this.locale, this.model),
				is(nullValue()));
	}

	@Test
	public void shouldReturnFirstSuitableResolver() throws Exception {
		ModelAndView modelAndView = mock(ModelAndView.class);
		List<DestinationViewResolver> resolvers = new ArrayList<DestinationViewResolver>();
		DestinationViewResolver r1 = mock(DestinationViewResolver.class);
		DestinationViewResolver r2 = mock(DestinationViewResolver.class);
		DestinationViewResolver r3 = mock(DestinationViewResolver.class);
		resolvers.add(r1);
		resolvers.add(r2);
		resolvers.add(r3);
		given(r2.resolveDestination(this.context, this.destination, this.locale, this.model)).willReturn(modelAndView);
		this.chain.setResolvers(resolvers);
		ModelAndView resolved = this.chain.resolveDestination(this.context, this.destination, this.locale, this.model);
		assertThat(resolved, is(sameInstance(modelAndView)));
		verify(r1).resolveDestination(this.context, this.destination, this.locale, this.model);
		verify(r3, never()).resolveDestination(this.context, resolved, this.locale, this.model);
	}
}
