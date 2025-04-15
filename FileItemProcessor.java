import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ObjectComparer {
    public static void main(String[] args) {
        Person person1 = new Person("John", 30, "New York");
        Person person2 = new Person("John", 25, "Los Angeles");

        List<Diff> differences = compareObjects(person1, person2);
        for (Diff diff : differences) {
            System.out.println(diff);
        }
    }

    public static List<Diff> compareObjects(Object obj1, Object obj2) {
        List<Diff> diffs = new ArrayList<>();

        // 确保两个对象是同一类型
        if (obj1.getClass() != obj2.getClass()) {
            System.out.println("Objects are of different types and cannot be compared.");
            return diffs;
        }

        // 获取类的所有字段（属性）
        Field[] fields = obj1.getClass().getDeclaredFields();

        try {
            for (Field field : fields) {
                field.setAccessible(true);  // 设置字段可访问

                // 获取属性值
                Object value1 = field.get(obj1);
                Object value2 = field.get(obj2);

                // 如果属性值不同，记录到 List<Diff> 中
                if (value1 == null && value2 != null || value1 != null && !value1.equals(value2)) {
                    diffs.add(new Diff(field.getName(), value1, value2));
                }

                // 如果字段本身是嵌套对象，则递归调用比较
                if (value1 != null && value2 != null && value1.getClass().isAssignableFrom(value2.getClass())) {
                    diffs.addAll(compareObjects(value1, value2));  // 递归比较并加入差异
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return diffs;
    }
}

class Person {
    private String name;
    private int age;
    private String city;

    public Person(String name, int age, String city) {
        this.name = name;
        this.age = age;
        this.city = city;
    }

    // Getters and Setters (if necessary)
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

要不要我直接给你出一个框架化
	------------------------------------------------------------
	public static void main(String[] args) {
        String jdbcURL = "jdbc:oracle:thin:@//your_db_host:port/service_name";
        String username = "your_username";
        String password = "your_password";

        try (Connection conn = DriverManager.getConnection(jdbcURL, username, password)) {
            // SQL 查询，假设返回的列中包含了 STRUCT 类型
            String query = "SELECT your_struct_column FROM your_table WHERE your_condition";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                 
                while (rs.next()) {
                    // 获取 STRUCT 类型的列
                    STRUCT struct = (STRUCT) rs.getObject("your_struct_column");
                    if (struct != null) {
                        printValue(struct);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据传入对象的类型递归打印：
     *  - 如果为null，直接打印null；
     *  - 如果是STRUCT，则调用 printStructAttributes 递归打印；
     *  - 如果是数组（Array），则取出数组元素逐个处理；
     *  - 否则直接打印对象值。
     */
    public static void printValue(Object value) throws SQLException {
        if (value == null) {
            System.out.println("null");
        } else if (value instanceof STRUCT) {
            System.out.println("Nested STRUCT:");
            printStructAttributes((STRUCT) value);
        } else if (value instanceof java.sql.Array) {
            System.out.println("Array:");
            java.sql.Array sqlArray = (java.sql.Array) value;
            Object[] elements = (Object[]) sqlArray.getArray();
            for (int i = 0; i < elements.length; i++) {
                System.out.print("Element " + i + ": ");
                printValue(elements[i]);  // 对数组中的每个元素也递归调用
            }
        } else {
            System.out.println(value);
        }
    }

    /**
     * 遍历并打印 STRUCT 中的所有属性。
     */
    public static void printStructAttributes(STRUCT struct) throws SQLException {
        Object[] attributes = struct.getAttributes();
        for (int i = 0; i < attributes.length; i++) {
            System.out.print("Attribute " + i + ": ");
            printValue(attributes[i]);  // 对每个属性进行递归处理
        }
    }
