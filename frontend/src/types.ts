export type Username = string;

export const OJ_NAMES = {
  CODEFORCES: 'CODEFORCES',
  ATCODER: 'ATCODER',
} as const;

export type OjName = typeof OJ_NAMES[keyof typeof OJ_NAMES];

export const OJ_LABELS: Record<OjName, string> = {
  [OJ_NAMES.CODEFORCES]: 'Codeforces',
  [OJ_NAMES.ATCODER]: 'AtCoder',
};

export interface CollectionState {
  lastCollectedAt: string | null;
}

export interface Member {
  username: Username;
  needCollect: boolean;
  handles: Partial<Record<OjName, string>>;
  collectionStates: Partial<Record<OjName, CollectionState>>;
  createdAt: string;
  updatedAt: string;
}

export type TrainingUser = Member;

export interface MemberInput {
  username: Username;
  needCollect: boolean;
  handles: Partial<Record<OjName, string>>;
}

export interface TrainingQueryRange {
  acceptedFromDateUtcPlus8: string;
  acceptedToDateUtcPlus8: string;
  minProblemRating: string;
  maxProblemRating: string;
}

export interface PageQuery {
  page: number;
  limit: number;
}

export interface AcceptedSummary {
  username: Username;
  authorHandle: string;
  totalAcceptedProblemCount: number;
  ratingCounts: Array<{
    problemRating: string;
    acceptedProblemCount: number;
  }>;
}

export interface SubmissionItem {
  submissionId: string;
  username: Username;
  handle: string;
  submittedAtUtcPlus8: string | null;
  submittedDateUtcPlus8: string | null;
  problemKey: string | null;
  problemIndex: string | null;
  problemName: string | null;
  difficulty: string | null;
  language: string | null;
  verdict: string | null;
  accepted: boolean;
  timeConsumedMillis: number | null;
  sourceUrl: string | null;
}

export interface UserSubmissionReport {
  username: Username;
  authorHandle: string;
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
  submissions: SubmissionItem[];
}

export interface ProblemSubmissionReport {
  problemKey: string;
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
  submissions: SubmissionItem[];
}

export interface FirstAcceptedProblem {
  problemKey: string;
  problemIndex: string | null;
  problemName: string | null;
  difficulty: string | null;
  firstAcceptedSubmissionId: string;
  firstAcceptedAtUtcPlus8: string;
  firstAcceptedDateUtcPlus8: string;
  firstAcceptedLanguage: string | null;
  firstAcceptedSourceUrl: string | null;
}

export interface UserFirstAcceptedReport {
  username: Username;
  authorHandle: string;
  totalAcceptedProblemCount: number;
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
  problems: FirstAcceptedProblem[];
}

export interface ProblemFirstAcceptedHandle {
  username: Username;
  handle: string;
  firstAcceptedAtUtcPlus8: string;
}

export interface ProblemFirstAcceptedReport {
  problemKey: string;
  acceptedHandleCount: number;
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
  acceptedHandles: ProblemFirstAcceptedHandle[];
}

export type TrainingQueryMode = 'multiple' | 'single' | 'problem';

export interface CollectionJobStartRequest {
  usernames: Username[];
  lookbackHours: number;
  lookbackHoursByUsername?: Record<Username, number>;
  refreshWarehouse: boolean;
  ojName: OjName | null;
}

export type CollectionJobStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';
export type CollectionJobItemStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';
export type CollectionStatus = 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED' | 'SKIPPED';
export type CollectionRefreshStatus = 'NOT_REQUESTED' | 'NO_BATCH' | 'SUCCESS' | 'FAILED';

export interface CollectionJobItem {
  username: Username;
  ojName: OjName | null;
  itemStatus: CollectionJobItemStatus;
  collectionStatus: CollectionStatus | null;
  handle: string | null;
  batchId: string | null;
  tableName: string | null;
  writtenRows: number;
  fetchedSubmissionCount: number;
  matchedSubmissionCount: number;
  fetchedAt: string | null;
  message: string | null;
  refreshStatus: CollectionRefreshStatus;
  refreshMessage: string | null;
}

export interface CollectionJob {
  jobId: string;
  ojName: OjName | null;
  status: CollectionJobStatus;
  requestedCount: number;
  completedCount: number;
  collectedCount: number;
  failedCount: number;
  refreshedCount: number;
  writtenRows: number;
  batchIds: string[];
  startedAt: string;
  finishedAt: string | null;
  message: string | null;
  items: CollectionJobItem[];
}
