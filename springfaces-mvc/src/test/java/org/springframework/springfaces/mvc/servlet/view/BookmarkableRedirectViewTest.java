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
package org.springframework.springfaces.mvc.servlet.view;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for {@link BookmarkableRedirectView}.
 * 
 * @author Phillip Webb
 */
@RunWith(MockitoJUnitRunner.class)
public class BookmarkableRedirectViewTest {

	@Mock
	private HttpServletRequest request;

	@Before
	public void setupMocks() {
		given(this.request.getContextPath()).willReturn("/context");
	}

	@Test
	public void shouldExpandPathVariables() throws Exception {
		BookmarkableRedirectView view = new BookmarkableRedirectView("/ab/{cd}/ef/{gh}");
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("cd", "CD");
		model.put("gh", "GH");
		String actual = view.getBookmarkUrl(model, this.request);
		assertThat(actual, is(equalTo("/ab/CD/ef/GH")));
	}

	@Test
	@Ignore
	public void shouldEncodePathVariables() throws Exception {
		BookmarkableRedirectView view = new BookmarkableRedirectView("/ab/{cd}");
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("cd", "C D");
		String actual = view.getBookmarkUrl(model, this.request);
		assertThat(actual, is(equalTo("/ab/C%20D")));
	}

	@Test
	public void shouldNotAddQueryParamForPathVariable() throws Exception {
		BookmarkableRedirectView view = new BookmarkableRedirectView("/ab/{cd}");
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("cd", "CD");
		model.put("gh", "GH");
		String actual = view.getBookmarkUrl(model, this.request);
		assertThat(actual, is(equalTo("/ab/CD?gh=GH")));
	}

	@Test
	public void shouldFailIfPathVariableNotInModel() throws Exception {

	}

	@Test
	public void shouldAddContextPath() throws Exception {
		BookmarkableRedirectView view = new BookmarkableRedirectView("/ab", true);
		String actual = view.getBookmarkUrl(null, this.request);
		assertThat(actual, is(equalTo("/context/ab")));
	}

	@Test
	public void shouldNotAddContextPathIfNotSlashPrefixed() throws Exception {
		BookmarkableRedirectView view = new BookmarkableRedirectView("ab", true);
		String actual = view.getBookmarkUrl(null, this.request);
		assertThat(actual, is(equalTo("ab")));
	}

	@Test
	public void shouldRedirectWithPathVariables() throws Exception {
		BookmarkableRedirectView view = new BookmarkableRedirectView("/ab/{cd}");
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("cd", "CD");
		String expected = "/ab/CD";
		HttpServletResponse response = mock(HttpServletResponse.class);
		given(response.encodeRedirectURL(expected)).willReturn(expected);
		view.render(model, this.request, response);
		verify(response).sendRedirect(expected);
	}

	@Test
	public void shouldRenderWithFacesContext() throws Exception {

	}
}
