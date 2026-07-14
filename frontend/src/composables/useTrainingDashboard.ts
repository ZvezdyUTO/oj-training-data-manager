import { computed, ref, type Ref } from 'vue';
import {
  getAcceptedSummaries,
  getAcceptedSummary,
  getProblemFirstAccepted,
  getProblemSubmissions,
  getUserFirstAccepted,
  getUserSubmissions,
  listTrainingUsers,
} from '../api/training';
import { OJ_NAMES } from '../types';
import type {
  AcceptedSummary,
  Member,
  OjName,
  ProblemFirstAcceptedReport,
  ProblemSubmissionReport,
  TrainingQueryMode,
  TrainingQueryRange,
  UserFirstAcceptedReport,
  UserSubmissionReport,
} from '../types';

export interface MultiUserSummaryRow {
  user: Member;
  status: 'ready' | 'error';
  summary: AcceptedSummary | null;
  message: string | null;
}

export interface MultiUserLoadProgress {
  completed: number;
  total: number;
  active: boolean;
  failed: number;
}

type DashboardStatus = 'loading' | 'ready' | 'error';

function dateUtcPlus8(date: Date): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date);
}

function recentWeek(): TrainingQueryRange {
  const end = new Date();
  const start = new Date(end);
  start.setDate(start.getDate() - 6);
  return {
    acceptedFromDateUtcPlus8: dateUtcPlus8(start),
    acceptedToDateUtcPlus8: dateUtcPlus8(end),
    minProblemRating: '',
    maxProblemRating: '',
  };
}

function errorMessageOf(error: unknown): string {
  return error instanceof Error ? error.message : '请求失败。';
}

export function compareMultiUserSummaryRows(left: MultiUserSummaryRow, right: MultiUserSummaryRow): number {
  if (!left.summary || !right.summary) {
    if (!left.summary && !right.summary) return left.user.username.localeCompare(right.user.username);
    return left.summary ? -1 : 1;
  }
  return right.summary.totalAcceptedProblemCount - left.summary.totalAcceptedProblemCount
    || left.user.username.localeCompare(right.user.username);
}

