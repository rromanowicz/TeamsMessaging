package ex.rr.teamsmessaging.adaptivecard;

import ex.rr.teamsmessaging.adaptivecard.annotations.AdaptiveCard;
import ex.rr.teamsmessaging.adaptivecard.annotations.CardField;
import ex.rr.teamsmessaging.adaptivecard.annotations.CardIgnore;
import ex.rr.teamsmessaging.adaptivecard.enums.Style;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static ex.rr.teamsmessaging.adaptivecard.constants.Replacements.*;
import static ex.rr.teamsmessaging.adaptivecard.constants.Templates.*;

@Slf4j
@Component
@SuppressWarnings("unused")
public class AdaptiveTemplate {

    private final Object obj;
    private final Map<String, String> facts;
    private final Style style;
    private String title;

    private final Boolean wrapMessage;


    private AdaptiveTemplate(Object obj, Map<String, String> facts, Style style, Boolean wrapMessage) {
        this.obj = obj;
        this.facts = facts;
        this.style = style;
        this.wrapMessage = wrapMessage;
    }

    public String apply() {
        String processObject = processObject(obj);
        if (processObject.isBlank()) {
            log.error("Cannot process element of type [{}].", obj.getClass().getSimpleName());
            throw new UnsupportedOperationException("Cannot process element.");
        }
        String output = T_CONTAINER.replace(R_FACTS, processFacts());
        output = output.replace(R_BODY, processObject);

        String adaptiveCard = T_ADAPTIVE_CARD.replace(R_CONTAINER, output);
        if (wrapMessage) {
            return T_MESSAGE.replace(R_VALUE, adaptiveCard);
        }
        return adaptiveCard;
    }

    private String processFacts() {
        if (facts != null && !facts.isEmpty()) {
            List<String> factList = facts.entrySet().stream().map(entry ->
                    T_FACT_ENTRY.replace(R_KEY, entry.getKey())
                            .replace(R_VALUE, entry.getValue())).toList();
            return T_FACTS.replace(R_FACTS, factList.toString())
                    .replace(R_STYLE, style.name);
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
            return T_TITLE.replace(R_VALUE, annotation.title());
        }
        return "";
    }

    private String processDefaultTemplate(Object obj) throws Exception {
        Map<String, Object> fieldMap = getFieldMap(obj);
        return processMap(fieldMap);
    }

    private String processMap(Map<String, Object> fieldMap) {
        List<String> dataRows = new ArrayList<>(fieldMap.size());

        String result = T_MAP_TABLE_TEMPLATE.replace(R_TITLE, title);

        fieldMap.forEach((k, v) -> {
            log.debug("Key - [{}]: [{}]\t | Value - [{}]: [{}]",
                    k.getClass().getSimpleName(), k, v.getClass().getSimpleName(), v);

            String dataRow = T_MAP_DATA_ROW;
            dataRow = dataRow.replace(R_KEY, T_DEFAULT_CELL_ITEM.replace(R_VALUE, k));

            String cellValue;

            if (v.getClass().getName().startsWith("java.lang")) {
                cellValue = T_DEFAULT_CELL_ITEM.replace(R_VALUE, v.toString());
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
                    throw new UnsupportedOperationException("I missed something. Please raise create a bug.");
                }
            } else {
                String nestedObject = processObject(v);
                if (nestedObject.isBlank()) return;
                cellValue = nestedObject;
            }

            dataRow = dataRow.replace(R_VALUE, cellValue);
            dataRows.add(dataRow);
        });
        return result.replace(R_ROWS, dataRows.toString());
    }

    private String processCollection(Collection<?> c) {
        List<String> cellRows = c.stream().map(it ->
                        T_LIST_DATA_ROW.replace(R_VALUE,
                                T_DEFAULT_CELL_ITEM.replace(R_VALUE, it.toString())))
                .toList();
        return T_LIST_TABLE_TEMPLATE.replace(R_ROWS, cellRows.toString());
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
        private Style style = Style.DEFAULT;

        private Boolean wrapMessage = false;

        public Builder object(Object obj) {
            this.obj = obj;
            return this;
        }

        public Builder facts(Map<String, String> facts) {
            this.facts = facts;
            return this;
        }

        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        public Builder addWrapper() {
            this.wrapMessage = true;
            return this;
        }

        public AdaptiveTemplate build() {
            return new AdaptiveTemplate(obj, facts, style, wrapMessage);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

}
