import { requestData } from './client';
import type {
  AcceptedSummary,
  Member,
  OjName,
  PageQuery,
  ProblemFirstAcceptedReport,
  ProblemSubmissionReport,
  TrainingQueryRange,
  UserFirstAcceptedReport,
  UserSubmissionReport,
} from '../types';

type QueryValue = string | number | boolean | null | undefined;

function withQuery(path: string, query: Record<string, QueryValue>): string {
  const search = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== null && value !== undefined && String(value).trim()) {
      search.set(key, String(value));
    }
  });
  const suffix = search.toString();
  return suffix ? path + '?' + suffix : path;
}

function dateStart(value: string): string {
  return value ? value + 'T00:00:00' : '';
}

function dateEnd(value: string): string {
  return value ? value + 'T23:59:59' : '';
}

export function listTrainingUsers(includeRetired = false, signal?: AbortSignal): Promise<Member[]> {
  return requestData(withQuery('/training-data/users', {
    includeRetired: includeRetired ? true : undefined,
  }), { signal });
}

export function getAcceptedSummary(
  username: string,
  range: TrainingQueryRange,
  ojName: OjName,
  signal?: AbortSignal,
): Promise<AcceptedSummary> {
  return requestData(withQuery('/training-data/accepted-summary', {
    ojName,
    username,
    ...range,
  }), { signal });
}

export function getAcceptedSummaries(
  range: TrainingQueryRange,
  ojName: OjName,
  includeRetired = false,
  signal?: AbortSignal,
): Promise<AcceptedSummary[]> {
  return requestData(withQuery('/training-data/accepted-summaries', {
    ojName,
    includeRetired: includeRetired ? true : undefined,
    ...range,
  }), { signal });
}

export function getUserSubmissions(
  username: string,
  range: TrainingQueryRange,
  page: PageQuery,
  ojName: OjName,
  signal?: AbortSignal,
): Promise<UserSubmissionReport> {
  return requestData(withQuery('/training-data/submissions/by-user', {
    ojName,
    username,
    submittedFromUtcPlus8: dateStart(range.acceptedFromDateUtcPlus8),
    submittedToUtcPlus8: dateEnd(range.acceptedToDateUtcPlus8),
    minProblemRating: range.minProblemRating,
    maxProblemRating: range.maxProblemRating,
    ...page,
  }), { signal });
}

export function getProblemSubmissions(
  problemKey: string,
  range: TrainingQueryRange,
  page: PageQuery,
  ojName: OjName,
  signal?: AbortSignal,
): Promise<ProblemSubmissionReport> {
  return requestData(withQuery('/training-data/submissions/by-problem', {
    ojName,
    problemKey,
    submittedFromUtcPlus8: dateStart(range.acceptedFromDateUtcPlus8),
    submittedToUtcPlus8: dateEnd(range.acceptedToDateUtcPlus8),
    ...page,
  }), { signal });
}

export function getUserFirstAccepted(
  username: string,
  range: TrainingQueryRange,
  page: PageQuery,
  ojName: OjName,
  signal?: AbortSignal,
): Promise<UserFirstAcceptedReport> {
  return requestData(withQuery('/training-data/first-accepted/by-user', {
    ojName,
    username,
    firstAcceptedFromUtcPlus8: dateStart(range.acceptedFromDateUtcPlus8),
    firstAcceptedToUtcPlus8: dateEnd(range.acceptedToDateUtcPlus8),
    minProblemRating: range.minProblemRating,
    maxProblemRating: range.maxProblemRating,
    ...page,
  }), { signal });
}

export function getProblemFirstAccepted(
  problemKey: string,
  range: TrainingQueryRange,
  page: PageQuery,
  ojName: OjName,
  signal?: AbortSignal,
): Promise<ProblemFirstAcceptedReport> {
  return requestData(withQuery('/training-data/first-accepted/by-problem', {
    ojName,
    problemKey,
    firstAcceptedFromUtcPlus8: dateStart(range.acceptedFromDateUtcPlus8),
    firstAcceptedToUtcPlus8: dateEnd(range.acceptedToDateUtcPlus8),
    ...page,
  }), { signal });
}
