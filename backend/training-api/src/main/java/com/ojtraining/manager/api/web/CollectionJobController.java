package com.ojtraining.manager.api.web;

import com.ojtraining.manager.trainingdata.common.collector.job.OjSubmissionCollectionJobService;
import com.ojtraining.manager.trainingdata.common.web.collector.request.OjSubmissionCollectionJobStartRequest;
import com.ojtraining.manager.trainingdata.common.web.collector.response.OjSubmissionCollectionJobResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/training-data/submission-collection-jobs")
public class CollectionJobController {
    private final OjSubmissionCollectionJobService jobService;

    public CollectionJobController(OjSubmissionCollectionJobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ApiResponse<OjSubmissionCollectionJobResponse> startJob(
            @RequestBody OjSubmissionCollectionJobStartRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        return ApiResponse.ok("任务已创建", OjSubmissionCollectionJobResponse.from(
                jobService.startBatchCollection(
                        request.requireUsernames(),
                        request.requireLookbackDuration(),
                        request.requireLookbackDurationsByUsername(),
                        request.refreshWarehouseOrDefault(),
                        request.optionalOjName()
                )
        ));
    }

    @GetMapping
    public ApiResponse<List<OjSubmissionCollectionJobResponse>> listJobs() {
        return ApiResponse.ok("获取成功", jobService.listJobs().stream()
                .map(OjSubmissionCollectionJobResponse::from)
                .toList());
    }

    @GetMapping("/{jobId}")
    public ApiResponse<OjSubmissionCollectionJobResponse> getJob(@PathVariable String jobId) {
        return ApiResponse.ok("获取成功", OjSubmissionCollectionJobResponse.from(jobService.getJob(jobId)));
    }
}
