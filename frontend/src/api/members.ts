import { operationHeaders, requestData } from './client';
import type { Member, MemberInput } from '../types';

export function listMembers(signal?: AbortSignal): Promise<Member[]> {
  return requestData('/training-data/users?includeRetired=true', { signal });
}

export function batchCreateMembers(password: string, members: MemberInput[]): Promise<Member[]> {
  return requestData('/members/batch', {
    method: 'POST',
    headers: operationHeaders(password),
    body: JSON.stringify({ members }),
  });
}

export function updateMember(
  password: string,
  currentUsername: string,
  member: MemberInput,
): Promise<Member> {
  return requestData('/members/' + encodeURIComponent(currentUsername), {
    method: 'PUT',
    headers: operationHeaders(password),
    body: JSON.stringify(member),
  });
}

export function deleteMember(password: string, username: string): Promise<unknown> {
  return requestData('/members/' + encodeURIComponent(username), {
    method: 'DELETE',
    headers: operationHeaders(password),
  });
}
