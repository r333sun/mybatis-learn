for (int i = 0; i < inputIds.size(); i += batchSize) {
        List<String> batch = inputIds.subList(i, Math.min(i + batchSize, inputIds.size()));
        String placeholders = String.join(",", Collections.nCopies(batch.size(), "?"));
        String sql = "SELECT your_column FROM your_table WHERE your_column IN (" + placeholders + ")";
        List<String> found = jdbcTemplate.queryForList(sql, batch.toArray(), String.class);
        existingIds.addAll(found);
    }

    // 构造结果：存在就返回 ID，不存在就返回 null
    List<String> result = new ArrayList<>();
    for (String id : inputIds) {
        if (existingIds.contains(id)) {
            result.add(id);
        } else {
            result.add(null);
        }
    }

    return result;
