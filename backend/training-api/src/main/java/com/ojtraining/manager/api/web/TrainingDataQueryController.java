package com.ojtraining.manager.api.web;

import com.ojtraining.manager.trainingdata.common.app.account.OjHandleAccountService;
import com.ojtraining.manager.trainingdata.common.app.query.OjWarehouseQueryFacade;
import com.ojtraining.manager.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.ojtraining.manager.trainingdata.common.domain.oj.value.OjNames;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/training-data")
public class TrainingDataQueryController {
    private final OjWarehouseQueryFacade queryFacade;
    private final OjHandleAccountService handleAccountService;

    public TrainingDataQueryController(
            OjWarehouseQueryFacade queryFacade,
            OjHandleAccountService handleAccountService
    ) {
        this.queryFacade = queryFacade;
        this.handleAccountService = handleAccountService;
    }

    @GetMapping("/users")
    public ApiResponse<List<OjHandleAccount>> users(
            @RequestParam(value = "includeRetired", defaultValue = "false") boolean includeRetired
    ) {
        List<OjHandleAccount> members = handleAccountService.listAll().stream()
                .filter(member -> includeRetired || member.needCollect())
                .toList();
        return ApiResponse.ok("获取成功", members);
    }

    @GetMapping("/accepted-summary")
    public ApiResponse<?> acceptedSummary(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam String username,
            @RequestParam(required = false) String acceptedFromDateUtcPlus8,
            @RequestParam(required = false) String acceptedToDateUtcPlus8,
            @RequestParam(required = false) Integer minProblemRating,
            @RequestParam(required = false) Integer maxProblemRating
    ) {
        return ApiResponse.ok("获取成功", queryFacade.summarizeAcceptedProblems(
                ojName, username, acceptedFromDateUtcPlus8, acceptedToDateUtcPlus8,
                minProblemRating, maxProblemRating));
    }

    @GetMapping("/accepted-summaries")
    public ApiResponse<?> acceptedSummaries(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam(value = "includeRetired", defaultValue = "false") boolean includeRetired,
            @RequestParam(required = false) String acceptedFromDateUtcPlus8,
            @RequestParam(required = false) String acceptedToDateUtcPlus8,
            @RequestParam(required = false) Integer minProblemRating,
            @RequestParam(required = false) Integer maxProblemRating
    ) {
        return ApiResponse.ok("获取成功", queryFacade.summarizeAcceptedProblems(
                ojName, includeRetired, acceptedFromDateUtcPlus8, acceptedToDateUtcPlus8,
                minProblemRating, maxProblemRating));
    }

    @GetMapping("/submissions/by-user")
    public ApiResponse<?> submissionsByUser(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam String username,
            @RequestParam(required = false) String submittedFromUtcPlus8,
            @RequestParam(required = false) String submittedToUtcPlus8,
            @RequestParam(required = false) Integer minProblemRating,
            @RequestParam(required = false) Integer maxProblemRating,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.ok("获取成功", queryFacade.listStudentSubmissions(
                ojName, username, submittedFromUtcPlus8, submittedToUtcPlus8,
                minProblemRating, maxProblemRating, page, limit));
    }

    @GetMapping("/submissions/by-problem")
    public ApiResponse<?> submissionsByProblem(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam String problemKey,
            @RequestParam(required = false) String submittedFromUtcPlus8,
            @RequestParam(required = false) String submittedToUtcPlus8,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.ok("获取成功", queryFacade.listProblemSubmissions(
                ojName, problemKey, submittedFromUtcPlus8, submittedToUtcPlus8, page, limit));
    }

    @GetMapping("/first-accepted/by-user")
    public ApiResponse<?> firstAcceptedByUser(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam String username,
            @RequestParam(required = false) String firstAcceptedFromUtcPlus8,
            @RequestParam(required = false) String firstAcceptedToUtcPlus8,
            @RequestParam(required = false) Integer minProblemRating,
            @RequestParam(required = false) Integer maxProblemRating,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.ok("获取成功", queryFacade.summarizeStudentFirstAcceptedProblems(
                ojName, username, firstAcceptedFromUtcPlus8, firstAcceptedToUtcPlus8,
                minProblemRating, maxProblemRating, page, limit));
    }

    @GetMapping("/first-accepted/by-problem")
    public ApiResponse<?> firstAcceptedByProblem(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam String problemKey,
            @RequestParam(required = false) String firstAcceptedFromUtcPlus8,
            @RequestParam(required = false) String firstAcceptedToUtcPlus8,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.ok("获取成功", queryFacade.summarizeProblemFirstAcceptedHandles(
                ojName, problemKey, firstAcceptedFromUtcPlus8, firstAcceptedToUtcPlus8, page, limit));
    }
}
