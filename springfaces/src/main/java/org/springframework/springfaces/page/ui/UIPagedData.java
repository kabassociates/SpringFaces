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
package org.springframework.springfaces.page.ui;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;

import org.springframework.springfaces.model.DataModelRowSet;
import org.springframework.springfaces.model.DefaultDataModelRowSet;
import org.springframework.springfaces.model.LazyDataLoader;
import org.springframework.springfaces.page.model.PagedDataModel;
import org.springframework.springfaces.page.model.PagedDataModelState;
import org.springframework.springfaces.page.model.PagedDataRows;
import org.springframework.springfaces.page.model.PrimeFacesPagedDataModel;
import org.springframework.springfaces.util.FacesUtils;
import org.springframework.util.Assert;

/**
 * Component that can be used to create a paged {@link DataModel} that lazily fetches data from an underlying source.
 * The <tt>value</tt> expression will be called each time new data needs to be fetched and the optional
 * <tt>rowCount</tt> expression will be used to determine the total number of rows. The expression should use the
 * <tt>pageRequest</tt> variable to access {@link PageRequest context} information about the specific data that needs to
 * be returned.
 * <p>
 * For example:
 * 
 * <pre>
 * &lt;s:pagedData value="#{userRepository.findByLastName(backingBean.lastName, pageRequest.offset, pageRequest.pageSize)}"
 *    rowCount="#{userRepository.countByLastName(backingBean.lastName)}"/&gt;
 * 
 * &lt;!-- use the variable pagedData with a scrolled data table --&gt;
 * </pre>
 * <p>
 * The resulting data model is made available as a request scoped variable named '<tt>pagedData</tt>'. You can set a
 * different name using the <tt>var</tt> attribute. The data model will extend the JSF {@link DataModel} class and also
 * implements the {@link PagedDataRows} interface. By default the data model will fetch 10 rows at a time, this can be
 * configured using the <tt>pageSize</tt> attribute.
 * <p>
 * If Spring Data is present on the classpath then <tt>pageRequest</tt> will also implement the
 * <tt>org.springframework.data.domain.Pageable</tt> interface. The <tt>value</tt> expression can also return a
 * <tt>org.springframework.data.domain.Page</tt> removing the need to use <tt>rowCount</tt>.
 * 
 * <pre>
 * &lt;s:pagedData value="#{userRepository.findByLastName(backingBean.lastName, pageRequest)}"/&gt;
 * </pre>
 * <p>
 * If PrimeFaces is present on the classpath then the resulting model will extend
 * <tt>org.primefaces.model.LazyDataModel</tt> rather than <tt>javax.faces.model.DataModel</tt>. Use the
 * {@link PagedDataRows} interface if you need a consistent way of dealing with PrimeFaces and Standard DataModels.
 * 
 * @author Phillip Webb
 * @see PageRequest
 * @see PagedDataRows
 */
public class UIPagedData extends UIComponentBase {

	public static final String COMPONENT_FAMILY = "spring.faces.PagedData";

	private static final String DEFAULT_VAR = "pagedData";
	private static final Object DEFAULT_PAGE_SIZE = 10;
	private static final String PAGE_REQUEST_VARIABLE = "pageRequest";

	private static PagedPrimeFacesSupport primeFacesSupport = PagedPrimeFacesSupport.getInstance();
	private static PagedSpringDataSupport springDataSupport = PagedSpringDataSupport.getInstance();

	@Override
	public String getFamily() {
		return COMPONENT_FAMILY;
	}

	/**
	 * Return the request-scope attribute under which the {@link PagedDataModel} will be exposed. This property is
	 * <b>not</b> enabled for value binding expressions.
	 * @return The variable name
	 */
	public String getVar() {
		String var = (String) getStateHelper().get(PropertyKeys.var);
		return var == null ? DEFAULT_VAR : var;
	}

	/**
	 * Set the request-scope attribute under which the {@link PagedDataModel} will be exposed.
	 * @param var The new request-scope attribute name
	 */
	public void setVar(String var) {
		getStateHelper().put(PropertyKeys.var, var);
	}

