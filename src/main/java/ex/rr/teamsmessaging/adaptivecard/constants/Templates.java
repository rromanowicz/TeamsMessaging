package ex.rr.teamsmessaging.adaptivecard.constants;

public class Templates {

    public static final String T_MESSAGE = """
            { "type":"message", "attachments":[ { "contentType":"application/vnd.microsoft.card.adaptive", "contentUrl":null, "content": ${VALUE} } ] }
            """;
    public static final String T_ADAPTIVE_CARD = """
            { "type": "AdaptiveCard", "$schema": "http://adaptivecards.io/schemas/adaptive-card.json", "version": "1.5", "body": [ ${CONTAINER} ] }""";

    public static final String T_CONTAINER = """
            { "type": "Container","items": [ ${FACTS} ${BODY} ] }""";

    public static final String T_FACTS = """
            { "type": "Container", "style": "${STYLE}", "bleed": true, "items": [ { "type":"FactSet", "facts": ${FACTS} } ] },""";

    public static final String T_FACT_ENTRY = """
            { "title":"${KEY}", "value":"${VALUE}" }""";
    public static final String T_TITLE = """
            { "type": "RichTextBlock", "inlines": [ { "type": "TextRun", "text": "${VALUE}" } ] },""";

    public static final String T_MAP_TABLE_TEMPLATE = """
            ${TITLE} { "type":"Table", "firstRowAsHeaders": false, "columns":[ {"width":2}, {"width":4} ], "rows":${ROWS} }""";

    public static final String T_LIST_TABLE_TEMPLATE = """
            { "type":"Table", "firstRowAsHeaders": false, "columns":[ {"width":1} ], "rows":${ROWS} }""";

    public static final String T_MAP_DATA_ROW = """
            { "type":"TableRow", "cells": [ { "type":"TableCell", "items": [ ${KEY} ] }, { "type":"TableCell", "items": [ ${VALUE} ] } ] }""";

    public static final String T_LIST_DATA_ROW = """
            { "type":"TableRow", "cells": [ { "type":"TableCell", "items": [ ${VALUE} ] } ] }""";

    public static final String T_DEFAULT_CELL_ITEM = """
            { "text": "${VALUE}", "type": "TextBlock", "wrap": false }""";

}
