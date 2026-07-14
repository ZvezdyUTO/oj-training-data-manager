<template>
  <div class="training-site">
    <header class="blog-topbar">
      <div class="blog-topbar-inner">
        <RouterLink class="site-name" to="/multiple" aria-label="OJ Training Data Manager 首页">
          OJ Training Data Manager
        </RouterLink>

        <nav class="standalone-training-nav" aria-label="主导航">
          <RouterLink to="/multiple" active-class="is-active">
            <UsersRound :size="17" aria-hidden="true" />多人统计
          </RouterLink>
          <RouterLink to="/single" active-class="is-active">
            <UserRound :size="17" aria-hidden="true" />单人查询
          </RouterLink>
          <RouterLink to="/problem" active-class="is-active">
            <Braces :size="17" aria-hidden="true" />题目查询
          </RouterLink>
          <RouterLink to="/members" active-class="is-active">
            <ContactRound :size="17" aria-hidden="true" />成员管理
          </RouterLink>
          <RouterLink to="/collection" active-class="is-active">
            <Database :size="17" aria-hidden="true" />数据采集
          </RouterLink>
        </nav>

        <button
          class="blog-theme-toggle standalone-theme-toggle"
          type="button"
          role="switch"
          :aria-label="theme === 'dark' ? '切换到日间模式' : '切换到深夜模式'"
          :aria-checked="theme === 'dark'"
          :title="theme === 'dark' ? '切换到日间模式' : '切换到深夜模式'"
          @click="switchTheme"
        >
          <span class="theme-switch-track" :class="{ 'is-dark': theme === 'dark' }" aria-hidden="true">
            <span class="theme-switch-thumb">
              <Moon v-if="theme === 'dark'" :size="13" />
              <Sun v-else :size="13" />
            </span>
          </span>
          <span class="theme-switch-status">{{ theme === 'dark' ? '深夜模式' : '日间模式' }}</span>
        </button>
      </div>
    </header>

    <main class="training-main"><slot /></main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Braces, ContactRound, Database, Moon, Sun, UserRound, UsersRound } from '@lucide/vue';
import { currentTheme, toggleTheme, type ColorTheme } from '../theme';

// Author: huangbingrui.awa
const theme = ref<ColorTheme>(currentTheme());

function switchTheme() {
  theme.value = toggleTheme();
}
</script>
