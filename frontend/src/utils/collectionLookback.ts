import type { Member, OjName } from '../types';

// Author: huangbingrui.awa
export function isFirstCollection(member: Member, ojName: OjName): boolean {
  return !member.collectionStates[ojName]?.lastCollectedAt;
}

export function parseLookbackHours(value: string | number): number {
  const normalized = String(value).trim();
  const hours = Number(normalized);
  if (!normalized || !Number.isInteger(hours) || hours < 0) {
    throw new Error('倒退小时数必须是大于等于 0 的整数。');
  }
  return hours;
}

export function effectiveLookbackHours(
  member: Member,
  ojName: OjName,
  configuredLookback: string | number,
): number {
  return isFirstCollection(member, ojName) ? 0 : parseLookbackHours(configuredLookback);
}

export function buildLookbacksByUsername(
  members: Member[],
  ojName: OjName,
  configuredLookback: string | number,
): Record<string, number> {
  const hasIncrementalMember = members.some((member) => !isFirstCollection(member, ojName));
  const parsedLookback = hasIncrementalMember ? parseLookbackHours(configuredLookback) : 0;
  return Object.fromEntries(members.map((member) => [
    member.username,
    isFirstCollection(member, ojName) ? 0 : parsedLookback,
  ]));
}
