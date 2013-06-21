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
package org.springframework.springfaces.model;

import java.util.Collections;
import java.util.List;

/**
 * Default implementation of {@link DataModelRowSet}.
 * 
 * @author Phillip Webb
 * @param <E>
 */
public class DefaultDataModelRowSet<E> implements DataModelRowSet<E> {

	private static final DataModelRowSet<?> EMPTY = emptySet(-1);

	private int offset;
	private int size;
	private List<E> contents;
	private long totalRowCount;

	public DefaultDataModelRowSet(List<E> contents) {
		this(0, contents);
	}

	public DefaultDataModelRowSet(int offset, List<E> contents) {
		this(offset, contents, UNKNOWN_TOTAL_ROW_COUNT);
	}

	public DefaultDataModelRowSet(int offset, List<E> contents, long totalRowCount) {
		this(offset, contents, contents.size(), totalRowCount);
	}

	public DefaultDataModelRowSet(int offset, List<E> contents, int size, long totalRowCount) {
		this.offset = offset;
		this.contents = contents;
		this.size = size;
		this.totalRowCount = totalRowCount;
	}

	public long getTotalRowCount() {
		return this.totalRowCount;
	}

	public boolean contains(int rowIndex) {
		int ci = getContentsIndex(rowIndex);
		return (ci >= 0) && (ci < this.size);
	}

	public boolean isRowAvailable(int rowIndex) {
		int ci = getContentsIndex(rowIndex);
		return contains(rowIndex) && (ci >= 0) && (ci < this.contents.size());
	}

	public E getRowData(int rowIndex) throws NoRowAvailableException {
		if (!isRowAvailable(rowIndex)) {
			throw new NoRowAvailableException();
		}
		return this.contents.get(getContentsIndex(rowIndex));
	}

	private int getContentsIndex(int rowIndex) {
		return rowIndex - this.offset;
	}

	@SuppressWarnings("unchecked")
	public static <E> DataModelRowSet<E> emptySet() {
		return (DataModelRowSet<E>) EMPTY;
	}

	public static <E> DataModelRowSet<E> emptySet(int rowIndex) {
		return new DefaultDataModelRowSet<E>(rowIndex, Collections.<E> emptyList(), 1, UNKNOWN_TOTAL_ROW_COUNT);
	}
}