	/**
	 * Returns the expression used to obtain a page of data. This expression can be called many times as
	 * {@link PagedDataRows} are navigated. The resulting expression should return a List of rows or, if Spring Data is
	 * being used a <tt>org.springframework.data.domain.Page</tt> object can also be returned.
	 * @return the {@link ValueExpression} to obtain the page data
	 */
	protected ValueExpression getValue() {
		ValueExpression value = getValueExpression(PropertyKeys.value.toString());
		Assert.notNull(value, "UIPageData components must include a value attribute");
		return value;
	}

	/**
	 * Returns the optional expression used to obtain the total row count. This expression can be called many times as
	 * {@link PagedDataRows} are navigated. The resulting expression should return an int or long value.
	 * @return the {@link ValueExpression} to obtain the number of rows
	 */
	protected ValueExpression getRowCount() {
		return getValueExpression(PropertyKeys.rowCount.toString());
	}

	/**
	 * Return the initial page size for the {@link PagedDataRows}. If not specified the default value of 10 is used.
	 * @return the page size
	 */
	public int getPageSize() {
		return (Integer) getStateHelper().eval(PropertyKeys.pageSize, DEFAULT_PAGE_SIZE);
	}

	/**
	 * Set the initial page size for the {@link PagedDataRows}.
	 * @param pageSize the page size
	 */
	public void setPageSize(int pageSize) {
		Assert.isTrue(pageSize > 0, "PageSize must be a positive number");
		getStateHelper().put(PropertyKeys.pageSize, pageSize);
	}

	/**
	 * Return the initial sort column for the {@link PagedDataRows}.
	 * @return the sort column
	 */
	public String getSortColumn() {
		return (String) getStateHelper().eval(PropertyKeys.sortColumn);
	}

	/**
	 * Set the initial sort column for the {@link PagedDataRows}.
	 * @param sortColumn the sort column
	 */
	public void setSortColumn(String sortColumn) {
		getStateHelper().put(PropertyKeys.sortColumn, sortColumn);
	}

	/**
	 * Returns the initial sort ascending values for the {@link PagedDataRows}.
	 * @return the sort ascending values.
	 */
	public Boolean getSortAscending() {
		return (Boolean) getStateHelper().eval(PropertyKeys.sortAscending);
	}

	/**
	 * Set the initial sort ascending value for the {@link PagedDataRows}.
	 * @param sortAscending the sort ascending value
	 */
	public void setSortAscending(Boolean sortAscending) {
		getStateHelper().put(PropertyKeys.sortAscending, sortAscending);
	}

