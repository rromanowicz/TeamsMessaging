package ex.rr.teamsmessaging.adaptivecard;

import ex.rr.teamsmessaging.adaptivecard.annotations.AdaptiveCard;
import ex.rr.teamsmessaging.adaptivecard.annotations.CardField;
import ex.rr.teamsmessaging.adaptivecard.annotations.CardIgnore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AdaptiveTemplate {

    private Object obj;
    private Map<String, String> facts;
    private String title;


    private AdaptiveTemplate(Object obj, Map<String, String> facts) {
        this.obj = obj;
        this.facts = facts;
    }


    public static final String KEY = "${KEY}";
    public static final String VALUE = "${VALUE}";
    public static final String ROWS = "${ROWS}";

    private final String ADAPTIVE_CARD = """
            { "type": "AdaptiveCard", "$schema": "http://adaptivecards.io/schemas/adaptive-card.json", "version": "1.5", "body": [ ${CONTAINER} ] }""";

    private final String CONTAINER = """
            { "type": "Container","items": [ ${FACTS} ${BODY} ] }""";

    private final String FACTS = """
            { "type":"FactSet", "facts": ${FACTS} },""";

    private final String FACT_ENTRY = """
            { "title":"${KEY}", "value":"${VALUE}" }""";
    private final String TITLE = """
            { "type": "RichTextBlock", "inlines": [ { "type": "TextRun", "text": "${VALUE}" } ] },""";

    private final String MAP_TABLE_TEMPLATE = """
            ${TITLE} { "type":"Table", "firstRowAsHeaders": false, "columns":[ {"width":20}, {"width":30} ], "rows":${ROWS} }""";

    private final String LIST_TABLE_TEMPLATE = """
            { "type":"Table", "firstRowAsHeaders": false, "columns":[ {"width":1} ], "rows":${ROWS} }""";

    private final String MAP_DATA_ROW = """
            { "type":"TableRow", "cells": [ { "type":"TableCell", "items": [ ${KEY} ] }, { "type":"TableCell", "items": [ ${VALUE} ] } ] }""";

    private final String LIST_DATA_ROW = """
            { "type":"TableRow", "cells": [ { "type":"TableCell", "items": [ ${VALUE} ] } ] }""";

    private final String DEFAULT_CELL_ITEM = """
            { "text": "${VALUE}", "type": "TextBlock", "wrap": false }""";


    public String apply() {
        String output = CONTAINER.replace("${FACTS}", processFacts());
        output = output.replace("${BODY}", processObject(obj));

        return ADAPTIVE_CARD.replace("${CONTAINER}", output);
    }

    private String processFacts() {
        if (facts != null && !facts.isEmpty()) {
            List<String> factList = facts.entrySet().stream().map(entry ->
                    FACT_ENTRY.replace(KEY, entry.getKey())
                            .replace(VALUE, entry.getValue())).toList();
            return FACTS.replace("${FACTS}", factList.toString());
        }
        return "";
    }

    private String processObject(Object obj) {
        if (obj.getClass().isAnnotationPresent(AdaptiveCard.class)) {
            AdaptiveCard annotation = obj.getClass().getAnnotation(AdaptiveCard.class);
            title = processTitle(annotation);
            try {
                return switch (annotation.type()) {
                    case DEFAULT -> processDefaultTemplate(obj);
                    case CUSTOM -> processCustomTemplate(obj, annotation.template());
                };
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return "";
    }

    private String processTitle(AdaptiveCard annotation) {
        if (!annotation.title().isBlank()) {
            return TITLE.replace(VALUE, annotation.title());
        }
        return "";
    }

    private String processDefaultTemplate(Object obj) throws Exception {
        Map<String, Object> fieldMap = getFieldMap(obj);
        return processMap(fieldMap);
    }

    private String processMap(Map<String, Object> fieldMap) {
        List<String> dataRows = new ArrayList<>(fieldMap.size());

        String result = MAP_TABLE_TEMPLATE.replace("${TITLE}", title);

        fieldMap.forEach((k, v) -> {
            log.debug("Key - [{}]: [{}]\t | Value - [{}]: [{}]", k.getClass().getSimpleName(), k, v.getClass().getSimpleName(), v);

            String dataRow = MAP_DATA_ROW;
            dataRow = dataRow.replace(KEY, DEFAULT_CELL_ITEM.replace(VALUE, k));

            String cellValue = "";

            if (v.getClass().getName().startsWith("java.lang")) {
                cellValue = DEFAULT_CELL_ITEM.replace(VALUE, v.toString());
            } else if (v.getClass().getName().startsWith("java.util")) {
                if (v instanceof Map<?, ?> m) {
                    Map<String, Object> collect = m.entrySet().stream()
                            .collect(Collectors.toMap(
                                    e -> e.getKey().toString(),
                                    e -> (Object) e.getValue()));
                    cellValue = processMap(collect);
                } else if (v instanceof Collection<?> c) {
                    cellValue = processCollection(c);
                } else {
                }
            } else {
                cellValue = processObject(v);
            }

            dataRow = dataRow.replace(VALUE, cellValue);
            dataRows.add(dataRow);
        });
        return result.replace(ROWS, dataRows.toString());
    }

    private String processCollection(Collection<?> c) {
        List<String> cellRows = c.stream().map(it ->
                        LIST_DATA_ROW.replace(VALUE,
                                DEFAULT_CELL_ITEM.replace(VALUE, it.toString())))
                .toList();
        return LIST_TABLE_TEMPLATE.replace(ROWS, cellRows.toString());
    }

    private String processCustomTemplate(Object obj, String template) {
        log.error("Not yet. Work in progress. [{}]: [{}]", obj, template);
        return null;
    }

    private static Map<String, Object> getFieldMap(Object obj) throws IllegalAccessException {
        Class<?> clazz = obj.getClass();
        Map<String, Object> fieldMap = new HashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(CardIgnore.class)) {
                fieldMap.put(
                        field.isAnnotationPresent(CardField.class) ?
                                field.getAnnotation(CardField.class).name() : field.getName(),
                        field.get(obj));
            }
        }
        return fieldMap;
    }

    public static class Builder {
        private Map<String, String> facts;
        private Object obj;

        public Builder object(Object obj) {
            this.obj = obj;
            return this;
        }

        public Builder facts(Map<String, String> facts) {
            this.facts = facts;
            return this;
        }

        public AdaptiveTemplate build() {
            return new AdaptiveTemplate(obj, facts);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

}
