package com.drama.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.drama.common.R;
import com.drama.entity.TaskLog;
import com.drama.service.TaskLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务进度追踪接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/task-logs")
@RequiredArgsConstructor
public class TaskLogController {

    private final TaskLogService taskLogService;

    /**
     * 分页查询任务日志
     */
    @GetMapping
    public R<IPage<TaskLog>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String dramaId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType) {
        IPage<TaskLog> page = taskLogService.page(pageNum, pageSize, dramaId, status, taskType);
        return R.ok(page);
    }

    /**
     * 查询剧集的所有任务日志
     */
    @GetMapping("/drama/{dramaId}")
    public R<List<TaskLog>> listByDramaId(@PathVariable String dramaId) {
        List<TaskLog> list = taskLogService.listByDramaId(dramaId);
        return R.ok(list);
    }

    /**
     * 获取正在运行的任务（用于轮询）
     */
    @GetMapping("/running")
    public R<List<TaskLog>> runningTasks() {
        List<TaskLog> tasks = taskLogService.listRunningTasks();
        return R.ok(tasks);
    }

    /**
     * 获取单个任务详情
     */
    @GetMapping("/{id}")
    public R<TaskLog> getById(@PathVariable String id) {
        TaskLog task = taskLogService.getById(id);
        if (task == null || task.getDeleted() == 1) {
            throw new com.drama.common.BusinessException(com.drama.common.ResultCode.NOT_FOUND, "任务不存在");
        }
        return R.ok(task);
    }

    /**
     * 删除任务记录
     */
    @PostMapping("/{id}/delete")
    public R<Void> delete(@PathVariable String id) {
        TaskLog task = taskLogService.getById(id);
        if (task == null || task.getDeleted() == 1) {
            throw new com.drama.common.BusinessException(com.drama.common.ResultCode.NOT_FOUND, "任务不存在");
        }
        task.setDeleted(1);
        taskLogService.updateById(task);
        return R.ok();
    }

    /**
     * 清理已完成/失败的任务记录
     * 逻辑删除 N 天前（默认30天）状态为 completed 或 failed 的任务
     */
    @PostMapping("/cleanup")
    public R<Void> cleanup(@RequestParam(defaultValue = "30") int daysBefore) {
        if (daysBefore < 1) daysBefore = 1; // 至少保留1天
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysBefore);
        int deleted = taskLogService.cleanupOlderThan(cutoff);
        log.info("Cleanup task logs: deleted {} records older than {} days", deleted, daysBefore);
        return R.ok();
    }
}