// Author: huangbingrui.awa
export function useTrainingDashboard(mode: Readonly<Ref<TrainingQueryMode>>) {
  const status = ref<DashboardStatus>('loading');
  const errorMessage = ref<string | null>(null);
  const users = ref<Member[]>([]);
  const includeRetiredUsers = ref(false);
  const selectedUsername = ref<string | null>(null);
  const selectedOjName = ref<OjName>(OJ_NAMES.CODEFORCES);
  const trainingQuery = ref<TrainingQueryRange>(recentWeek());
  const multiUserRows = ref<MultiUserSummaryRow[]>([]);
  const multiUserProgress = ref<MultiUserLoadProgress>({ completed: 0, total: 0, active: false, failed: 0 });
  const acceptedSummary = ref<AcceptedSummary | null>(null);
  const submissions = ref<UserSubmissionReport | null>(null);
  const firstAccepted = ref<UserFirstAcceptedReport | null>(null);
  const problemKey = ref('');
  const problemSubmissions = ref<ProblemSubmissionReport | null>(null);
  const problemFirstAccepted = ref<ProblemFirstAcceptedReport | null>(null);
  const submissionPage = ref(1);
  const submissionLimit = ref(15);
  const firstAcceptedPage = ref(1);
  const firstAcceptedLimit = ref(15);
  const problemSubmissionPage = ref(1);
  const problemSubmissionLimit = ref(15);
  const problemFirstAcceptedPage = ref(1);
  const problemFirstAcceptedLimit = ref(15);
  let requestSequence = 0;

  const selectedTrainingUser = computed(() => (
    users.value.find((item) => item.username === selectedUsername.value) || null
  ));
  const usersForSelectedOj = computed(() => (
    users.value.filter((item) => Boolean(item.handles[selectedOjName.value]))
  ));

  async function loadUsers(sequence: number): Promise<void> {
    const loaded = await listTrainingUsers(includeRetiredUsers.value);
    if (sequence !== requestSequence) return;
    users.value = loaded;
    if (selectedUsername.value && !loaded.some((item) => item.username === selectedUsername.value)) {
      selectedUsername.value = null;
      acceptedSummary.value = null;
      submissions.value = null;
      firstAccepted.value = null;
    }
  }

  async function loadMultiple(sequence: number): Promise<void> {
    const eligible = usersForSelectedOj.value;
    multiUserProgress.value = { completed: 0, total: eligible.length, active: true, failed: 0 };
    if (!eligible.length) {
      multiUserRows.value = [];
      multiUserProgress.value = { completed: 0, total: 0, active: false, failed: 0 };
      return;
    }
    try {
      const summaries = await getAcceptedSummaries(
        trainingQuery.value,
        selectedOjName.value,
        includeRetiredUsers.value,
      );
      if (sequence !== requestSequence) return;
      const byUsername = new Map(summaries.map((item) => [item.username, item]));
      multiUserRows.value = eligible.map((user): MultiUserSummaryRow => {
        const summary = byUsername.get(user.username) || null;
        return {
          user,
          status: summary ? 'ready' : 'error',
          summary,
          message: summary ? null : '批量汇总结果缺失。',
        };
      }).sort(compareMultiUserSummaryRows);
      const failed = multiUserRows.value.filter((row) => row.status === 'error').length;
      multiUserProgress.value = {
        completed: multiUserRows.value.length,
        total: eligible.length,
        active: false,
        failed,
      };
    } catch (error) {
      if (sequence !== requestSequence) return;
      const message = errorMessageOf(error);
      multiUserRows.value = eligible.map((user) => ({
        user,
        status: 'error',
        summary: null,
        message,
      }));
      multiUserProgress.value = {
        completed: eligible.length,
        total: eligible.length,
        active: false,
        failed: eligible.length,
      };
      throw error;
    }
  }

  async function loadSingle(sequence: number): Promise<void> {
    const username = selectedUsername.value;
    if (!username) return;
    const [summary, submissionReport, firstAcceptedReport] = await Promise.all([
      getAcceptedSummary(username, trainingQuery.value, selectedOjName.value),
      getUserSubmissions(username, trainingQuery.value, {
        page: submissionPage.value,
        limit: submissionLimit.value,
      }, selectedOjName.value),
      getUserFirstAccepted(username, trainingQuery.value, {
        page: firstAcceptedPage.value,
        limit: firstAcceptedLimit.value,
      }, selectedOjName.value),
    ]);
    if (sequence !== requestSequence) return;
    acceptedSummary.value = summary;
    submissions.value = submissionReport;
    firstAccepted.value = firstAcceptedReport;
  }

  async function loadProblem(sequence: number): Promise<void> {
    const key = problemKey.value.trim();
    if (!key) return;
    const [submissionReport, firstAcceptedReport] = await Promise.all([
      getProblemSubmissions(key, trainingQuery.value, {
        page: problemSubmissionPage.value,
        limit: problemSubmissionLimit.value,
      }, selectedOjName.value),
      getProblemFirstAccepted(key, trainingQuery.value, {
        page: problemFirstAcceptedPage.value,
        limit: problemFirstAcceptedLimit.value,
      }, selectedOjName.value),
    ]);
    if (sequence !== requestSequence) return;
    problemSubmissions.value = submissionReport;
    problemFirstAccepted.value = firstAcceptedReport;
  }

  async function refresh(activeMode: TrainingQueryMode = mode.value): Promise<void> {
    const sequence = ++requestSequence;
    status.value = 'loading';
    errorMessage.value = null;
    try {
      if (activeMode !== 'problem') await loadUsers(sequence);
      if (sequence !== requestSequence) return;
      if (activeMode === 'multiple') await loadMultiple(sequence);
      if (activeMode === 'single') await loadSingle(sequence);
      if (activeMode === 'problem') await loadProblem(sequence);
      if (sequence === requestSequence) status.value = 'ready';
    } catch (error) {
      if (sequence !== requestSequence) return;
      status.value = 'error';
      errorMessage.value = errorMessageOf(error);
    }
  }

  async function applyTrainingQuery(
    query: TrainingQueryRange,
    activeMode: TrainingQueryMode = mode.value,
  ): Promise<void> {
    trainingQuery.value = { ...query };
    submissionPage.value = 1;
    firstAcceptedPage.value = 1;
    problemSubmissionPage.value = 1;
    problemFirstAcceptedPage.value = 1;
    await refresh(activeMode);
  }

  async function chooseUsername(username: string): Promise<void> {
    selectedUsername.value = username;
    await refresh('single');
  }

  async function chooseOjName(ojName: OjName): Promise<void> {
    selectedOjName.value = ojName;
    if (selectedUsername.value && !usersForSelectedOj.value.some((item) => item.username === selectedUsername.value)) {
      selectedUsername.value = null;
    }
    await refresh();
  }

  async function setIncludeRetiredUsers(include: boolean): Promise<void> {
    includeRetiredUsers.value = include;
    await refresh();
  }

  async function retryMultiUserSummary(username: string): Promise<void> {
    const user = users.value.find((item) => item.username === username);
    if (!user) return;
    try {
      const summary = await getAcceptedSummary(username, trainingQuery.value, selectedOjName.value);
      multiUserRows.value = [
        ...multiUserRows.value.filter((item) => item.user.username !== username),
        { user, status: 'ready' as const, summary, message: null },
      ].sort(compareMultiUserSummaryRows);
      errorMessage.value = null;
      status.value = 'ready';
    } catch (error) {
      errorMessage.value = errorMessageOf(error);
      status.value = 'error';
    }
  }

  async function changeSubmissionPage(page: number, limit: number): Promise<void> {
    submissionPage.value = page;
    submissionLimit.value = limit;
    await refresh('single');
  }

  async function changeFirstAcceptedPage(page: number, limit: number): Promise<void> {
    firstAcceptedPage.value = page;
    firstAcceptedLimit.value = limit;
    await refresh('single');
  }

  async function changeProblemSubmissionPage(page: number, limit: number): Promise<void> {
    problemSubmissionPage.value = page;
    problemSubmissionLimit.value = limit;
    await refresh('problem');
  }

  async function changeProblemFirstAcceptedPage(page: number, limit: number): Promise<void> {
    problemFirstAcceptedPage.value = page;
    problemFirstAcceptedLimit.value = limit;
    await refresh('problem');
  }

  return {
    status,
    errorMessage,
    users,
    usersForSelectedOj,
    includeRetiredUsers,
    selectedUsername,
    selectedTrainingUser,
    selectedOjName,
    trainingQuery,
    multiUserRows,
    multiUserProgress,
    acceptedSummary,
    submissions,
    firstAccepted,
    problemKey,
    problemSubmissions,
    problemFirstAccepted,
    submissionPage,
    submissionLimit,
    firstAcceptedPage,
    firstAcceptedLimit,
    problemSubmissionPage,
    problemSubmissionLimit,
    problemFirstAcceptedPage,
    problemFirstAcceptedLimit,
    refresh,
    refreshDashboard: refresh,
    trainingUsers: users,
    applyTrainingQuery,
    chooseUsername,
    chooseOjName,
    setIncludeRetiredUsers,
    retryMultiUserSummary,
    changeSubmissionPage,
    changeFirstAcceptedPage,
    changeProblemSubmissionPage,
    changeProblemFirstAcceptedPage,
  };
}
