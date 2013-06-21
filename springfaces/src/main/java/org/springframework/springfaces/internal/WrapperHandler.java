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
package org.springframework.springfaces.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.springfaces.FacesWrapperFactory;
import org.springframework.springfaces.SpringFacesIntegration;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * Utility class that can wrap JSF objects by consulting all {@link FacesWrapperFactory} objects registered within the
 * {@link WebApplicationContext} containing the {@link SpringFacesIntegration} bean.
 * <p>
 * Wrapping will be re-applied if whenever the {@link WebApplicationContext} is reloaded. If no
 * {@link SpringFacesIntegration} is {@link SpringFacesIntegration#isInstalled(ExternalContext) installed} then the
 * original delegate is returned as the wrapped instance.
 * 
 * @author Phillip Webb
 * @param <T> The JSF type being managed
 * @see #getWrapped()
 */
class WrapperHandler<T> {

	private final Log logger = LogFactory.getLog(getClass());

	/**
	 * The type of JSF object being managed.
	 */
	private Class<?> typeClass;

	/**
	 * access to the wrapped instance
	 */
	private WrappedAccessor<T> wrappedAccessor;

	/**
	 * The fully wrapped implementation. This is late binding.
	 * @see #getWrapped()
	 */
	private T wrapped;

	/**
	 * The date that the application context used to create the wrapped object was last refreshed.
	 */
	private Date lastRefreshedDate;

	private boolean warnOnMissingSpringFaces;

	/**
	 * Create a mew WrapperHandler.
	 * @param typeClass The JSF type being wrapped
	 * @param wrapped The root delegate
	 */
	public WrapperHandler(Class<T> typeClass, T wrapped) {
		Assert.notNull(typeClass, "TypeClass must not be null");
		Assert.notNull(wrapped, "Delegate must not be null");
		this.typeClass = typeClass;
		this.wrappedAccessor = new DirectAccessor<T>(wrapped);
	}

	/**
	 * Create a mew WrapperHandler.
	 * @param typeClass The JSF type being wrapped
	 * @param delegate Access to the root delegate
	 */
	public WrapperHandler(Class<T> typeClass, WrappedAccessor<T> delegate) {
		Assert.notNull(typeClass, "TypeClass must not be null");
		Assert.notNull(delegate, "Delegate must not be null");
		this.typeClass = typeClass;
		this.wrappedAccessor = delegate;
	}

	/**
	 * Set if a warning message is logged due to {@link SpringFacesIntegration} not being installed. Defaults to false
	 * as most wrappers can be instantiated before Spring.
	 * @param warnOnMissingSpringFaces if a warning message should be logged
	 */
	public void setWarnOnMissingSpringFaces(boolean warnOnMissingSpringFaces) {
		this.warnOnMissingSpringFaces = warnOnMissingSpringFaces;
	}

	/**
	 * Creates a fully wrapped implementation of the delegate by consulting all {@link FacesWrapperFactory factories}
	 * registered with Spring.
	 * @return a wrapped implementation
	 */
	public T getWrapped() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (facesContext == null) {
			// Calls to wrapped instances can occur when there is no faces context if JSF has not yet completely
			// intialized. We allow these early calls to proceed to the delegate.
			this.wrapped = null;
			return this.wrappedAccessor.getWrapped(WrappedAccessType.WRAP);
		}
		ExternalContext externalContext = facesContext.getExternalContext();
		if ((this.wrapped == null)
				|| (SpringFacesIntegration.isInstalled(externalContext) && (!SpringFacesIntegration
						.getLastRefreshedDate(externalContext).equals(this.lastRefreshedDate)))) {
			WrappedAccessType accessType = (this.wrapped == null ? WrappedAccessType.WRAP : WrappedAccessType.REWRAP);
			if (this.logger.isDebugEnabled()) {
				this.logger.debug((accessType == WrappedAccessType.WRAP ? "Wrapping " : "Rewrapping ")
						+ this.wrappedAccessor.getDescription());
			}
			this.wrapped = wrap(externalContext, this.wrappedAccessor.getWrapped(accessType));
			if (SpringFacesIntegration.isInstalled(externalContext)) {
				this.lastRefreshedDate = SpringFacesIntegration.getLastRefreshedDate(externalContext);
			}
		}
		return this.wrapped;
	}

	/**
	 * Wrap the specified delegate by consulting all {@link FacesWrapperFactory factories} registered with Spring.
	 * @param externalContext the external context
	 * @param delegate the root delegate
	 * @return a wrapped implementation
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private T wrap(ExternalContext externalContext, T delegate) {
		if (!SpringFacesIntegration.isInstalled(externalContext)) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("SpringFacesSupport is not yet installed, wrapping will be deferred");
			}
			if (this.logger.isWarnEnabled() && this.warnOnMissingSpringFaces) {
				this.logger
						.warn("SpringFacesSupport is not installed, full Spring/JSF integration may not be availble");
			}
			return delegate;
		}

		ApplicationContext applicationContext = SpringFacesIntegration.getCurrentInstance(externalContext)
				.getApplicationContext();

		List<Map.Entry<String, FacesWrapperFactory>> orderdBeans = new ArrayList<Map.Entry<String, FacesWrapperFactory>>();
		orderdBeans.addAll(BeanFactoryUtils
				.beansOfTypeIncludingAncestors(applicationContext, FacesWrapperFactory.class).entrySet());
		Collections.sort(orderdBeans, new OrderedMapEntryComparator());
		T rtn = delegate;
		for (Map.Entry<String, FacesWrapperFactory> entry : orderdBeans) {
			FacesWrapperFactory factory = entry.getValue();
			if (isFactorySupported(factory)) {
				T wrapper = (T) factory.newWrapper(this.typeClass, rtn);
				if (wrapper != null) {
					Assert.isInstanceOf(this.typeClass, wrapper, "FacesWrapperFactory " + entry.getValue()
							+ " returned incorrect type ");
					if (this.logger.isDebugEnabled()) {
						this.logger.debug("Wrapping " + this.typeClass.getSimpleName() + " with " + wrapper.getClass()
								+ " obtained from FacesWrapperFactory " + entry.getValue());
					}
					postProcessWrapper(wrapper);
					rtn = wrapper;
				}
			}
		}
		return rtn;
	}

	/**
	 * Determine if a given {@link FacesWrapperFactory} is suitable by resolving generic arguments.
	 * @param factory the factory to test
	 * @return <tt>true</tt> if the <tt>factory</tt> is supported, otherwise <tt>false</tt>
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean isFactorySupported(FacesWrapperFactory factory) {
		Class typeArg = GenericTypeResolver.resolveTypeArgument(factory.getClass(), FacesWrapperFactory.class);
		if (typeArg == null) {
			Class targetClass = AopUtils.getTargetClass(factory);
			if (targetClass != factory.getClass()) {
				typeArg = GenericTypeResolver.resolveTypeArgument(targetClass, FacesWrapperFactory.class);
			}
		}
		return (typeArg == null || typeArg.isAssignableFrom(this.typeClass));
	}

	/**
	 * Strategy method called after a wrapped instance has been created. Subclasses can implement custom post-processing
	 * as required.
	 * @param wrapped the newly created wrapped instance
	 */
	protected void postProcessWrapper(T wrapped) {
	}

	/**
	 * Convenience factory method to create a {@link WrapperHandler} with the generic type obtained from
	 * <tt>typeClass</tt>
	 * @param <T> the JSF type being managed
	 * @param typeClass the JSF type being managed
	 * @param delegate the delegate
	 * @return a {@link WrapperHandler}
	 */
	public static <T> WrapperHandler<T> get(Class<T> typeClass, T delegate) {
		return new WrapperHandler<T>(typeClass, delegate);
	}

	/**
	 * {@link Comparator} implementation to sort {@link Map.Entry} values by {@link org.springframework.core.Ordered} as
	 * well as the {@link Order} annotation.
	 */
	private static class OrderedMapEntryComparator extends AnnotationAwareOrderComparator {
		@Override
		public int compare(Object o1, Object o2) {
			return super.compare(((Map.Entry<?, ?>) o1).getValue(), ((Map.Entry<?, ?>) o2).getValue());
		}
	}

	/**
	 * The various reasons that a delegate can be accessed.
	 */
	public enum WrappedAccessType {
		/**
		 * The delegate is required for an initial wrap.
		 */
		WRAP,
		/**
		 * The delegate is required for a re-wrap. This can occur if the application context has been refreshed.
		 */
		REWRAP
	};

	/**
	 * Interface to provide access to the underlying wrapped delegate. Implementations can return a different delegate
	 * if required.
	 * @param <T> The wrapped delegate type
	 */
	public static interface WrappedAccessor<T> {
		/**
		 * Returns a description of the wrapped item.
		 * @return the description
		 */
		public String getDescription();

		/**
		 * Returns the actual delegate to use.
		 * @param accessType the reason that the delegate is being accessed
		 * @return the delegate
		 */
		public T getWrapped(WrappedAccessType accessType);
	}

	/**
	 * Implementation of {@link WrapperHandler.WrappedAccessor} that simple returns an object instance.
	 * @param <T> the data type
	 */
	private static class DirectAccessor<T> implements WrappedAccessor<T> {

		private T wrapped;

		public DirectAccessor(T delegate) {
			this.wrapped = delegate;
		}

		public String getDescription() {
			return this.wrapped.getClass().getName();
		}

		public T getWrapped(WrappedAccessType accessType) {
			return this.wrapped;
		}
	}
}
