/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.budiyev.population.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

/**
 * String table, used with {@link CsvParser}
 *
 * @see CsvParser#parse(InputStream, char, String)
 * @see CsvParser#parse(String, char)
 */
public class StringTable implements Iterable<StringRow> {
    private final ArrayList<StringRow> mRows = new ArrayList<>();

    public StringTable() {
    }

    public StringTable(int rows, int columns) {
        for (int i = 0; i < rows; i++) {
            add(new StringRow(columns));
        }
    }

    public StringTable(StringRow... rows) {
        for (StringRow row : rows) {
            add(row);
        }
    }

    public StringTable(Iterable<StringRow> rows) {
        for (StringRow row : rows) {
            add(row);
        }
    }

    @Override
    public Iterator<StringRow> iterator() {
        return mRows.iterator();
    }

    /**
     * Row
     *
     * @param index Row position in table
     * @return Row at index
     */
    public StringRow row(int index) {
        return mRows.get(index);
    }

    /**
     * Add empty row
     */
    public void add() {
        mRows.add(new StringRow());
    }

    /**
     * Add row to table
     *
     * @param row Row
     */
    public void add(StringRow row) {
        mRows.add(Objects.requireNonNull(row));
    }

    /**
     * Add row that contains specified cells
     *
     * @param cells Cells
     */
    public void add(Object... cells) {
        mRows.add(new StringRow(cells));
    }

    /**
     * Add row that contains specified cells
     *
     * @param cells Cells
     */
    public void add(Iterable<Object> cells) {
        mRows.add(new StringRow(cells));
    }

    /**
     * Insert empty row to specified position
     * <br>
     * If {@code position} is greater than or equal to {@link #size()},
     * empty rows will be inserted.
     *
     * @param position Position to insert cell value
     * @return Previous value or {@code null} if empty rows were inserted
     */
    public StringRow insert(int position) {
        return insert(position, new StringRow());
    }

    /**
     * Insert row to specified position
     * <br>
     * If {@code position} is greater than or equal to {@link #size()},
     * empty rows will be inserted.
     *
     * @param position Position to insert cell value
     * @param cells    Cells
     * @return Previous value or {@code null} if empty rows were inserted
     */
    public StringRow insert(int position, Object... cells) {
        return insert(position, new StringRow(cells));
    }

    /**
     * Insert row to specified position
     * <br>
     * If {@code position} is greater than or equal to {@link #size()},
     * empty rows will be inserted.
     *
     * @param position Position to insert cell value
     * @param cells    Cells
     * @return Previous value or {@code null} if empty rows were inserted
     */
    public StringRow insert(int position, Iterable<Object> cells) {
        return insert(position, new StringRow(cells));
    }

    /**
     * Insert row to specified position
     * <br>
     * If {@code position} is greater than or equal to {@link #size()},
     * empty rows will be inserted.
     *
     * @param position Position to insert cell value
     * @param row      Row
     * @return Previous value or {@code null} if empty rows were inserted
     */
    public StringRow insert(int position, StringRow row) {
        if (position < 0) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(row);
        StringRow previousValue = null;
        if (!insertEmptyRowsIfNeeded(position)) {
            previousValue = mRows.get(position);
        }
        mRows.add(position, row);
        return previousValue;
    }

    /**
     * Set or add row to specified position
     * <br>
     * If {@code position} is greater than or equal to {@link #size()},
     * empty rows will be inserted.
     *
     * @param position Position to set row
     * @param cells    Cells
     * @return Previous value or {@code null}
     */
    public StringRow set(int position, Object... cells) {
        return set(position, new StringRow(cells));
    }

    /**
     * Set or add row to specified position
     * <br>
     * If {@code position} is greater than or equal to {@link #size()},
     * empty rows will be inserted.
     *
     * @param position Position to set row
     * @param cells    Cells
     * @return Previous value or {@code null}
     */
    public StringRow set(int position, Iterable<Object> cells) {
        return set(position, new StringRow(cells));
    }

    /**
     * Set or add row to specified position
     * <br>
     * If {@code position} is greater than or equal to {@link #size()},
     * empty rows will be inserted.
     *
     * @param position Position to set row
     * @param row      Row
     * @return Previous value or {@code null}
     */
    public StringRow set(int position, StringRow row) {
        if (position < 0) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(row);
        if (insertEmptyRowsIfNeeded(position)) {
            mRows.add(row);
            return null;
        } else {
            return mRows.set(position, row);
        }
    }

    /**
     * Remove row from table
     *
     * @param index Row index
     * @return Removed row
     */
    public StringRow remove(int index) {
        return mRows.remove(index);
    }

    /**
     * Size of table
     *
     * @return Rows count
     */
    public int size() {
        return mRows.size();
    }

    /**
     * Whether if this {@link StringTable} has no rows
     */
    public boolean isEmpty() {
        return mRows.isEmpty();
    }

    /**
     * Clear table completely
     */
    public void clear() {
        clearCells();
        clearRows();
    }

    /**
     * Delete all cells from table (leave all rows empty)
     */
    public void clearCells() {
        for (StringRow row : mRows) {
            row.clear();
        }
    }

    /**
     * Delete all rows from table (don't touch cells)
     */
    public void clearRows() {
        mRows.clear();
    }

    @Override
    public boolean equals(Object o) {
        return o == this ||
                o instanceof StringTable && Objects.equals(((StringTable) o).mRows, mRows);
    }

    @Override
    public int hashCode() {
        int hashCode = Integer.MAX_VALUE;
        for (StringRow row : mRows) {
            hashCode ^= row.hashCode();
        }
        return hashCode;
    }

    @Override
    public String toString() {
        if (mRows.isEmpty()) {
            return "StringTable []";
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("StringTable [");
            for (StringRow row : mRows) {
                stringBuilder.append(System.lineSeparator()).append(row);
            }
            return stringBuilder.append(']').toString();
        }
    }

    private boolean insertEmptyRowsIfNeeded(int position) {
        int size = mRows.size();
        if (position > size) {
            int empty = position - size;
            for (int i = 0; i < empty; i++) {
                mRows.add(new StringRow());
            }
            return true;
        } else {
            return false;
        }
    }
}
