<template>
  <section class="members-page" aria-label="成员管理">
    <section class="create-users-page admin-reference-page" aria-label="创建成员">
      <header class="reference-page-header">
        <span class="reference-page-icon"><UserPlus :size="22" /></span>
        <div>
          <h2>创建成员</h2>
          <p>文本导入会先填入信息栏；提交时创建成员，并为填写 handle 的行新增 OJ handle 绑定。</p>
        </div>
      </header>

      <p v-if="notice" class="admin-notice" role="status">{{ notice }}</p>
      <p v-if="pageError" class="form-error" role="alert">{{ pageError }}</p>

      <label class="create-import-field">
        <span>文本导入</span>
        <textarea
          v-model="importText"
          rows="7"
          :placeholder="importPlaceholder"
        />
      </label>
      <button class="secondary-button import-fill-button" type="button" @click="fillCreateRows">
        <FileInput :size="17" />填入信息栏
      </button>

      <form class="create-user-rows" @submit.prevent="prepareCreate">
        <div v-for="(row, index) in createRows" :key="index" class="create-user-row">
          <label>username<input v-model="row.username" required /></label>
          <label>Codeforces<input v-model="row.codeforcesHandle" placeholder="可选" /></label>
          <label>AtCoder<input v-model="row.atcoderHandle" placeholder="可选" /></label>
          <label>自动采集
            <select v-model="row.needCollect">
              <option :value="true">开启</option>
              <option :value="false">关闭</option>
            </select>
          </label>
          <button
            class="create-row-remove"
            type="button"
            aria-label="删除创建行"
            @click="removeCreateRow(index)"
          >
            <Trash2 :size="18" />
          </button>
        </div>
        <div class="create-user-actions">
          <button class="primary-button" type="submit"><UserPlus :size="17" />执行创建</button>
          <button class="secondary-button" type="button" @click="addCreateRow"><Plus :size="17" />增加一名成员</button>
          <span>{{ createRows.length }} 行待提交</span>
        </div>
      </form>
    </section>

    <section class="admin-user-management-panel admin-reference-page" aria-label="管理成员信息">
      <header class="reference-page-header member-management-header">
        <span class="reference-page-icon"><UsersRound :size="22" /></span>
        <div>
          <h2>管理成员信息</h2>
          <p>按 username 查询成员；在列表中直接修改 username、OJ handle 和自动采集状态。</p>
        </div>
        <button class="secondary-button refresh-members-button" type="button" :disabled="loading" @click="load">
          <RefreshCw :class="{ spin: loading }" :size="16" />刷新列表
        </button>
      </header>

      <label class="admin-user-search">
        <span>查询 username</span>
        <span class="admin-user-search-control">
          <Search :size="16" />
          <input v-model="searchQuery" aria-label="查询 username" placeholder="输入 username 子串" />
        </span>
      </label>
      <div class="reference-count"><strong>{{ filteredMembers.length }}</strong><span>个成员</span></div>

      <div class="table-shell admin-user-table-shell reference-user-table-shell">
        <table class="admin-user-table reference-user-table" aria-label="成员列表">
          <thead>
            <tr><th>成员信息</th><th>采集状态</th><th>更新时间</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-if="!filteredMembers.length">
              <td class="admin-user-empty" colspan="4">
                {{ loading ? '正在加载成员…' : searchQuery.trim() ? '没有匹配的成员' : '暂无成员' }}
              </td>
            </tr>
            <template v-for="member in filteredMembers" :key="member.username">
              <tr :class="{ 'is-expanded': editingUsername === member.username }">
                <td>
                  <div class="reference-user-profile">
                    <span class="reference-user-avatar reference-user-avatar-fallback" aria-hidden="true">
                      <UserRound :size="24" />
                    </span>
                    <div class="reference-user-identity">
                      <div class="reference-user-name"><strong>{{ member.username }}</strong></div>
                      <small>username · 唯一训练身份</small>
                      <div class="reference-handle-list">
                        <span v-for="handle in handleEntries(member)" :key="handle.oj">
                          <b>{{ handle.label }}</b>{{ handle.value }}
                        </span>
                        <em v-if="!handleEntries(member).length">未绑定 OJ handle</em>
                      </div>
                    </div>
                  </div>
                </td>
                <td>
                  <div class="reference-status-list">
                    <span :class="['collect-chip', { 'is-retired': !member.needCollect }]">
                      {{ member.needCollect ? '自动采集开启' : '自动采集关闭' }}
                    </span>
                  </div>
                </td>
                <td>{{ formatTime(member.updatedAt) }}</td>
                <td>
                  <button class="secondary-button reference-edit-button" type="button" @click="toggleEdit(member)">
                    <UserRoundCog :size="16" />编辑
                  </button>
                </td>
              </tr>
              <tr v-if="editingUsername === member.username && editForm" class="admin-user-edit-row">
                <td colspan="4">
                  <form class="admin-user-edit-form" @submit.prevent="prepareUpdate(member)">
                    <div class="admin-user-edit-grid">
                      <label>username<input v-model="editForm.username" required /></label>
                      <label>Codeforces handle<input v-model="editForm.codeforcesHandle" /></label>
                      <label>AtCoder handle<input v-model="editForm.atcoderHandle" /></label>
                      <label>自动采集
                        <select v-model="editForm.needCollect">
                          <option :value="true">开启</option>
                          <option :value="false">关闭</option>
                        </select>
                      </label>
                    </div>
                    <div class="admin-user-edit-actions">
                      <button class="primary-button" type="submit"><Save :size="16" />保存修改</button>
                      <button class="danger-button subtle" type="button" @click="prepareDelete(member)">
                        <Trash2 :size="16" />删除成员
                      </button>
                      <button class="secondary-button" type="button" @click="closeEdit">取消</button>
                    </div>
                  </form>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </section>

    <OperationPasswordDialog
      :open="Boolean(pendingOperation)"
      dialog-id="member-operation-password"
      :title="pendingOperation?.title || ''"
      :description="pendingOperation?.description || ''"
      :confirm-label="pendingOperation?.confirmLabel || '确认操作'"
      :tone="pendingOperation?.tone || 'default'"
      :busy="operationBusy"
      :error="operationError"
      @cancel="cancelOperation"
      @confirm="executeOperation"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  FileInput,
  Plus,
  RefreshCw,
  Save,
  Search,
  Trash2,
  UserPlus,
  UserRound,
  UserRoundCog,
  UsersRound,
} from '@lucide/vue';
import {
  batchCreateMembers,
  deleteMember,
  listMembers,
  updateMember,
} from '../api/members';
import OperationPasswordDialog from './OperationPasswordDialog.vue';
import { OJ_NAMES, type Member, type MemberInput, type OjName } from '../types';

