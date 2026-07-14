import { mount } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';
import OperationPasswordDialog from '../components/OperationPasswordDialog.vue';

// Author: huangbingrui.awa
describe('OperationPasswordDialog', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('emits a one-shot password, clears the input, and never writes storage', async () => {
    const localStorageWrite = vi.spyOn(Storage.prototype, 'setItem');
    const wrapper = mount(OperationPasswordDialog, {
      props: {
        open: true,
        dialogId: 'test-operation',
        title: '确认修改？',
        description: '此操作需要密码。',
      },
      global: {
        stubs: { Teleport: true },
      },
    });
    const input = wrapper.get('input[type="password"]');
    await input.setValue('once-only-password');
    await wrapper.get('form').trigger('submit');
    await nextTick();

    expect(wrapper.emitted('confirm')).toEqual([['once-only-password']]);
    expect((wrapper.get('input[type="password"]').element as HTMLInputElement).value).toBe('');
    expect(localStorageWrite).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it('clears the password when cancelled', async () => {
    const wrapper = mount(OperationPasswordDialog, {
      props: {
        open: true,
        dialogId: 'test-cancel',
        title: '确认操作？',
        description: '取消后不保留密码。',
      },
      global: {
        stubs: { Teleport: true },
      },
    });
    const input = wrapper.get('input[type="password"]');
    await input.setValue('discard-me');
    await wrapper.findAll('button')[0]!.trigger('click');
    await nextTick();

    expect(wrapper.emitted('cancel')).toHaveLength(1);
    expect((wrapper.get('input[type="password"]').element as HTMLInputElement).value).toBe('');
    wrapper.unmount();
  });
});
