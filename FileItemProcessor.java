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
