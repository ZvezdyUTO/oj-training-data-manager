<template>
  <section class="training-data-collection-panel admin-reference-page" aria-label="训练数据采集">
    <header class="reference-page-header collection-reference-header">
      <span class="reference-page-icon"><Database :size="22" /></span>
      <div>
        <h2>训练数据采集</h2>
        <p>首次采集抓取全部历史数据；之后从上次成功窗口结束时间向前倒退指定小时数，并采集至当前时间。</p>
      </div>
      <div class="collection-reference-controls">
        <label>
          OJ
          <select v-model="collectionOj">
            <option :value="OJ_NAMES.CODEFORCES">Codeforces</option>
            <option :value="OJ_NAMES.ATCODER">AtCoder</option>
          </select>
        </label>
        <label>
          统一倒退小时数
          <input v-model="globalLookback" min="0" type="number" />
        </label>
        <button
          class="primary-button collect-all-button"
          :disabled="allBusy || !collectableMembers.length"
          type="button"
          @click="collectAll"
        >
          <RefreshCw :class="{ spin: allBusy }" :size="18" />
          {{ allBusy ? '正在采集' : '全部采集' }}
        </button>
      </div>
    </header>
    <p v-if="collectError" class="form-error" role="alert">{{ collectError }}</p>

    <div class="collection-member-list">
      <article v-for="member in collectableMembers" :key="member.username" class="collection-member-row">
        <div class="collection-member-identity">
          <strong><span>{{ member.username }}</span></strong>
          <span>{{ OJ_LABELS[collectionOj] }}：{{ member.handles[collectionOj] }}</span>
          <div class="collection-member-state">
            <b :class="{ 'is-ready': member.collectionStates[collectionOj]?.lastCollectedAt }">
              {{ collectionProgressLabel(member.collectionStates[collectionOj]) }}
            </b>
            <small>最近成功窗口结束：{{ collectionTimeLabel(member.collectionStates[collectionOj]?.lastCollectedAt) }}</small>
          </div>
        </div>
        <label>
          倒退小时数{{ isFirstCollection(member, collectionOj) ? '（首次固定为 0）' : '' }}
          <input
            :value="memberLookbackValue(member)"
            min="0"
            type="number"
            placeholder="沿用统一值"
            :disabled="isFirstCollection(member, collectionOj)"
            @input="updateMemberLookback(member.username, $event)"
          />
        </label>
        <button
          class="primary-button"
          :disabled="busyUsers.has(member.username)"
          type="button"
          @click="collectOne(member.username)"
        >
          <RefreshCw :class="{ spin: busyUsers.has(member.username) }" :size="18" />
          {{ busyUsers.has(member.username) ? '正在采集' : '执行采集' }}
        </button>
      </article>
      <p v-if="!collectableMembers.length" class="batch-target-empty">当前 OJ 暂无可采集的现役队员。</p>
    </div>

    <section class="collection-history-section">
      <header>
        <div>
          <h2>采集任务历史</h2>
          <p>运行中任务会自动轮询，终态后停止。</p>
        </div>
      </header>
      <ul v-if="jobs.length" class="collection-job-list">
        <li v-for="job in jobs" :key="job.jobId">
          <button class="collection-job-row" type="button" @click="toggleJob(job.jobId)">
            <span>
              <strong>{{ shortJobId(job.jobId) }}</strong>
              <small>{{ formatTime(job.startedAt) }}</small>
            </span>
            <strong :class="['result-status', `result-status-${statusClass(job.status)}`]">
              {{ jobStatus(job.status) }}
            </strong>
            <small>
              采集 {{ job.collectedCount }}/{{ job.requestedCount }}，失败 {{ job.failedCount }}，写入 {{ job.writtenRows }} 行
            </small>
            <ChevronDown :class="{ 'is-expanded': expandedJobs.has(job.jobId) }" :size="16" />
          </button>
          <div v-if="expandedJobs.has(job.jobId)" class="collection-job-detail">
            <dl>
              <div><dt>任务 ID</dt><dd>{{ job.jobId }}</dd></div>
              <div><dt>完成时间</dt><dd>{{ job.finishedAt ? formatTime(job.finishedAt) : '运行中' }}</dd></div>
            </dl>
            <div class="collection-result-table-scroll">
              <table class="collection-result-table">
                <thead>
                  <tr><th>队员 / handle</th><th>状态</th><th>写入</th><th>匹配</th><th>仓库</th><th>批次</th></tr>
                </thead>
                <tbody>
                  <tr v-for="row in job.items" :key="`${row.username}-${row.ojName}`">
                    <td><strong>{{ row.username }}</strong><small>{{ row.handle || row.message || '等待解析' }}</small></td>
                    <td>{{ row.itemStatus }}</td>
                    <td>{{ row.writtenRows }} 行</td>
                    <td>{{ row.matchedSubmissionCount }}/{{ row.fetchedSubmissionCount }}</td>
                    <td>{{ row.refreshStatus }}</td>
                    <td>{{ row.batchId || '无批次' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </li>
      </ul>
      <p v-else class="batch-target-empty">暂无采集任务。</p>
    </section>

    <OperationPasswordDialog
      :open="Boolean(pendingCollection)"
      dialog-id="collection-operation-password"
      title="确认执行数据采集？"
      :description="collectionDescription"
      confirm-label="确认执行采集"
      busy-label="正在启动…"
      :busy="operationBusy"
      :error="operationError"
      @cancel="cancelCollection"
      @confirm="executeCollection"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { ChevronDown, Database, RefreshCw } from '@lucide/vue';
import {
  getCollectionJob,
  listCollectionJobs,
  startCollectionJob,
} from '../api/collection';
import { listMembers } from '../api/members';
import OperationPasswordDialog from './OperationPasswordDialog.vue';
import {
  buildLookbacksByUsername,
  effectiveLookbackHours,
  isFirstCollection,
  parseLookbackHours,
} from '../utils/collectionLookback';
import {
  OJ_LABELS,
  OJ_NAMES,
  type CollectionJob,
  type CollectionJobStatus,
  type CollectionState,
  type Member,
  type OjName,
} from '../types';

// Author: huangbingrui.awa
interface PendingCollection {
  usernames: string[];
  targetLabel: string;
  lookbackHours: number;
  lookbackHoursByUsername: Record<string, number>;
  firstCollectionCount: number;
  ojName: OjName;
  all: boolean;
}

const members = ref<Member[]>([]);
const jobs = ref<CollectionJob[]>([]);
const collectionOj = ref<OjName>(OJ_NAMES.CODEFORCES);
const globalLookback = ref<string | number>('1440');
const lookbackByUsername = ref<Record<string, string | number>>({});
const collectError = ref('');
const allBusy = ref(false);
const busyUsers = ref(new Set<string>());
const expandedJobs = ref(new Set<string>());
const pendingCollection = ref<PendingCollection | null>(null);
const operationBusy = ref(false);
const operationError = ref('');
const pollTimers = new Map<string, number>();
let mounted = true;

const collectableMembers = computed(() => members.value.filter(
  (member) => member.needCollect && Boolean(member.handles[collectionOj.value]),
));
const collectionDescription = computed(() => {
  const pending = pendingCollection.value;
  if (!pending) return '';
  const existingCount = pending.usernames.length - pending.firstCollectionCount;
  const firstCollectionText = pending.firstCollectionCount
    ? `其中 ${pending.firstCollectionCount} 名尚无成功记录，倒退小时强制为 0 并采集全部历史数据。`
    : '';
  const incrementalText = existingCount
    ? `其余 ${existingCount} 名从上次窗口结束时间向前倒退 ${pending.lookbackHours} 小时后采集至当前时间。`
    : '';
  return `即将采集 ${pending.targetLabel} 的 ${OJ_LABELS[pending.ojName]} 提交。${firstCollectionText}${incrementalText}采集完成后会自动刷新数仓。`;
});

onMounted(() => {
  mounted = true;
  void load();
});

onBeforeUnmount(() => {
  mounted = false;
  pollTimers.forEach((timer) => window.clearTimeout(timer));
  pollTimers.clear();
});

async function load(): Promise<void> {
  collectError.value = '';
  try {
    const [loadedMembers, loadedJobs] = await Promise.all([
      listMembers(),
      listCollectionJobs(),
    ]);
    members.value = loadedMembers;
    jobs.value = loadedJobs;
    loadedJobs.filter(isActive).forEach((job) => schedulePoll(job.jobId));
  } catch (error) {
    collectError.value = error instanceof Error ? error.message : '加载采集状态失败。';
  }
}

function collectAll(): void {
  const targetMembers = collectableMembers.value;
  const usernames = targetMembers.map((member) => member.username);
  if (!usernames.length) return;
  collectError.value = '';
  operationError.value = '';
  try {
    const firstCollectionCount = targetMembers.filter(
      (member) => isFirstCollection(member, collectionOj.value),
    ).length;
    const lookbackHours = firstCollectionCount === targetMembers.length
      ? 0
      : parseLookbackHours(globalLookback.value);
    const lookbackHoursByUsername = buildLookbacksByUsername(
      targetMembers,
      collectionOj.value,
      lookbackHours,
    );
    pendingCollection.value = {
      usernames,
      targetLabel: `全部 ${usernames.length} 名队员`,
      lookbackHours,
      lookbackHoursByUsername,
      firstCollectionCount,
      ojName: collectionOj.value,
      all: true,
    };
  } catch (error) {
    collectError.value = error instanceof Error ? error.message : '倒退小时数无效。';
  }
}

function collectOne(username: string): void {
  collectError.value = '';
  operationError.value = '';
  try {
    const member = collectableMembers.value.find((candidate) => candidate.username === username);
    if (!member) return;
    const lookbackHours = effectiveLookbackHours(
      member,
      collectionOj.value,
      lookbackByUsername.value[username] ?? globalLookback.value,
    );
    pendingCollection.value = {
      usernames: [username],
      targetLabel: username,
      lookbackHours,
      lookbackHoursByUsername: { [username]: lookbackHours },
      firstCollectionCount: isFirstCollection(member, collectionOj.value) ? 1 : 0,
      ojName: collectionOj.value,
      all: false,
    };
  } catch (error) {
    collectError.value = error instanceof Error ? error.message : '倒退小时数无效。';
  }
}

function cancelCollection(): void {
  if (operationBusy.value) return;
  pendingCollection.value = null;
  operationError.value = '';
}

async function executeCollection(password: string): Promise<void> {
  const request = pendingCollection.value;
  if (!request) return;
  operationBusy.value = true;
  operationError.value = '';
  if (request.all) {
    allBusy.value = true;
  } else {
    const next = new Set(busyUsers.value);
    next.add(request.usernames[0]!);
    busyUsers.value = next;
  }
  try {
    const started = await startCollectionJob(password, {
      usernames: request.usernames,
      lookbackHours: request.lookbackHours,
      lookbackHoursByUsername: request.lookbackHoursByUsername,
      refreshWarehouse: true,
      ojName: request.ojName,
    });
    upsertJob(started);
    pendingCollection.value = null;
    if (isActive(started)) schedulePoll(started.jobId);
  } catch (error) {
    operationError.value = error instanceof Error ? error.message : '启动采集失败。';
  } finally {
    operationBusy.value = false;
    allBusy.value = false;
    const done = new Set(busyUsers.value);
    request.usernames.forEach((username) => done.delete(username));
    busyUsers.value = done;
  }
}

function schedulePoll(jobId: string): void {
  if (!mounted || pollTimers.has(jobId)) return;
  const timer = window.setTimeout(async () => {
    pollTimers.delete(jobId);
    if (!mounted) return;
    try {
      const job = await getCollectionJob(jobId);
      upsertJob(job);
      if (isActive(job)) schedulePoll(jobId);
      else members.value = await listMembers();
    } catch (error) {
      collectError.value = error instanceof Error ? error.message : '刷新任务进度失败。';
    }
  }, 1200);
  pollTimers.set(jobId, timer);
}

function upsertJob(job: CollectionJob): void {
  jobs.value = [job, ...jobs.value.filter((item) => item.jobId !== job.jobId)];
}

function isActive(job: CollectionJob): boolean {
  return job.status === 'PENDING' || job.status === 'RUNNING';
}

function toggleJob(id: string): void {
  const next = new Set(expandedJobs.value);
  if (next.has(id)) next.delete(id);
  else next.add(id);
  expandedJobs.value = next;
}

function shortJobId(id: string): string {
  return id.length > 18 ? `${id.slice(0, 18)}...` : id;
}

function formatTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'short',
    timeStyle: 'short',
    hour12: false,
  }).format(new Date(value));
}

function statusClass(status: string): string {
  return status.toLowerCase().replaceAll('_', '-');
}

function jobStatus(status: CollectionJobStatus): string {
  if (status === 'PENDING') return '等待中';
  if (status === 'RUNNING') return '正在执行';
  if (status === 'SUCCESS') return '执行成功';
  if (status === 'PARTIAL_SUCCESS') return '部分成功';
  return '执行失败';
}

function collectionProgressLabel(state?: CollectionState): string {
  return state?.lastCollectedAt ? '已建立增量采集游标' : '首次采集将抓取全部历史';
}

function memberLookbackValue(member: Member): string | number {
  if (isFirstCollection(member, collectionOj.value)) return 0;
  return lookbackByUsername.value[member.username] ?? globalLookback.value;
}

function updateMemberLookback(username: string, event: Event): void {
  lookbackByUsername.value[username] = (event.target as HTMLInputElement).value;
}

function collectionTimeLabel(value?: string | null): string {
  return value ? formatTime(value) : '尚无记录';
}
</script>
