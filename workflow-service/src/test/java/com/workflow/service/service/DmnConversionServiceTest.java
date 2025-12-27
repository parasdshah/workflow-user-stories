package com.workflow.service.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DmnConversionServiceTest {

    private final DmnConversionService conversionService = new DmnConversionService();

    @Test
    void testValidCsvConversion() {
        String csv = "IN:amount,IN:score,OUT:risk,OUT:autoApprove\n" +
                "< 1000, > 700, LOW, true\n" +
                ">= 1000, > 700, MEDIUM, false";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        String xml = conversionService.convertCsvToDmnXml("TEST_KEY", "Test Rule", inputStream);

        System.out.println(xml); // For debugging

        assertNotNull(xml);
        assertTrue(xml.contains("definitions"));
        assertTrue(xml.contains("decision id=\"TEST_KEY\""));

        // Check Inputs
        assertTrue(xml.contains("label=\"amount\""));
        assertTrue(xml.contains("label=\"score\""));

        // Check Outputs
        assertTrue(xml.contains("name=\"risk\""));
        assertTrue(xml.contains("name=\"autoApprove\""));

        // Check Rules
        assertTrue(xml.contains("<text><![CDATA[< 1000]]></text>"));
        assertTrue(xml.contains("<text><![CDATA[\"LOW\"]]></text>"));
        assertTrue(xml.contains("<text><![CDATA[true]]></text>"));
    }

    @Test
    void testInvalidHeader() {
        String csv = "amount,score,risk\n100,200,LOW";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        Assertions.assertThrows(RuntimeException.class, () -> {
            conversionService.convertCsvToDmnXml("KEY", "Name", inputStream);
        });
    }
}
