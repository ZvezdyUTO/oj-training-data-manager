<template>
  <div class="dashboard-main">
    <TrainingQueryPanel :dashboard="dashboard" :mode="mode" />
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import TrainingQueryPanel from '../components/TrainingQueryPanel.vue';
import { useTrainingDashboard } from '../composables/useTrainingDashboard';
import type { TrainingQueryMode } from '../types';

// Author: huangbingrui.awa
const route = useRoute();
const mode = computed<TrainingQueryMode>(() => (
  route.meta.mode === 'single' || route.meta.mode === 'problem' ? route.meta.mode : 'multiple'
));
const dashboard = useTrainingDashboard(mode);

watch(mode, () => {
  void dashboard.refresh();
}, { immediate: true });
</script>
