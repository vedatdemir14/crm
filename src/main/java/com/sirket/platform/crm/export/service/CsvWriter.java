package com.sirket.platform.crm.export.service;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Minimal RFC 4180 CSV writer with two Excel-specific concessions.
 * <p>
 * <strong>Byte order mark.</strong> Excel on Windows assumes the system code page for a CSV without
 * a BOM, which turns Turkish characters into mojibake ("Ayşe" reads as "AyÅŸe"). The BOM makes it
 * read the file as UTF-8.
 * <p>
 * <strong>Formula injection.</strong> Excel evaluates a cell whose text starts with =, +, - or @,
 * so an exported field could execute as a formula on the machine that opens it. Those values are
 * prefixed with an apostrophe, which Excel strips on display but does not evaluate.
 */
public class CsvWriter {

    private static final String BOM = "﻿";
    private static final String LINE_ENDING = "\r\n";

    private final Writer writer;
    private final char delimiter;

    public CsvWriter(Writer writer, char delimiter) throws IOException {
        this.writer = writer;
        this.delimiter = delimiter;
        writer.write(BOM);
    }

    public void writeRow(List<String> values) throws IOException {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                row.append(delimiter);
            }
            row.append(escape(values.get(i)));
        }
        row.append(LINE_ENDING);
        writer.write(row.toString());
    }

    public void flush() throws IOException {
        writer.flush();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String guarded = guardAgainstFormula(value);
        boolean needsQuoting = guarded.indexOf(delimiter) >= 0
                || guarded.indexOf('"') >= 0
                || guarded.indexOf('\n') >= 0
                || guarded.indexOf('\r') >= 0;
        return needsQuoting ? '"' + guarded.replace("\"", "\"\"") + '"' : guarded;
    }

    private String guardAgainstFormula(String value) {
        if (value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        return (first == '=' || first == '+' || first == '-' || first == '@') ? "'" + value : value;
    }
}
