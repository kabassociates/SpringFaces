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
package org.springframework.springfaces.selectitems.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.faces.component.UIParameter;
import javax.faces.component.UISelectItem;
import javax.faces.component.UISelectItems;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.primefaces.component.selectonemenu.SelectOneMenu;

/**
 * Tests for {@link SelectItemsIterator}.
 * 
 * @author Phillip Webb
 */
public class SelectItemsIteratorTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private FacesContext context;

	private SelectOneMenu component = new SelectOneMenu();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		ExternalContext externalContext = mock(ExternalContext.class);
		Map<String, Object> requestMap = new HashMap<String, Object>();
		given(this.context.getExternalContext()).willReturn(externalContext);
		given(externalContext.getRequestMap()).willReturn(requestMap);
	}

	@Test
	public void shouldNeedFacesContext() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Context must not be null");
		new SelectItemsIterator(null, this.component);
	}

	@Test
	public void shouldNeedComponent() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Component must not be null");
		new SelectItemsIterator(this.context, null);
	}

	@Test
	public void shouldIterateOverChildComponents() throws Exception {

		// Child 1 = UISelectItems containing a Map
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("ka", "va");
		map.put(null, "vb");
		map.put("kc", null);
		UISelectItems mapItems = new UISelectItems();
		mapItems.setValue(map);
		this.component.getChildren().add(mapItems);

		// Mix in something we don't support
		this.component.getChildren().add(new UIParameter());

		// Child 2 = UISelectItems containing List
		SelectItem la = new SelectItem();
		SelectItem lb = new SelectItem();
		UISelectItems listItems = new UISelectItems();
		listItems.setValue(Arrays.asList(la, lb));
		this.component.getChildren().add(listItems);

		// Child3 = UISelectItem
		SelectItem i = new SelectItem();
		UISelectItem uiSelectItem = new UISelectItem();
		uiSelectItem.setValue(i);
		this.component.getChildren().add(uiSelectItem);

		Iterator<SelectItem> iterator = new SelectItemsIterator(this.context, this.component);
		assertThat(iterator.hasNext(), is(true));
		SelectItem i1 = iterator.next();
		assertThat(iterator.hasNext(), is(true));
		SelectItem i2 = iterator.next();
		assertThat(iterator.hasNext(), is(true));
		SelectItem i3 = iterator.next();
		assertThat(iterator.hasNext(), is(true));
		SelectItem i4 = iterator.next();
		assertThat(iterator.hasNext(), is(true));
		SelectItem i5 = iterator.next();
		assertThat(iterator.hasNext(), is(true));
		SelectItem i6 = iterator.next();
		assertThat(iterator.hasNext(), is(false));
		try {
			iterator.next();
			fail("Did not throw");
		} catch (NoSuchElementException e) {

		}

		assertThat(i1.getLabel(), is("ka"));
		assertThat(i1.getValue(), is((Object) "va"));
		assertThat(i2.getLabel(), is("vb"));
		assertThat(i2.getValue(), is((Object) "vb"));
		assertThat(i3.getLabel(), is("kc"));
		assertThat(i3.getValue(), is((Object) ""));
		assertThat(i4, is(sameInstance((Object) la)));
		assertThat(i5, is(sameInstance((Object) lb)));
		assertThat(i6, is(sameInstance((Object) i)));
	}

	@Test
	public void shouldSupportUISelectItemWithNullValue() throws Exception {
		UISelectItem uiSelectItem = new UISelectItem();
		uiSelectItem.setItemValue("v");
		uiSelectItem.setItemLabel("l");
		uiSelectItem.setItemDescription("d");
		uiSelectItem.setItemDisabled(true);
		uiSelectItem.setItemEscaped(true);
		uiSelectItem.setNoSelectionOption(true);
		this.component.getChildren().add(uiSelectItem);
		Iterator<SelectItem> iterator = new SelectItemsIterator(this.context, this.component);
		SelectItem item = iterator.next();
		assertThat(item.getValue(), is((Object) "v"));
		assertThat(item.getLabel(), is("l"));
		assertThat(item.getDescription(), is("d"));
		assertThat(item.isDisabled(), is(true));
		assertThat(item.isEscape(), is(true));
		assertThat(item.isNoSelectionOption(), is(true));
	}

	@Test
	public void shouldBuildFromUISelectItemsWithValue() throws Exception {
		Object value = "a";
		setupUISelectItems(value);
		SelectItem item = getSingleSelectItems();
		assertThat(item.getValue(), is(value));
		assertThat(item.getLabel(), is(value));
	}

	@Test
	public void shouldBuildFromUISelectItemsWithItemValue() throws Exception {
		UISelectItems selectItems = setupUISelectItems("a");
		selectItems.getAttributes().put("itemValue", "b");
		SelectItem item = getSingleSelectItems();
		assertThat(item.getValue(), is((Object) "b"));
		assertThat(item.getLabel(), is("b"));
	}

	@Test
	public void shouldBuildFromUISelectItemsWithAttributes() throws Exception {
		UISelectItems selectItems = setupUISelectItems("a");
		selectItems.getAttributes().put("itemLabel", "l");
		selectItems.getAttributes().put("itemDescription", "d");
		selectItems.getAttributes().put("itemLabelEscaped", "true");
		selectItems.getAttributes().put("itemDisabled", "true");
		selectItems.getAttributes().put("noSelectionOption", "true");
		SelectItem item = getSingleSelectItems();
		assertThat(item.getValue(), is((Object) "a"));
		assertThat(item.getLabel(), is("l"));
		assertThat(item.getDescription(), is("d"));
		assertThat(item.isEscape(), is(true));
		assertThat(item.isDisabled(), is(true));
		assertThat(item.isNoSelectionOption(), is(true));
	}

	@Test
	public void shouldBuildFromUISelectItemsWithVar() throws Exception {

		final Map<String, Object> map = spy(new HashMap<String, Object>());
		given(map.get(any())).willAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				if ("itemLabel".equals(invocation.getArguments()[0])) {
					assertThat(SelectItemsIteratorTest.this.context.getExternalContext().getRequestMap().get("var"),
							is(not(nullValue())));
					return "label";
				}
				return invocation.callRealMethod();
			}
		});
		UISelectItems selectItems = new UISelectItems() {
			@Override
			public java.util.Map<String, Object> getAttributes() {
				return map;
			};
		};
		selectItems.setValue(Collections.singleton("a"));
		this.component.getChildren().add(selectItems);
		selectItems.getAttributes().put("var", "var");
		selectItems.getAttributes().put("itemLabel", "#{var}");
		SelectItem item = getSingleSelectItems();
		assertThat(item.getLabel(), is("label"));
	}

	private UISelectItems setupUISelectItems(Object value) {
		UISelectItems uiSelectItems = new UISelectItems();
		uiSelectItems.setValue(Collections.singleton(value));
		this.component.getChildren().add(uiSelectItems);
		return uiSelectItems;
	}

	private SelectItem getSingleSelectItems() {
		Iterator<SelectItem> iterator = new SelectItemsIterator(this.context, this.component);
		SelectItem item = iterator.next();
		assertThat(iterator.hasNext(), is(false));
		return item;
	}

	@Test
	public void shouldThrowOnRemove() throws Exception {
		Iterator<SelectItem> iterator = new SelectItemsIterator(this.context, this.component);
		this.thrown.expect(UnsupportedOperationException.class);
		iterator.remove();
	}
}
