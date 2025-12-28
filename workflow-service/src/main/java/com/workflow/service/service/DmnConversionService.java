package com.workflow.service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DmnConversionService {

    public String convertCsvToDmnXml(String definitionKey, String decisionName, InputStream csvContent) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvContent, StandardCharsets.UTF_8))) {
            List<String> lines = reader.lines().filter(line -> !line.trim().isEmpty()).collect(Collectors.toList());

            if (lines.isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            // Parse Header
            String headerLine = lines.get(0);
            String[] headers = headerLine.split(",");
            List<DmnColumn> inputs = new ArrayList<>();
            List<DmnColumn> outputs = new ArrayList<>();

            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].trim();
                if (header.toUpperCase().startsWith("IN:")) {
                    String varName = header.substring(3).trim();
                    inputs.add(new DmnColumn(i, varName, "string")); // Default to string, infer later or assume
                } else if (header.toUpperCase().startsWith("OUT:")) {
                    String varName = header.substring(4).trim();
                    outputs.add(new DmnColumn(i, varName, "string"));
                } else {
                    throw new IllegalArgumentException(
                            "Invalid header format: " + header + ". Must start with IN: or OUT:");
                }
            }

            if (inputs.isEmpty() || outputs.isEmpty()) {
                throw new IllegalArgumentException("CSV must contain at least one IN: and one OUT: column");
            }

            // Build XML
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<definitions xmlns=\"http://www.omg.org/spec/DMN/20180521/MODEL/\" ");
            xml.append("xmlns:flowable=\"http://flowable.org/dmn\" ");
            xml.append("id=\"definition_").append(definitionKey).append("\" ");
            xml.append("name=\"").append(decisionName).append("\" ");
            xml.append("namespace=\"http://www.flowable.org/dmn\">\n");

            xml.append("  <decision id=\"").append(definitionKey).append("\" name=\"").append(decisionName)
                    .append("\">\n");
            xml.append("    <decisionTable id=\"decisionTable_").append(definitionKey)
                    .append("\" hitPolicy=\"FIRST\">\n");

            // Input Clauses
            for (DmnColumn input : inputs) {
                xml.append("      <input id=\"input_").append(input.index).append("\" label=\"").append(input.variable)
                        .append("\">\n");
                xml.append("        <inputExpression id=\"inputExpression_").append(input.index).append("\" typeRef=\"")
                        .append(input.type).append("\">\n");
                xml.append("          <text>").append(input.variable).append("</text>\n");
                xml.append("        </inputExpression>\n");
                xml.append("      </input>\n");
            }

            // Output Clauses
            for (DmnColumn output : outputs) {
                xml.append("      <output id=\"output_").append(output.index).append("\" label=\"")
                        .append(output.variable).append("\" name=\"").append(output.variable).append("\" typeRef=\"")
                        .append(output.type).append("\" />\n");
            }

            // Rules
            for (int r = 1; r < lines.size(); r++) {
                String line = lines.get(r);
                // Simple CSV Split (Does not handle quoted commas for simplicity, can enhance
                // later)
                String[] values = line.split(",", -1);

                xml.append("      <rule>\n");

                // Input Entries
                for (DmnColumn input : inputs) {
                    String val = (input.index < values.length) ? values[input.index].trim() : "";
                    if (val.isEmpty() || val.equalsIgnoreCase("ANY")) {
                        // Empty implies match anything, simpler DMN syntax is often "-" or empty
                        val = ""; // DMN 1.1 uses empty for any? Or "-"
                    }
                    xml.append("        <inputEntry id=\"rule_").append(r).append("_input_").append(input.index)
                            .append("\">\n");
                    xml.append("          <text><![CDATA[").append(val).append("]]></text>\n");
                    xml.append("        </inputEntry>\n");
                }

                // Output Entries
                for (DmnColumn output : outputs) {
                    String val = (output.index < values.length) ? values[output.index].trim() : "";
                    // Quote strings if needed? For now assume raw value
                    if (!isNumeric(val) && !val.equalsIgnoreCase("true") && !val.equalsIgnoreCase("false")
                            && !val.startsWith("\"")) {
                        val = "\"" + val + "\"";
                    }
                    xml.append("        <outputEntry id=\"rule_").append(r).append("_output_").append(output.index)
                            .append("\">\n");
                    xml.append("          <text><![CDATA[").append(val).append("]]></text>\n");
                    xml.append("        </outputEntry>\n");
                }

                xml.append("      </rule>\n");
            }

            xml.append("    </decisionTable>\n");
            xml.append("  </decision>\n");
            xml.append("</definitions>");

            return xml.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert CSV to DMN", e);
        }
    }

    private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    private static class DmnColumn {
        int index;
        String variable;
        String type;

        DmnColumn(int index, String variable, String type) {
            this.index = index;
            this.variable = variable;
            this.type = type;
        }
    }
}
