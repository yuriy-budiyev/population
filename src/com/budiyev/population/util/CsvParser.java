/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2016 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.budiyev.population.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;

public final class CsvParser {
    private static final String QUOTE_STRING = "\"";
    private static final String DOUBLE_QUOTE_STRING = "\"\"";
    private static final char QUOTE = '\"';
    private static final char CR = '\r';
    private static final char LF = '\n';

    private CsvParser() {
    }

    private static boolean encode(double[][] values, String[] headers, OutputStream outputStream,
            char separator, String charset) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(outputStream, charset))) {
            for (double[] row : values) {
                int size = row.length;
                for (int i = 0; i < size; i++) {
                    writer.append(QUOTE).append(String.valueOf(row[i])).append(QUOTE);
                    if (i != size - 1) {
                        writer.append(separator);
                    }
                }
                writer.append(LF);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean encode(Table table, OutputStream outputStream, char separator,
            String charset) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(outputStream, charset))) {
            for (Row row : table) {
                int size = row.size();
                for (int i = 0; i < size; i++) {
                    writer.append(QUOTE)
                            .append(row.cell(i).replace(QUOTE_STRING, DOUBLE_QUOTE_STRING))
                            .append(QUOTE);
                    if (i != size - 1) {
                        writer.append(separator);
                    }
                }
                writer.append(LF);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String encode(Table table, char separator) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Row row : table) {
            int size = row.size();
            for (int i = 0; i < size; i++) {
                stringBuilder.append(QUOTE)
                        .append(row.cell(i).replace(QUOTE_STRING, DOUBLE_QUOTE_STRING))
                        .append(QUOTE);
                if (i != size - 1) {
                    stringBuilder.append(separator);
                }
            }
            stringBuilder.append(LF);
        }
        return stringBuilder.toString();
    }

    public static Table parse(InputStream inputStream, char separator, String charset) {
        return new Table(inputStream, separator, charset);
    }

    public static Table parse(String string, char separator) {
        return new Table(string, separator);
    }

    public static class Table implements Iterable<Row> {
        private final ArrayList<Row> mRows = new ArrayList<>();

        private Table(String table, char separator) {
            StringBuilder row = new StringBuilder();
            boolean inQuotes = false;
            int length = table.length();
            for (int i = 0; i < length; i++) {
                char current = table.charAt(i);
                if (current == LF && !inQuotes) {
                    mRows.add(new Row(row.toString(), separator));
                    row.delete(0, row.length());
                } else {
                    if (current == QUOTE) {
                        inQuotes = !inQuotes;
                    }
                    row.append(current);
                }
            }
            if (row.length() != 0) {
                mRows.add(new Row(row.toString(), separator));
            }
        }

        private Table(InputStream table, char separator, String charset) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(table, charset))) {
                StringBuilder row = new StringBuilder();
                boolean inQuotes = false;
                for (; ; ) {
                    int c = reader.read();
                    char current;
                    if (c == -1) {
                        break;
                    } else {
                        current = (char) c;
                    }
                    if (current == LF && !inQuotes) {
                        mRows.add(new Row(row.toString(), separator));
                        row.delete(0, row.length());
                    } else {
                        if (current == QUOTE) {
                            inQuotes = !inQuotes;
                        }
                        row.append(current);
                    }
                }
                if (row.length() != 0) {
                    mRows.add(new Row(row.toString(), separator));
                }
            } catch (IOException ignored) {
            }
        }

        public Table() {
        }

        public Table(int rows, int columns) {
            for (int i = 0; i < rows; i++) {
                add(new Row(columns));
            }
        }

        @Override
        public Iterator<Row> iterator() {
            return mRows.iterator();
        }

        /**
         * Row
         *
         * @param index Row position in table
         * @return Row at index
         */
        public Row row(int index) {
            return mRows.get(index);
        }

        /**
         * Add empty row
         */
        public void add() {
            mRows.add(new Row());
        }

        /**
         * Add row to table
         *
         * @param row Row
         */
        public void add(Row row) {
            mRows.add(row);
        }

        /**
         * Remove row from table
         *
         * @param index Row index
         * @return Removed row
         */
        public Row remove(int index) {
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
    }

    public static class Row implements Iterable<String> {
        private final ArrayList<String> mCells = new ArrayList<>();

        private Row(String row, char separator) {
            StringBuilder cell = new StringBuilder();
            boolean inQuotes = false;
            boolean inElementQuotes = false;
            int length = row.length();
            for (int i = 0; i < length; i++) {
                char current = row.charAt(i);
                if (current == separator && !inElementQuotes) {
                    mCells.add(cell.toString());
                    cell.delete(0, cell.length());
                } else if (current == QUOTE) {
                    int n = i + 1;
                    int p = i - 1;
                    if ((p > -1 && row.charAt(p) == separator || i == 0) && !inElementQuotes) {
                        inElementQuotes = true;
                    } else if ((n < length && row.charAt(n) == separator || n == length) &&
                               inElementQuotes) {
                        inElementQuotes = false;
                    } else if (n < length && row.charAt(n) == QUOTE) {
                        cell.append(current);
                        i++;
                    } else {
                        inQuotes = !inQuotes;
                    }
                } else {
                    cell.append(current);
                }
            }
            mCells.add(cell.toString());
        }

        public Row() {
        }

        public Row(int cells) {
            for (int i = 0; i < cells; i++) {
                add();
            }
        }

        public Row(Object... cells) {
            for (Object cell : cells) {
                add(String.valueOf(cell));
            }
        }

        @Override
        public Iterator<String> iterator() {
            return mCells.iterator();
        }

        /**
         * Column
         *
         * @param index Column position in row
         * @return Column at index
         */
        public String cell(int index) {
            return mCells.get(index);
        }

        /**
         * Add cell with null value to row
         */
        public void add() {
            mCells.add(null);
        }

        /**
         * Add cell to row
         *
         * @param cell Cell value
         */
        public void add(String cell) {
            mCells.add(cell);
        }

        /**
         * Remove cell from row
         *
         * @param index Column index
         * @return Removed cell value
         */
        public String remove(int index) {
            return mCells.remove(index);
        }

        /**
         * Size of row
         *
         * @return Columns count
         */
        public int size() {
            return mCells.size();
        }
    }
}
