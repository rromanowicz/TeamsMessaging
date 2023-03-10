package ex.rr.teamsmessaging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;


@Slf4j
@RequiredArgsConstructor
@Component
public class JsonReader {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
    }


    public static final String COULD_NOT_LOAD_RESOURCE_FILE = "Could not load resource file!";
    public static final String COULD_NOT_LOAD_INPUT_STRING = "Could not load input string!";

    public <T> T loadInputString(String input, Class<T> type) {
        try {
            return objectMapper.readValue(input, type);
        } catch (IOException e) {
            log.error(COULD_NOT_LOAD_INPUT_STRING + " [{}]", e.getMessage());
            throw new JsonReaderException(COULD_NOT_LOAD_INPUT_STRING);
        }
    }

    public <T> T loadInputFile(String fileName, Class<T> type) {
        ClassLoader classLoader = JsonReader.class.getClassLoader();
        try {
            InputStream resourceAsStream = classLoader.getResourceAsStream(fileName);
            String file = readFileContentFromResource(resourceAsStream);
            return objectMapper.readValue(file, type);
        } catch (IOException e) {
            log.error(COULD_NOT_LOAD_RESOURCE_FILE + " [{}]", e.getMessage());
            throw new JsonReaderException(COULD_NOT_LOAD_RESOURCE_FILE);
        }
    }

    public static String asJsonString(final Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonReaderException(e.getMessage());
        }
    }

    public static JsonNode populateJson(JsonNode jsonNode, Map<String, String> variables) throws JsonProcessingException {
        String json = asJsonString(jsonNode);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            json = json.replace(entry.getKey(), entry.getValue());
        }
        return asJson(json);
    }

    public static JsonNode populateMapEntry(JsonNode jsonNode, Map.Entry<String, String> entry) throws JsonProcessingException {
        String json = asJsonString(jsonNode);
        json = json.replace("{{KEY}}", entry.getKey());
        json = json.replace("{{VALUE}}", entry.getValue());
        return asJson(json);
    }


    public static String populateJson(String json, Map<String, String> variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            json = json.replace(entry.getKey(), entry.getValue());
            json = json.replace(entry.getKey(), entry.getValue());
        }
        return json;
    }

    public static JsonNode asJson(String json) throws JsonProcessingException {
        return objectMapper.readTree(json);
    }

    private String readFileContentFromResource(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }


    private static class JsonReaderException extends RuntimeException {
        public JsonReaderException(String message) {
            super(message);
        }
    }

}