// Author: huangbingrui.awa
interface MemberForm {
  username: string;
  codeforcesHandle: string;
  atcoderHandle: string;
  needCollect: boolean;
}

interface PendingOperation {
  title: string;
  description: string;
  confirmLabel: string;
  tone: 'default' | 'danger';
  run(password: string): Promise<void>;
}

const members = ref<Member[]>([]);
const loading = ref(true);
const pageError = ref('');
const notice = ref('');
const importText = ref('');
const importPlaceholder = '每行：username,Codeforces,AtCoder,needCollect\nteam_member_01,Utonut-Zvezdy,Zvezdy,true';
const searchQuery = ref('');
const createRows = ref<MemberForm[]>([emptyForm()]);
const editingUsername = ref<string | null>(null);
const editForm = ref<MemberForm | null>(null);
const pendingOperation = ref<PendingOperation | null>(null);
const operationBusy = ref(false);
const operationError = ref('');

const filteredMembers = computed(() => {
  const query = searchQuery.value.trim().toLocaleLowerCase();
  const sorted = [...members.value].sort((left, right) => left.username.localeCompare(right.username));
  return query ? sorted.filter((member) => member.username.toLocaleLowerCase().includes(query)) : sorted;
});

onMounted(() => {
  void load();
});

function emptyForm(): MemberForm {
  return { username: '', codeforcesHandle: '', atcoderHandle: '', needCollect: true };
}

function formOf(member: Member): MemberForm {
  return {
    username: member.username,
    codeforcesHandle: member.handles[OJ_NAMES.CODEFORCES] || '',
    atcoderHandle: member.handles[OJ_NAMES.ATCODER] || '',
    needCollect: member.needCollect,
  };
}

function inputOf(form: MemberForm): MemberInput {
  const username = form.username.trim();
  if (!username) throw new Error('username 不能为空。');
  const handles: Partial<Record<OjName, string>> = {};
  if (form.codeforcesHandle.trim()) handles[OJ_NAMES.CODEFORCES] = form.codeforcesHandle.trim();
  if (form.atcoderHandle.trim()) handles[OJ_NAMES.ATCODER] = form.atcoderHandle.trim();
  return { username, needCollect: form.needCollect, handles };
}

async function load(): Promise<void> {
  loading.value = true;
  pageError.value = '';
  try {
    members.value = await listMembers();
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : '加载成员失败。';
  } finally {
    loading.value = false;
  }
}