	@Override
	public void restoreState(FacesContext context, Object state) {
		super.restoreState(context, state);
		// Components may need to refer to previous data during decode
		createPagedDataInRequestMap(context);
	}

	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		createPagedDataInRequestMap(context);
		super.encodeEnd(context);
	}

	private void createPagedDataInRequestMap(FacesContext context) {
		Object pagedData = createPagedData();
		Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
		requestMap.put(getVar(), pagedData);
	}

	/**
	 * Factory method used to create the paged data object to be exposed. By default this method will return a
	 * {@link DataModel} subclass (either {@link PagedDataModel} or {@link PrimeFacesPagedDataModel}).
	 * @return the paged data to expose
	 */
	protected Object createPagedData() {
		LazyDataLoader<Object, PagedDataModelState> lazyDataLoader = new LazyDataLoader<Object, PagedDataModelState>() {
			public DataModelRowSet<Object> getRows(PagedDataModelState state) {
				return UIPagedData.this.getRows(state);
			}
		};
		PagedDataModelState state = (PagedDataModelState) getStateHelper().get(PropertyKeys.dataModelstate);
		if (state == null) {
			state = new PagedDataModelState(getPageSize());
			state.setSortColumn(getSortColumn());
			if (getSortAscending() != null) {
				state.setSortAscending(getSortAscending().booleanValue());
			}
			getStateHelper().put(PropertyKeys.dataModelstate, state);
		}
		return adaptPagedDataModel(new PagedDataModel<Object>(lazyDataLoader, state));
	}

	/**
	 * Strategy method called to adapt a {@link PagedDataModel} to a more appropriate subclass. By default this method
	 * is used to support PrimeFaces.
	 * @param pagedDataModel the data model
	 * @return the adapted model
	 */
	protected Object adaptPagedDataModel(PagedDataModel<Object> pagedDataModel) {
		return primeFacesSupport.wrapPagedDataRows(pagedDataModel);
	}

	/**
	 * Strategy method used to obtain the rows to be used by the {@link PagedDataModel}. By default this method will
	 * expose a <tt>pageRequest</tt> before calling the appropriate EL expressions.
	 * @param state the state
	 * @return the data model rows
	 * @see #getRowCountFromValue(Object)
	 * @see #getContentFromValue(Object)
	 */
	protected DataModelRowSet<Object> getRows(PagedDataModelState state) {
		final PageRequest pageRequest = createPageRequest(state);
		return FacesUtils.doWithRequestScopeVariable(getFacesContext(), PAGE_REQUEST_VARIABLE, pageRequest,
				new Callable<DataModelRowSet<Object>>() {
					public DataModelRowSet<Object> call() throws Exception {
						return executeExpressionsToGetRows(pageRequest);
					}
				});
	}

	/**
	 * Create the page request to expose. This method also deals with adding Spring Data <tt>Pageable</tt> support.
	 * @param state the state
	 * @return a page request
	 */
	private PageRequest createPageRequest(PagedDataModelState state) {
		PageRequest pageRequest = new PageRequestAdapter(state);
		return springDataSupport.makePageable(pageRequest);
	}

	/**
	 * Executes the appropriate EL expression to obtain page and row count data.
	 * @param pageRequest the page request
	 * @return the data model rows
	 */
	private DataModelRowSet<Object> executeExpressionsToGetRows(PageRequest pageRequest) {
		ELContext context = getFacesContext().getELContext();
		ValueExpression valueExpression = getValue();
		ValueExpression rowCountExpression = getRowCount();
		Object value = valueExpression.getValue(context);
		Assert.state(value != null, "UIPageData value returned null result");
		Object rowCount = (rowCountExpression == null ? null : rowCountExpression.getValue(context));
		return getRowsFromExpressionResults(pageRequest, value, rowCount);
	}

	/**
	 * Obtains row data from the results of the EL expressions.
	 * @param pageRequest the page request
	 * @param value the value EL result
	 * @param rowCount the rowCount EL result
	 * @return the data model rows
	 */
	@SuppressWarnings("unchecked")
	private DataModelRowSet<Object> getRowsFromExpressionResults(PageRequest pageRequest, Object value, Object rowCount) {
		if (rowCount == null) {
			rowCount = getRowCountFromValue(value);
		}
		value = getContentFromValue(value);
		long totalRowCount = -1;
		Assert.isInstanceOf(List.class, value);
		if (rowCount != null) {
			Assert.isInstanceOf(Number.class, rowCount);
			totalRowCount = ((Number) rowCount).longValue();
		}
		return new DefaultDataModelRowSet<Object>(pageRequest.getOffset(), (List<Object>) value,
				pageRequest.getPageSize(), totalRowCount);
	}

	/**
	 * Strategy method used to obtain a count from the value EL result. This method is called when no rowCount EL
	 * expression is specified. By default this method will deal with Spring Data <tt>Page</tt> results.
	 * @param value the value EL result
	 * @return a row count or <tt>null</tt>
	 */
	protected Object getRowCountFromValue(Object value) {
		return springDataSupport.getRowCountFromPage(value);
	}

	/**
	 * Strategy method used to obtain content from the value EL result. By default this method will deal with Spring
	 * Data <tt>Page</tt> results.
	 * @param value the value EL result
	 * @return contents extracted from the value of the original value unchanged.
	 */
	protected Object getContentFromValue(Object value) {
		return springDataSupport.getContentFromPage(value);
	}

	private enum PropertyKeys {
		value, rowCount, var, pageSize, sortColumn, sortAscending, dataModelstate
	}
}
