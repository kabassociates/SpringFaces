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
package org.springframework.springfaces.mvc.method.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Convenient base class for any {@link HandlerMethodArgumentResolver}s that resolve objects against some implicit
 * context.
 * 
 * @author Phillip Webb
 * @see #add(Class, Callable, Callable)
 */
public abstract class ImplicitObjectMethodArgumentResolver implements HandlerMethodArgumentResolver {

	/**
	 * {@link Callable} that always returns true.
	 */
	protected static final Callable<Boolean> ALWAYS = new Callable<Boolean>() {
		public Boolean call() throws Exception {
			return Boolean.TRUE;
		}
	};

	private List<ImplicitObject<?>> implicitObjects = new ArrayList<ImplicitObject<?>>();

	/**
	 * Creates a new {@link ImplicitObjectMethodArgumentResolver}. Subclasses are expected to implement a constructor
	 * that calls the {@link #add(Class, Callable, Callable) add} method in order to register the implicit objects that
	 * are supported.
	 */
	public ImplicitObjectMethodArgumentResolver() {
		super();
	}

	/**
	 * Add support for the specified type. See {@link #add(Class, Callable, Callable)} for details.
	 * @param <T> the type being added
	 * @param type the class type to add
	 * @param call A {@link Callable} that will be used to obtain the object instance
	 * @see #add(Class, Callable, Callable)
	 */
	protected final <T> void add(Class<T> type, Callable<T> call) {
		add(type, ALWAYS, call);
	}

	/**
	 * Add support for the specified type when a condition matches. This method is expected to be called from subclass
	 * constructors in order to register supported types.
	 * 
	 * @param <T> the type being added
	 * @param type the class type to add
	 * @param condition A {@link Callable} that will be used to determine if the object is available.
	 * @param call A {@link Callable} that will be used to obtain the object instance
	 * @see #ALWAYS
	 * @see #add(Class, Callable)
	 */
	protected final <T> void add(Class<T> type, Callable<Boolean> condition, Callable<T> call) {
		Assert.notNull(type, "Type must not be null");
		Assert.notNull(condition, "Condition must not be null");
		Assert.notNull(call, "Call must not be null");
		this.implicitObjects.add(new ImplicitObject<T>(type, condition, call));
	}

	public boolean supportsParameter(MethodParameter parameter) {
		try {
			for (ImplicitObject<?> implicitObject : this.implicitObjects) {
				if (parameter.getParameterType().isAssignableFrom(implicitObject.getType())
						&& Boolean.TRUE.equals(implicitObject.getCondition().call())) {
					return true;
				}
			}
		} catch (Exception e) {
			ReflectionUtils.rethrowRuntimeException(e);
		}
		return false;
	}

	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		for (ImplicitObject<?> implicitObject : this.implicitObjects) {
			if (implicitObject.getType().isAssignableFrom(parameter.getParameterType())
					&& Boolean.TRUE.equals(implicitObject.getCondition().call())) {
				return implicitObject.getCall().call();
			}
		}
		return null;
	}

	private static class ImplicitObject<T> {
		private Class<T> type;
		private Callable<Boolean> condition;
		private Callable<T> call;

		public ImplicitObject(Class<T> type, Callable<Boolean> condition, Callable<T> call) {
			this.type = type;
			this.condition = condition;
			this.call = call;
		}

		public Class<T> getType() {
			return this.type;
		}

		public Callable<Boolean> getCondition() {
			return this.condition;
		}

		public Callable<T> getCall() {
			return this.call;
		}
	}
}
