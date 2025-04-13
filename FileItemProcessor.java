package com.example.fileprocessor.reader;

import com.example.fileprocessor.entity.FileRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

public class DynamicJsonFileReader implements ItemReader<FileRecord> {

    private final Resource resource;
    private final ObjectMapper objectMapper;
    private Iterator<FileRecord> recordIterator;

    public DynamicJsonFileReader(Resource resource) {
        this.resource = resource;
        this.objectMapper = new ObjectMapper();
        initializeReader();
    }

    private void initializeReader() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            List<FileRecord> records = parseJsonArray(jsonContent.toString());
            this.recordIterator = records.iterator();
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + resource.getFilename(), e);
        }
    }

    private List<FileRecord> parseJsonArray(String jsonArrayString) {
        try {
            return objectMapper.readValue(jsonArrayString, objectMapper.getTypeFactory().constructCollectionType(List.class, FileRecord.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing JSON array", e);
        }
    }

    @Override
    public FileRecord read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (recordIterator != null && recordIterator.hasNext()) {
            return recordIterator.next();
        }
        return null; // End of file
    }
}


@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Value("#{jobParameters['filePath']}")
    private Resource dynamicFileResource;

    @Bean
    public Job compareAndWriteJob(Step compareAndWriteStep) {
        return jobBuilderFactory.get("compareAndWriteJob")
                .start(compareAndWriteStep)
                .build();
    }

    @Bean
    public Step compareAndWriteStep(ItemProcessor<FileRecord, FileRecord> processor, ItemWriter<FileRecord> writer) {
        return stepBuilderFactory.get("compareAndWriteStep")
                .<FileRecord, FileRecord>chunk(10)
                .reader(fileReader())
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public DynamicJsonFileReader fileReader() {
        return new DynamicJsonFileReader(dynamicFileResource);
    }

    @Bean
    public ItemProcessor<FileRecord, FileRecord> preProcessor() {
        return fileRecord -> {
            // 初步处理逻辑
            fileRecord.setProcessedTime(LocalDateTime.now());
            fileRecord.setStatus("PROCESSING");
            return fileRecord;
        };
    }

    @Bean
    public ItemWriter<FileRecord> differenceWriter() {
        return items -> {
            for (FileRecord item : items) {
                System.out.println("Writing item: " + item);
                // 写入数据库或其他存储逻辑
            }
        };
    }
}
