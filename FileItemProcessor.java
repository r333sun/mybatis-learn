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
————————————————————
你说的“process的过程会分成若干步”这个意思我理解为：

	每个 ItemProcessor 需要做多个逻辑阶段的处理，比如：字段校验 → 字段转换 → 数据比对 → 业务判断 → 结果封装等，逻辑复杂，想要拆分得更清晰、可维护。

✅ 最推荐方式：使用多个 Processor 组合成一个处理链

Spring Batch 原生就支持一个叫 CompositeItemProcessor 的类，它可以让你将多个 ItemProcessor 组成一个“处理流水线”。

示例：

@Bean
public ItemProcessor<YourObject, YourObject> compositeProcessor() {
    CompositeItemProcessor<YourObject, YourObject> processor = new CompositeItemProcessor<>();
    processor.setDelegates(List.of(
        new ValidationProcessor(),
        new TransformationProcessor(),
        new ComparisonProcessor(),
        new FinalDecisionProcessor()
    ));
    return processor;
}

每个 Processor 都单独负责一件事：

1. 校验阶段：

public class ValidationProcessor implements ItemProcessor<YourObject, YourObject> {
    @Override
    public YourObject process(YourObject item) throws Exception {
        if (item.getId() == null) throw new ValidationException("ID不能为空");
        return item;
    }
}

2. 字段转换阶段：

public class TransformationProcessor implements ItemProcessor<YourObject, YourObject> {
    @Override
    public YourObject process(YourObject item) {
        item.setNormalizedName(item.getName().trim().toLowerCase());
        return item;
    }
}

3. 数据库比对阶段：

public class ComparisonProcessor implements ItemProcessor<YourObject, YourObject> {
    @Autowired
    private YourDao dao;

    @Override
    public YourObject process(YourObject item) {
        YourObject dbObject = dao.findById(item.getId());
        if (!Objects.equals(item.getSomeValue(), dbObject.getSomeValue())) {
            item.setChanged(true);
        }
        return item;
    }
}

4. 最终业务判断（是否写入/过滤）：

public class FinalDecisionProcessor implements ItemProcessor<YourObject, YourObject> {
    @Override
    public YourObject process(YourObject item) {
        return item.isChanged() ? item : null; // return null 表示跳过写入
    }
}

✅ 优点

点	好处
解耦	每个 Processor 只做一件事，便于单测和维护
可组合	可以根据业务场景灵活调整处理顺序
可复用	相同校验逻辑可以在多个 Job 之间重用
易调试	出错能精确定位是哪一步的问题
可读性强	清晰展示每一步业务逻辑意图

✅ 如果处理链中有需要配置参数的 Processor？

你可以将它们声明为 Spring Bean，然后在配置类中通过注入再放入 Composite 中即可。

@Bean
public ComparisonProcessor comparisonProcessor(YourDao yourDao) {
    return new ComparisonProcessor(yourDao);
}

✅ 总结

方式	是否推荐	场景
单个大 Processor	不推荐	难维护、难调试
CompositeItemProcessor	强烈推荐	多阶段、业务链式处理

如果你告诉我你的每一步处理都有哪些逻辑，我可以帮你拆成 Processor 结构，并写一个完整的 Composite 示例。

要不要我直接给你出一个框架化的结构？
