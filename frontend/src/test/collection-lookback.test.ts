import { describe, expect, it } from 'vitest';
import { OJ_NAMES, type Member } from '../types';
import {
  buildLookbacksByUsername,
  effectiveLookbackHours,
  isFirstCollection,
  parseLookbackHours,
} from '../utils/collectionLookback';

// Author: huangbingrui.awa
function member(username: string, lastCollectedAt: string | null): Member {
  return {
    username,
    needCollect: true,
    handles: { [OJ_NAMES.CODEFORCES]: `${username}_cf` },
    collectionStates: {
      [OJ_NAMES.CODEFORCES]: { lastCollectedAt },
    },
    createdAt: '2026-07-14T00:00:00Z',
    updatedAt: '2026-07-14T00:00:00Z',
  };
}

describe('collection lookback policy', () => {
  it('forces a never-collected member to zero regardless of the configured value', () => {
    const newMember = member('new-user', null);

    expect(isFirstCollection(newMember, OJ_NAMES.CODEFORCES)).toBe(true);
    expect(effectiveLookbackHours(newMember, OJ_NAMES.CODEFORCES, 999)).toBe(0);
  });

  it('keeps the configured value for members with a successful cursor', () => {
    const existingMember = member('existing-user', '2026-07-13T00:00:00Z');

    expect(effectiveLookbackHours(existingMember, OJ_NAMES.CODEFORCES, 24)).toBe(24);
  });

  it('builds mixed batch lookbacks per username', () => {
    const lookbacks = buildLookbacksByUsername([
      member('new-user', null),
      member('existing-user', '2026-07-13T00:00:00Z'),
    ], OJ_NAMES.CODEFORCES, 1440);

    expect(lookbacks).toEqual({
      'new-user': 0,
      'existing-user': 1440,
    });
  });

  it('ignores the configured value when every member is a first collection', () => {
    expect(buildLookbacksByUsername([
      member('new-user', null),
    ], OJ_NAMES.CODEFORCES, -999)).toEqual({ 'new-user': 0 });
  });

  it('accepts zero but rejects negative and fractional values', () => {
    expect(parseLookbackHours(0)).toBe(0);
    expect(() => parseLookbackHours(-1)).toThrow('大于等于 0');
    expect(() => parseLookbackHours(1.5)).toThrow('大于等于 0');
  });
});
