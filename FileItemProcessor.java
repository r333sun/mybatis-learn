package com.example.fileprocessor.batch;

import com.example.fileprocessor.model.FileRecord;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileItemProcessor implements ItemProcessor<List<String>, List<Map<String, Object>>> {
    private FileRecord currentFileRecord;

    @Override
    public List<Map<String, Object>> process(List<String> items) throws Exception {
        if (items == null || items.isEmpty()) {
            return null;
        }

        List<Map<String, Object>> processedResults = new ArrayList<>();
        
        for (String jsonLine : items) {
            try {
                // 使用JsonPath直接提取所有需要的数据
                List<String> ids = JsonPath.read(jsonLine, "$.results[*].id");
                List<String> partyAlternateIds = JsonPath.read(jsonLine, "$.results[*].body.related-ids.related-id[?(@.relationship-type=='primary')].alternate-id");
                List<String> statuses = JsonPath.read(jsonLine, "$.results[*].body.status");
                List<String> types = JsonPath.read(jsonLine, "$.results[*].body.general-info.type");
                List<String> fullNames = JsonPath.read(jsonLine, "$.results[*].body.full-name");

                // 获取最大长度，确保能处理所有数据
                int maxSize = Math.max(Math.max(Math.max(ids.size(), partyAlternateIds.size()), 
                                              Math.max(statuses.size(), types.size())), 
                                     fullNames.size());

                // 构建处理后的数据，允许某些值为空
                for (int i = 0; i < maxSize; i++) {
                    // 安全获取值，如果索引超出范围则返回null
                    String id = i < ids.size() ? ids.get(i) : null;
                    String partyAlternateId = i < partyAlternateIds.size() ? partyAlternateIds.get(i) : null;
                    String status = i < statuses.size() ? statuses.get(i) : null;
                    String type = i < types.size() ? types.get(i) : null;
                    String fullName = i < fullNames.size() ? fullNames.get(i) : null;

                    // 只有当至少有一个关键字段不为空时才添加结果
                    if (id != null || partyAlternateId != null) {
                        Map<String, Object> processedData = Map.of(
                            "id", id != null ? id : "",
                            "partyAlternateId", partyAlternateId != null ? partyAlternateId : "",
                            "status", status != null ? status : "",
                            "type", type != null ? type : "",
                            "fullName", fullName != null ? fullName : "",
                            "fileRecordId", currentFileRecord != null ? currentFileRecord.getId() : null
                        );
                        processedResults.add(processedData);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing JSON line: {}", jsonLine, e);
            }
        }

        return processedResults.isEmpty() ? null : processedResults;
    }

    public void setCurrentFileRecord(FileRecord fileRecord) {
        this.currentFileRecord = fileRecord;
    }
} 