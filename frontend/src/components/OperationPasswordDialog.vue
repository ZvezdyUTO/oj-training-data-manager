<template>
  <Teleport to="body">
    <div v-if="open" class="admin-confirm-backdrop dialog-backdrop" role="presentation" @click.self="cancel">
      <section
        :class="['admin-confirm-dialog', 'password-dialog', tone === 'danger' ? 'is-danger' : 'is-info']"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="dialogId + '-title'"
        :aria-describedby="dialogId + '-description'"
      >
        <span class="admin-confirm-icon dialog-icon" aria-hidden="true">
          <ShieldCheck v-if="tone !== 'danger'" :size="24" />
          <TriangleAlert v-else :size="24" />
        </span>
        <div class="admin-confirm-copy dialog-copy">
          <h3 :id="dialogId + '-title'">{{ title }}</h3>
          <p :id="dialogId + '-description'">{{ description }}</p>
        </div>
        <form class="operation-password-form" @submit.prevent="confirm">
          <label>
            <span>全局操作密码</span>
            <input
              ref="passwordInput"
              v-model="password"
              type="password"
              autocomplete="off"
              required
              aria-label="全局操作密码"
              placeholder="仅用于这一次请求"
            />
          </label>
          <small>密码不会保存到浏览器；请求结束后立即丢弃。</small>
          <p v-if="error" class="dialog-error" role="alert">{{ error }}</p>
          <div class="admin-confirm-actions dialog-actions">
            <button type="button" :disabled="busy" @click="cancel">取消</button>
            <button class="admin-confirm-primary" type="submit" :disabled="busy || !password.trim()">
              <LoaderCircle v-if="busy" class="spin" :size="16" />
              {{ busy ? busyLabel : confirmLabel }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { LoaderCircle, ShieldCheck, TriangleAlert } from '@lucide/vue';

// Author: huangbingrui.awa
const props = withDefaults(defineProps<{
  open: boolean;
  dialogId: string;
  title: string;
  description: string;
  confirmLabel?: string;
  busyLabel?: string;
  tone?: 'default' | 'danger';
  busy?: boolean;
  error?: string;
}>(), {
  confirmLabel: '确认操作',
  busyLabel: '正在执行…',
  tone: 'default',
  busy: false,
  error: '',
});
const emit = defineEmits<{ cancel: []; confirm: [password: string] }>();
const password = ref('');
const passwordInput = ref<HTMLInputElement | null>(null);

watch(() => props.open, async (open) => {
  clearPassword();
  if (open) {
    await nextTick();
    passwordInput.value?.focus();
  }
});

onBeforeUnmount(() => {
  clearPassword();
});

function cancel() {
  if (props.busy) return;
  clearPassword();
  emit('cancel');
}

function confirm() {
  const value = password.value;
  if (!value.trim() || props.busy) return;
  clearPassword();
  emit('confirm', value);
}

function clearPassword() {
  password.value = '';
  if (passwordInput.value) passwordInput.value.value = '';
}
</script>
