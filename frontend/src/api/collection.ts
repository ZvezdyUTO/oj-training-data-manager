import { operationHeaders, requestData } from './client';
import type { CollectionJob, CollectionJobStartRequest } from '../types';

const JOB_PATH = '/training-data/submission-collection-jobs';

export function listCollectionJobs(signal?: AbortSignal): Promise<CollectionJob[]> {
  return requestData(JOB_PATH, { signal });
}

export function getCollectionJob(jobId: string, signal?: AbortSignal): Promise<CollectionJob> {
  return requestData(JOB_PATH + '/' + encodeURIComponent(jobId), { signal });
}

export function startCollectionJob(
  password: string,
  request: CollectionJobStartRequest,
): Promise<CollectionJob> {
  return requestData(JOB_PATH, {
    method: 'POST',
    headers: operationHeaders(password),
    body: JSON.stringify(request),
  });
}
