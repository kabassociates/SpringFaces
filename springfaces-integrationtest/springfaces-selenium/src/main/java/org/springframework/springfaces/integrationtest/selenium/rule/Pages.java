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
package org.springframework.springfaces.integrationtest.selenium.rule;

import java.util.regex.Pattern;

import org.junit.rules.MethodRule;
import org.openqa.selenium.WebDriver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.springfaces.integrationtest.selenium.WebDriverManager;
import org.springframework.springfaces.integrationtest.selenium.WebDriverUtils;
import org.springframework.springfaces.integrationtest.selenium.page.Page;
import org.springframework.springfaces.integrationtest.selenium.page.PageURL;
import org.springframework.util.Assert;

/**
 * A JUnit {@link MethodRule} that can be used to create new {@link Page}s. Pages will have their own {@link WebDriver}
 * instance that will be automatically {@link WebDriver#close() closed} when the test method has completed.
 * 
 * @author Phillip Webb
 */
public class Pages extends ManagedWebDrivers {

	private static final Pattern HTTP_PATTERN = Pattern.compile("^https?\\:\\/\\/", Pattern.CASE_INSENSITIVE);

	private String rootUrl;

	/**
	 * Create a new {@link Pages} instance. This constructor can be used by subclasses that implement
	 * {@link #getWebDriverManager()} and {@link #getRootUrl()}.
	 */
	protected Pages() {
	}

	/**
	 * Create a new {@link Pages} instance.
	 * @param webDriverManager the web driver manager
	 * @param rootUrl the root URL
	 */
	public Pages(WebDriverManager webDriverManager, String rootUrl) {
		super(webDriverManager);
		this.rootUrl = rootUrl;
	}

	/**
	 * @return the root URL prefixed to all page URLs.
	 */
	protected String getRootUrl() {
		return rootUrl;
	}

	/**
	 * Get a new Page instance of the specified class. The page URL will be deduced from a {@link PageURL} annotation.
	 * @param pageClass the page class
	 * @return the page instance
	 */
	public <P extends Page> P get(Class<P> pageClass) {
		return getPage(pageClass, null);
	}

	/**
	 * Get a new Page instance of the specified class at the give URL.
	 * @param pageClass the page class
	 * @param url the URL (this URL will be prefixed with {@link #getRootUrl()} when necessary)
	 * @return the page instance
	 */
	public <P extends Page> P get(Class<P> pageClass, String url) {
		Assert.notNull(url, "URL must not be null");
		return getPage(pageClass, url);
	}

	private <P extends Page> P getPage(Class<P> pageClass, String url) {
		Assert.notNull(pageClass, "PageClass must not be null");
		if (url == null) {
			url = getUrlFromPageClass(pageClass);
		}
		Assert.state(url != null, "No URL specified for page and unable to deduce a URL from @PageURL annotation");
		url = prefixUrlIfRequired(url);
		WebDriver webDriver = getWebDriver();
		webDriver.get(url);
		return WebDriverUtils.newPage(webDriver, pageClass);
	}

	private String getUrlFromPageClass(Class<? extends Page> pageClass) {
		PageURL annotation = AnnotationUtils.findAnnotation(pageClass, PageURL.class);
		if (annotation != null) {
			return annotation.value();
		}
		return null;
	}

	private String prefixUrlIfRequired(String url) {
		if (HTTP_PATTERN.matcher(url).matches()) {
			return url;
		}
		String rootUrl = getRootUrl();
		if (rootUrl != null) {
			url = rootUrl + url;
		}
		return url;
	}
}
