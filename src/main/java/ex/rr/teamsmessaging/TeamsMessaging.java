package ex.rr.teamsmessaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ex.rr.teamsmessaging.JsonReader.*;

@Slf4j
@Service
public class TeamsMessaging {

    private static final Configuration JSON_PATH_CONFIGURATION = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build();

    public static final String DATA_FACT_PATH = "$.attachments[0].content.body[0].items[0].facts";
    public static final String DATA_ROW_PATH = "$.attachments[0].content.body[0].items[1].rows";
    public static final String CARD_PATH = "$.attachments[0].content.body[0]";
    public static final String STYLE_PATH = "$.attachments[0].content.body[0].style";

    private final ParseContext jsonContext = JsonPath.using(JSON_PATH_CONFIGURATION);


    @Value("${teams.webhook.connector.url}")
    private String teamsconnectorUrl;
    @Value("${spring.application.name}")
    private String appName;
    @Value("${spring.profiles.active}")
    private String env;

    @Autowired
    private TeamsPosting teamsPosting;


    private static final String MESSAGE_TEMPLATE_ADAPTIVE = """
            {
               "type":"message",
               "attachments":[
                  {
                     "contentType":"application/vnd.microsoft.card.adaptive",
                     "contentUrl":null,
                     "content":{
                        "$schema":"http://adaptivecards.io/schemas/adaptive-card.json",
                        "type":"AdaptiveCard",
                        "version":"1.2",
                        "body":[
                           {
                              "type":"Container",
                              "style": "default",
                              "items":[
                                 {
                                    "type":"FactSet",
                                    "facts":[
                                       { "title":"{{KEY}}", "value":"{{VALUE}}" }
                                    ]
                                 },
                                 {
                                    "type":"Table",
                                    "firstRowAsHeaders": false,
                                    "columns":[
                                       {
                                          "width":20
                                       },
                                       {
                                          "width":30
                                       }
                                    ],
                                    "rows":[
                                       {
                                          "type":"TableRow",
                                          "cells": [
                                              { "type":"TableCell", "items": [ { "text": "{{KEY}}", "type": "TextBlock", "wrap": false } ] },
                                              { "type":"TableCell", "items": [ { "text": "{{VALUE}}", "type": "TextBlock", "wrap": false } ] }
                                          ]
                                       }
                                    ]
                                 }
                              ]
                           }
                        ]
                     }
                  }
               ]
            }
            """;


    private List<JsonNode> populateDataRows(Map<String, String> valueMap) throws JsonProcessingException {
        JsonNode read = jsonContext.parse(MESSAGE_TEMPLATE_ADAPTIVE).read(DATA_ROW_PATH + "[0]", JsonNode.class);

        List<JsonNode> nodeList = new ArrayList<>();
        for (Map.Entry<String, String> entry : valueMap.entrySet()) {
            nodeList.add(populateMapEntry(read, entry));
        }

        return nodeList;
    }

    private List<JsonNode> populateFacts(Map<String, String> factMap) throws JsonProcessingException {
        JsonNode read = jsonContext.parse(MESSAGE_TEMPLATE_ADAPTIVE).read(DATA_FACT_PATH + "[0]", JsonNode.class);

        List<JsonNode> nodeList = new ArrayList<>();
        for (Map.Entry<String, String> entry : factMap.entrySet()) {
            nodeList.add(populateMapEntry(read, entry));
        }

        return nodeList;
    }

    private JsonNode populateMapEntry(JsonNode jsonNode, Map.Entry<String, String> entry) throws JsonProcessingException {
        String json = asJsonString(jsonNode);
        json = json.replace("{{KEY}}", entry.getKey());
        json = json.replace("{{VALUE}}", entry.getValue());
        return asJson(json);
    }


    public String buildDefaultMessage(Map<String, String> dataMap) throws JsonProcessingException {
        DocumentContext context = jsonContext.parse(MESSAGE_TEMPLATE_ADAPTIVE);
        context.set(DATA_FACT_PATH, populateFacts(Map.of("App", appName, "Env", env)));
        context.set(DATA_ROW_PATH, populateDataRows(dataMap));
        context.set(STYLE_PATH, ThemeColor.valueOf(env.toUpperCase()).color);

        return asJsonString(context.json());
    }

    public String buildMessage(Map<String, String> factMap, Map<String, String> dataMap) throws JsonProcessingException {
        DocumentContext context = jsonContext.parse(MESSAGE_TEMPLATE_ADAPTIVE);
        context.set(DATA_FACT_PATH, populateFacts(factMap));
        context.set(DATA_ROW_PATH, populateDataRows(dataMap));

        return asJsonString(context.json());
    }

    public String buildMessageFromJsonString(String adaptiveCardJson, Map<String, String> dataMap) throws JsonProcessingException {

        DocumentContext context = jsonContext.parse(MESSAGE_TEMPLATE_ADAPTIVE);
        String s = populateJson(adaptiveCardJson, dataMap);
        context.set(CARD_PATH, asJson(s));
        return asJsonString(context.json());
    }


    public void postTeamsMessage(String message) {
        teamsPosting.publishMessage(message);
    }


    private enum ThemeColor {
        LOCAL("emphasis"),
        DEV("good"),
        SIT("accent"),
        SAT("warning"),
        PROD("attention");

        private final String color;

        ThemeColor(String s) {
            this.color = s;
        }
    }
}

class TeamsMessageException extends RuntimeException {

    private final HttpStatus status;

    public TeamsMessageException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public TeamsMessageException(String message, HttpStatus httpStatus) {
        super(message);
        this.status = httpStatus;
    }

    public HttpStatus getStatus() {
        return this.status;
    }
}




interface TeamsPosting {
    void publishMessage(String message);
}

@Slf4j
@Component
@ConditionalOnBean(RestTemplate.class)
class RestTemplateTeamsPosting implements TeamsPosting {

    @Value("${teams.webhook.connector.url}")
    private String teamsconnectorUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void publishMessage(String message) {
        log.debug("Sending message [{}].", message);
        try {
            restTemplate.postForObject(teamsconnectorUrl, message, String.class);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new TeamsMessageException("Error while sending the message.");
        }
    }
}

@Slf4j
@Component
@ConditionalOnBean(WebClient.class)
class WebClientTeamsPosting implements TeamsPosting {

    @Value("${teams.webhook.connector.url}")
    private String teamsconnectorUrl;

    @Autowired
    private WebClient webClient;

    @Override
    public void publishMessage(String message) {
        log.debug("Sending message [{}].", message);
        try {
            webClient.post().uri(teamsconnectorUrl).body(message, String.class).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(String.class);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new TeamsMessageException("Error while sending the message.");
        }
    }
}