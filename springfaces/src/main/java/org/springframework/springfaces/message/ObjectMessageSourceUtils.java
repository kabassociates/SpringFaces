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
package org.springframework.springfaces.message;

import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.util.Assert;

/**
 * Miscellaneous {@link ObjectMessageSource} utility methods.
 * 
 * @author Phillip Webb
 */
public abstract class ObjectMessageSourceUtils {

	/**
	 * Get an {@link ObjectMessageSource} from the specified <tt>messageSource</tt>. If the <tt>messageSource</tt>
	 * cannot be cast to an {@link ObjectMessageSource} a new {@link DefaultObjectMessageSource} will be returned.
	 * @param messageSource the message source
	 * @return a {@link ObjectMessageSource} instance
	 */
	public static ObjectMessageSource getObjectMessageSource(MessageSource messageSource) {
		Assert.notNull(messageSource, "MessageSource must not be null");
		return getObjectMessageSource(messageSource, null);
	}

	/**
	 * Get an {@link ObjectMessageSource} from the specified <tt>messageSource</tt> falling back to a Spring
	 * {@link ApplicationContext} if <tt>messageSource</tt> is <tt>null</tt>. If the resulting <tt>messageSource</tt>
	 * cannot be cast to an {@link ObjectMessageSource} a new {@link DefaultObjectMessageSource} will be returned.
	 * @param messageSource the message source (if <tt>null</tt> if the <tt>fallbackApplicationContext</tt> will be
	 * used)
	 * @param fallbackApplicationContext a fallback {@link ApplicationContext} to be used if the <tt>messageSource</tt>
	 * parameter is <tt>null</tt>. This parameter may be <tt>null</tt> as long as the <tt>messageSource</tt> parameter
	 * is not <tt>null</tt>
	 * @return a {@link ObjectMessageSource} instance
	 */
	public static ObjectMessageSource getObjectMessageSource(MessageSource messageSource,
			ApplicationContext fallbackApplicationContext) {
		if (messageSource == null) {
			messageSource = getMessageSource(fallbackApplicationContext);
		}
		if (messageSource instanceof ObjectMessageSource) {
			return (ObjectMessageSource) messageSource;
		}
		return new DefaultObjectMessageSource(messageSource);
	}

	/**
	 * Get a {@link MessageSource} for the given {@link ApplicationContext}. This method will attempt to access the
	 * message source bean directly so that it can be cast to an {@link ObjectMessageSource} instance. If the message
	 * source bean cannot be accessed the {@link ApplicationContext} itself is returned.
	 * @param applicationContext the application context
	 * @return a message source instance
	 */
	private static MessageSource getMessageSource(ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "ApplicationContext must not be null");
		if (applicationContext.containsBean(AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME)) {
			Object messageSourceBean = applicationContext.getBean(AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME);
			if (messageSourceBean instanceof MessageSource) {
				return (MessageSource) messageSourceBean;
			}
		}
		return applicationContext;
	}
}
