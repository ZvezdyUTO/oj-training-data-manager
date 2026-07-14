import { mount } from '@vue/test-utils';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { createMemoryHistory, createRouter } from 'vue-router';
import { describe, expect, it } from 'vitest';
import AppShell from '../components/AppShell.vue';

// Author: huangbingrui.awa
describe('restored Training UI contract', () => {
  it('puts every query and management entry directly in the black top bar', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        '/multiple',
        '/single',
        '/problem',
        '/members',
        '/collection',
      ].map((path) => ({ path, component: { template: '<div />' } })),
    });
    await router.push('/multiple');
    await router.isReady();

    const wrapper = mount(AppShell, { global: { plugins: [router] } });
    const links = wrapper.findAll('.standalone-training-nav > a');

    expect(links.map((link) => link.text())).toEqual([
      '多人统计',
      '单人查询',
      '题目查询',
      '成员管理',
      '数据采集',
    ]);
    expect(links.map((link) => link.attributes('href'))).toEqual([
      '/multiple',
      '/single',
      '/problem',
      '/members',
      '/collection',
    ]);
    expect(wrapper.find('.top-nav-dropdown').exists()).toBe(false);
    expect(wrapper.find('.nav-group').exists()).toBe(false);
  });

  it('keeps the original Training query DOM and compact pagination contract', () => {
    const query = readFileSync(resolve(process.cwd(), 'src/components/TrainingQueryPanel.vue'), 'utf8');
    const pagination = readFileSync(resolve(process.cwd(), 'src/components/PaginationBar.vue'), 'utf8');

    for (const className of [
      'query-form',
      'query-meta-row',
      'multi-summary-panel',
      'training-stat-grid',
      'rating-panel',
      'recent-submission-panel',
    ]) {
      expect(query).toContain(className);
    }
    expect(query).not.toContain('page-header');
    expect(query).not.toContain('data-surface');
    expect(pagination).toContain('submission-pagination');
    expect(pagination).toContain('const sizes = [15, 50, 100, 200]');
  });

  it('keeps the original 52px black shell tokens', () => {
    const shell = readFileSync(resolve(process.cwd(), 'src/styles/shell.css'), 'utf8');

    expect(shell).toContain('min-height: 52px');
    expect(shell).toContain('background: #1b1c1d');
    expect(shell).toContain('max-width: 1400px');
    expect(shell).toContain('padding: 24px 0 34px');
  });
});
