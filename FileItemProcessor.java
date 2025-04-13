 private final Resource resource;
    private final ObjectMapper objectMapper;
    private final JsonFactory jsonFactory;
    private BufferedReader reader;
    private JsonParser jsonParser;
    private JsonToken currentToken;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public StreamingJsonFileReader(Resource resource) {
        this.resource = resource;
        this.objectMapper = new ObjectMapper();
        this.jsonFactory = new JsonFactory();
        initializeReader();
    }

    private void initializeReader() {
        try {
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            jsonParser = jsonFactory.createParser(reader);
            currentToken = jsonParser.nextToken(); // Move to the first token
            if (currentToken != JsonToken.START_OBJECT) {
                throw new IOException("Expected JSON object but found: " + currentToken);
            }
            // Move to the "results" field
            while (jsonParser.nextToken() != JsonToken.FIELD_NAME || !"results".equals(jsonParser.getCurrentName())) {
                // Continue until we find the "results" field
            }
            // Move to the start of the "results" array
            currentToken = jsonParser.nextToken();
            if (currentToken != JsonToken.START_ARRAY) {
                throw new IOException("Expected JSON array but found: " + currentToken);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + resource.getFilename(), e);
        }
    }

    @Override
    public ComparisonResult read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (jsonParser != null) {
            currentToken = jsonParser.nextToken(); // Move to the next token
            if (currentToken == JsonToken.START_OBJECT) {
                // Read the JSON object as a string
                String jsonString = jsonParser.readValueAsTree().toString();
                // Parse the JSON string into a ComparisonResult object
                return objectMapper.readValue(jsonString, ComparisonResult.class);
            } else if (currentToken == JsonToken.END_ARRAY) {
                jsonParser.close();
                jsonParser = null;
            }
        }
        return null; // End of file
    }
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

// 创建 ObjectMapper 实例
ObjectMapper objectMapper = new ObjectMapper();

// 将 JSON 字符串转换为 Map
Map<String, Object> jsonMap = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});

// 获取 results 数组
List<Map<String, Object>> results = (List<Map<String, Object>>) jsonMap.get("results");

// 处理每个结果
for (Map<String, Object> result : results) {
    // 处理每个 JSON 对象
    String id = (String) result.get("id");
    // 其他处理逻辑...
}


-----------------------
 public List<String> extractJsonStrings(String content) {
        List<String> jsonStrings = new ArrayList<>();
        
        try (JsonParser parser = jsonFactory.createParser(content)) {
            // 确保我们从一个对象开始
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new IllegalStateException("Expected content to be an object");
            }
            
            // 查找 results 字段
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = parser.getCurrentName();
                if ("results".equals(fieldName)) {
                    // 确保 results 是一个数组
                    if (parser.nextToken() != JsonToken.START_ARRAY) {
                        throw new IllegalStateException("Expected results to be an array");
                    }
                    
                    // 处理数组中的每个对象
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        // 将当前对象转换为 JSON 字符串
                        String jsonString = objectMapper.writeValueAsString(parser.readValueAsTree());
                        jsonStrings.add(jsonString);
                    }
                } else {
                    // 跳过其他字段
                    parser.skipChildren();
                }
            }
        } catch (IOException e) {
            System.err.println("Error extracting JSON strings: " + e.getMessage());
            e.printStackTrace();
        }
        
        return jsonStrings;
    }
