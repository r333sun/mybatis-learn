JobInstanceDao jobInstanceDao = new org.springframework.batch.core.repository.dao.MapJobInstanceDao();
        JobExecutionDao jobExecutionDao = new org.springframework.batch.core.repository.dao.MapJobExecutionDao();
        StepExecutionDao stepExecutionDao = new org.springframework.batch.core.repository.dao.MapStepExecutionDao();
        
        // 创建SimpleJobRepository，不进行持久化
        return new SimpleJobRepository(jobInstanceDao, jobExecutionDao, stepExecutionDao);