function fillCreateRows(): void {
  pageError.value = '';
  notice.value = '';
  try {
    const lines = importText.value
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith('#'));
    const rows = lines.flatMap((line, index) => {
      const columns = (line.includes('\t') ? line.split('\t') : line.split(','))
        .map((column) => column.trim());
      const [username = '', codeforcesHandle = '', atcoderHandle = '', collectValue = 'true'] = columns;
      if (username.toLocaleLowerCase() === 'username') return [];
      if (!username) throw new Error(`第 ${index + 1} 行缺少 username。`);
      const normalizedCollect = collectValue.toLocaleLowerCase();
      if (!['', 'true', 'false'].includes(normalizedCollect)) {
        throw new Error(`第 ${index + 1} 行 needCollect 必须是 true 或 false。`);
      }
      return [{
        username,
        codeforcesHandle,
        atcoderHandle,
        needCollect: normalizedCollect !== 'false',
      }];
    });
    if (!rows.length) throw new Error('请至少输入一行成员数据。');
    createRows.value = rows;
    notice.value = `已填入 ${rows.length} 行，请确认后执行创建。`;
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : '文本导入失败。';
  }
}

function addCreateRow(): void {
  createRows.value.push(emptyForm());
}

function removeCreateRow(index: number): void {
  createRows.value.splice(index, 1);
  if (!createRows.value.length) addCreateRow();
}

function prepareCreate(): void {
  pageError.value = '';
  notice.value = '';
  let inputs: MemberInput[];
  try {
    inputs = createRows.value.map(inputOf);
    const usernames = inputs.map((item) => item.username);
    if (new Set(usernames).size !== usernames.length) throw new Error('待创建列表中存在重复 username。');
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : '创建信息不完整。';
    return;
  }
  pendingOperation.value = {
    title: '确认创建 ' + inputs.length + ' 个成员？',
    description: '成员会立即加入公开目录；已填写的 OJ handle 将用于后续采集。',
    confirmLabel: '确认创建',
    tone: 'default',
    run: async (password) => {
      const created = await batchCreateMembers(password, inputs);
      members.value = [...members.value, ...created];
      createRows.value = [emptyForm()];
      importText.value = '';
      notice.value = '已创建 ' + created.length + ' 个成员。';
    },
  };
}

function toggleEdit(member: Member): void {
  if (editingUsername.value === member.username) {
    closeEdit();
    return;
  }
  editingUsername.value = member.username;
  editForm.value = formOf(member);
  notice.value = '';
  pageError.value = '';
}

function closeEdit(): void {
  editingUsername.value = null;
  editForm.value = null;
}

function prepareUpdate(member: Member): void {
  if (!editForm.value) return;
  let input: MemberInput;
  try {
    input = inputOf(editForm.value);
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : '修改信息不完整。';
    return;
  }
  const handleChanged = input.handles.CODEFORCES !== member.handles.CODEFORCES
    || input.handles.ATCODER !== member.handles.ATCODER;
  pendingOperation.value = {
    title: '确认修改 ' + member.username + '？',
    description: handleChanged
      ? 'OJ handle 发生变化。后端会清理旧 handle 对应的训练数据并重置采集游标，此操作不可撤销。'
      : '将保存 username 与自动采集状态的修改。',
    confirmLabel: handleChanged ? '确认清理并修改' : '确认保存',
    tone: handleChanged ? 'danger' : 'default',
    run: async (password) => {
      const updated = await updateMember(password, member.username, input);
      members.value = members.value.map((item) => item.username === member.username ? updated : item);
      closeEdit();
      notice.value = '成员修改已保存。';
    },
  };
}

function prepareDelete(member: Member): void {
  pendingOperation.value = {
    title: '永久删除 ' + member.username + '？',
    description: '该成员的 OJ 绑定、原始提交与全部数仓训练记录会被清理，操作无法恢复。',
    confirmLabel: '确认永久删除',
    tone: 'danger',
    run: async (password) => {
      await deleteMember(password, member.username);
      members.value = members.value.filter((item) => item.username !== member.username);
      closeEdit();
      notice.value = '已删除成员 ' + member.username + '。';
    },
  };
}

function cancelOperation(): void {
  if (operationBusy.value) return;
  pendingOperation.value = null;
  operationError.value = '';
}

async function executeOperation(password: string): Promise<void> {
  const operation = pendingOperation.value;
  if (!operation) return;
  operationBusy.value = true;
  operationError.value = '';
  try {
    await operation.run(password);
    pendingOperation.value = null;
  } catch (error) {
    operationError.value = error instanceof Error ? error.message : '操作失败。';
  } finally {
    operationBusy.value = false;
  }
}

function handleEntries(member: Member): Array<{ oj: OjName; label: string; value: string }> {
  return ([OJ_NAMES.CODEFORCES, OJ_NAMES.ATCODER] as OjName[]).flatMap((oj) => {
    const value = member.handles[oj];
    if (!value) return [];
    return [{ oj, label: oj === OJ_NAMES.CODEFORCES ? 'Codeforces' : 'AtCoder', value }];
  });
}

function formatTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value || '—';
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'short',
    timeStyle: 'short',
    hour12: false,
  }).format(date);
}
</script>
