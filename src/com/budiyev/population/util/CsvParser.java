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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Parser of CSV format
 */
public final class CsvParser {
    private static final String QUOTE_STRING = "\"";
    private static final String DOUBLE_QUOTE_STRING = "\"\"";
    private static final char QUOTE = '\"';
    private static final char LF = '\n';
    private static final int BUFFER_SIZE = 8192;

    private CsvParser() {
    }

    /**
     * Encode {@link StringTable} in CSV format
     *
     * @param table        Table
     * @param outputStream Stream to save result
     * @param separator    Column separator
     * @param charset      Charset name
     * @return true if success, false otherwise
     */
    public static boolean encode(StringTable table, OutputStream outputStream, char separator, String charset) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, charset))) {
            for (StringRow row : table) {
                int size = row.size();
                for (int i = 0; i < size; i++) {
                    writer.append(QUOTE).append(row.cell(i).replace(QUOTE_STRING, DOUBLE_QUOTE_STRING)).append(QUOTE);
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

    /**
     * Encode {@link StringTable} in CSV format
     *
     * @param table     Table
     * @param separator Column separator
     * @return Encoded string
     */
    public static String encode(StringTable table, char separator) {
        StringBuilder stringBuilder = new StringBuilder();
        for (StringRow row : table) {
            int size = row.size();
            for (int i = 0; i < size; i++) {
                stringBuilder.append(QUOTE).append(row.cell(i).replace(QUOTE_STRING, DOUBLE_QUOTE_STRING))
                        .append(QUOTE);
                if (i != size - 1) {
                    stringBuilder.append(separator);
                }
            }
            stringBuilder.append(LF);
        }
        return stringBuilder.toString();
    }

    /**
     * Parse CSV into {@link StringTable}
     *
     * @param inputStream Source data stream
     * @param separator   Column separator
     * @param charset     Charset name
     * @return Table
     */
    public static StringTable parse(InputStream inputStream, char separator, String charset) {
        try (InputStreamReader reader = new InputStreamReader(inputStream, charset)) {
            StringTable table = new StringTable();
            StringBuilder row = new StringBuilder();
            boolean inQuotes = false;
            char[] buffer = new char[BUFFER_SIZE];
            for (; ; ) {
                int read = reader.read(buffer);
                if (read == -1) {
                    if (row.length() > 0) {
                        table.add(parseRow(row.toString(), separator));
                    }
                    break;
                }
                for (int i = 0; i < read; i++) {
                    if (buffer[i] == CsvParser.LF && !inQuotes) {
                        table.add(parseRow(row.toString(), separator));
                        row.delete(0, row.length());
                    } else {
                        if (buffer[i] == CsvParser.QUOTE) {
                            inQuotes = !inQuotes;
                        }
                        row.append(buffer[i]);
                    }
                }
            }
            return table;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Parse CSV into {@link StringTable}
     *
     * @param string    Source string
     * @param separator Column separator
     * @return Table
     */
    public static StringTable parse(String string, char separator) {
        StringTable table = new StringTable();
        StringBuilder row = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0, l = string.length(); i < l; i++) {
            char current = string.charAt(i);
            if (current == CsvParser.LF && !inQuotes) {
                table.add(parseRow(row.toString(), separator));
                row.delete(0, row.length());
            } else {
                if (current == CsvParser.QUOTE) {
                    inQuotes = !inQuotes;
                }
                row.append(current);
            }
        }
        if (row.length() > 0) {
            table.add(parseRow(row.toString(), separator));
        }
        return table;
    }

    private static StringRow parseRow(String rowString, char separator) {
        StringRow row = new StringRow();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        boolean inElementQuotes = false;
        int length = rowString.length();
        for (int i = 0; i < length; i++) {
            char current = rowString.charAt(i);
            if (current == separator && !inElementQuotes) {
                row.add(cell.toString());
                cell.delete(0, cell.length());
            } else if (current == CsvParser.QUOTE) {
                int n = i + 1;
                int p = i - 1;
                if ((p > -1 && rowString.charAt(p) == separator || i == 0) && !inElementQuotes) {
                    inElementQuotes = true;
                } else if ((n < length && rowString.charAt(n) == separator || n == length) && inElementQuotes) {
                    inElementQuotes = false;
                } else if (n < length && rowString.charAt(n) == CsvParser.QUOTE) {
                    cell.append(current);
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else {
                cell.append(current);
            }
        }
        row.add(cell.toString());
        return row;
    }
}
