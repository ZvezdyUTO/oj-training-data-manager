import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  getAcceptedSummaries,
  getAcceptedSummary,
  getProblemFirstAccepted,
  getProblemSubmissions,
  getUserFirstAccepted,
  getUserSubmissions,
  listTrainingUsers,
} from '../api/training';
import {
  batchCreateMembers,
  deleteMember,
  listMembers,
  updateMember,
} from '../api/members';
import {
  listCollectionJobs,
  startCollectionJob,
} from '../api/collection';
import { OJ_NAMES, type TrainingQueryRange } from '../types';

// Author: huangbingrui.awa
const range: TrainingQueryRange = {
  acceptedFromDateUtcPlus8: '2026-07-01',
  acceptedToDateUtcPlus8: '2026-07-14',
  minProblemRating: '',
  maxProblemRating: '',
};

function successfulFetch(data: unknown = []) {
  return vi.fn(async () => ({
    ok: true,
    status: 200,
    json: async () => ({ code: 200, message: 'ok', data }),
  }));
}

function headersOf(call: [RequestInfo | URL, RequestInit?]): Headers {
  return new Headers(call[1]?.headers);
}

describe('public query API contract', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', successfulFetch());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('sends all training queries as public GET requests', async () => {
    await listTrainingUsers();
    await getAcceptedSummary('alice', range, OJ_NAMES.CODEFORCES);
    await getAcceptedSummaries(range, OJ_NAMES.CODEFORCES);
    await getUserSubmissions('alice', range, { page: 1, limit: 15 }, OJ_NAMES.CODEFORCES);
    await getProblemSubmissions('100:A', range, { page: 1, limit: 15 }, OJ_NAMES.CODEFORCES);
    await getUserFirstAccepted('alice', range, { page: 1, limit: 15 }, OJ_NAMES.CODEFORCES);
    await getProblemFirstAccepted('100:A', range, { page: 1, limit: 15 }, OJ_NAMES.CODEFORCES);

    const calls = vi.mocked(fetch).mock.calls;
    expect(calls).toHaveLength(7);
    calls.forEach((call) => {
      expect(call[1]?.method || 'GET').toBe('GET');
      expect(headersOf(call).has('X-Operation-Password')).toBe(false);
      expect(headersOf(call).has('Authorization')).toBe(false);
    });
    expect(String(calls[0]?.[0])).toContain('/api/training-data/users');
    expect(String(calls[6]?.[0])).toContain('/api/training-data/first-accepted/by-problem');
  });

  it('keeps member and job lists public', async () => {
    await listMembers();
    await listCollectionJobs();

    vi.mocked(fetch).mock.calls.forEach((call) => {
      expect(call[1]?.method || 'GET').toBe('GET');
      expect(headersOf(call).has('X-Operation-Password')).toBe(false);
    });
  });
});

describe('operation password API contract', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', successfulFetch());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('attaches the password only to member mutations', async () => {
    const input = {
      username: 'alice',
      needCollect: true,
      handles: { CODEFORCES: 'alice_cf' },
    };
    await batchCreateMembers('once-only-password', [input]);
    await updateMember('once-only-password', 'alice', input);
    await deleteMember('once-only-password', 'alice');

    const calls = vi.mocked(fetch).mock.calls;
    expect(calls.map((call) => call[1]?.method)).toEqual(['POST', 'PUT', 'DELETE']);
    calls.forEach((call) => {
      expect(headersOf(call).get('X-Operation-Password')).toBe('once-only-password');
      expect(headersOf(call).has('Authorization')).toBe(false);
    });
    expect(String(calls[0]?.[0])).toBe('/api/members/batch');
    expect(String(calls[1]?.[0])).toBe('/api/members/alice');
  });

  it('attaches the password when starting collection', async () => {
    await startCollectionJob('once-only-password', {
      usernames: ['alice'],
      lookbackHours: 24,
      lookbackHoursByUsername: { alice: 0 },
      refreshWarehouse: true,
      ojName: OJ_NAMES.ATCODER,
    });

    const call = vi.mocked(fetch).mock.calls[0]!;
    expect(call[1]?.method).toBe('POST');
    expect(headersOf(call).get('X-Operation-Password')).toBe('once-only-password');
    expect(String(call[0])).toBe('/api/training-data/submission-collection-jobs');
    expect(JSON.parse(String(call[1]?.body))).toMatchObject({
      lookbackHours: 24,
      lookbackHoursByUsername: { alice: 0 },
    });
  });

  it('rejects an empty operation password before issuing a request', async () => {
    expect(() => deleteMember('   ', 'alice')).toThrow('请输入操作密码');
    expect(fetch).not.toHaveBeenCalled();
  });
});
