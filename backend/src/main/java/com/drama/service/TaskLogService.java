package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drama.common.IdUtils;
import com.drama.entity.TaskLog;
import com.drama.mapper.TaskLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务日志服务 - 记录和查询异步任务状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskLogService extends ServiceImpl<TaskLogMapper, TaskLog> {

    /**
     * 创建任务日志记录
     */
    @Transactional
    public TaskLog create(String dramaId, String taskType, String taskId) {
        TaskLog taskLog = new TaskLog();
        taskLog.setId(IdUtils.randomId());
        taskLog.setDramaId(dramaId);
        taskLog.setTaskType(taskType);
        taskLog.setTaskId(taskId);
        taskLog.setStatus("pending");
        taskLog.setProgress(0);
        taskLog.setCreatedAt(LocalDateTime.now());
        taskLog.setUpdatedAt(LocalDateTime.now());

        this.save(taskLog);
        log.info("Created task log: {} type={} taskId={}", taskLog.getId(), taskType, taskId);
        return taskLog;
    }

    /**
     * 更新任务进度
     */
    @Transactional
    public void updateProgress(String id, String status, Integer progress, String message) {
        TaskLog taskLog = new TaskLog();
        taskLog.setId(id);
        taskLog.setStatus(status);
        if (progress != null) {
            taskLog.setProgress(progress);
        }
        if (message != null) {
            taskLog.setMessage(message);
        }
        taskLog.setUpdatedAt(LocalDateTime.now());
        this.updateById(taskLog);
    }

    /**
     * 标记任务完成
     */
    @Transactional
    public void complete(String id, String result) {
        TaskLog taskLog = new TaskLog();
        taskLog.setId(id);
        taskLog.setStatus("completed");
        taskLog.setProgress(100);
        taskLog.setResult(result);
        taskLog.setUpdatedAt(LocalDateTime.now());
        this.updateById(taskLog);
        log.info("Task completed: {}", id);
    }

    /**
     * 标记任务失败
     */
    @Transactional
    public void fail(String id, String errorMessage) {
        TaskLog taskLog = new TaskLog();
        taskLog.setId(id);
        taskLog.setStatus("failed");
        taskLog.setMessage(errorMessage);
        taskLog.setUpdatedAt(LocalDateTime.now());
        this.updateById(taskLog);
        log.warn("Task failed: {} - {}", id, errorMessage);
    }

    /**
     * 按厂商任务ID查询
     */
    public TaskLog getByVendorTaskId(String vendorTaskId) {
        LambdaQueryWrapper<TaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskLog::getTaskId, vendorTaskId)
               .eq(TaskLog::getDeleted, 0)
               .orderByDesc(TaskLog::getCreatedAt)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    /**
     * 查询剧集的所有任务日志
     */
    public List<TaskLog> listByDramaId(String dramaId) {
        LambdaQueryWrapper<TaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskLog::getDramaId, dramaId)
               .eq(TaskLog::getDeleted, 0)
               .orderByDesc(TaskLog::getCreatedAt);
        return this.list(wrapper);
    }

    /**
     * 查询正在运行的任务（用于轮询检查）
     */
    public List<TaskLog> listRunningTasks() {
        LambdaQueryWrapper<TaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(TaskLog::getStatus, "pending", "running")
               .eq(TaskLog::getDeleted, 0)
               .orderByAsc(TaskLog::getCreatedAt);
        return this.list(wrapper);
    }

    /**
     * 分页查询任务日志
     */
    public IPage<TaskLog> page(int pageNum, int pageSize, String dramaId, String status, String taskType) {
        Page<TaskLog> p = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskLog::getDeleted, 0)
               .orderByDesc(TaskLog::getUpdatedAt);

        if (StringUtils.hasText(dramaId)) {
            wrapper.eq(TaskLog::getDramaId, dramaId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(TaskLog::getStatus, status);
        }
        if (StringUtils.hasText(taskType)) {
            wrapper.eq(TaskLog::getTaskType, taskType);
        }

        return this.page(p, wrapper);
    }
}
