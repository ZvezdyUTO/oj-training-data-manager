import { createRouter, createWebHistory } from 'vue-router';
import TrainingView from '../views/TrainingView.vue';
import MembersView from '../views/MembersView.vue';
import CollectionView from '../views/CollectionView.vue';

// Author: huangbingrui.awa
export default createRouter({
  history: createWebHistory('/'),
  routes: [
    { path: '/', redirect: '/multiple' },
    { path: '/multiple', name: 'multiple', component: TrainingView, meta: { mode: 'multiple' } },
    { path: '/single', name: 'single', component: TrainingView, meta: { mode: 'single' } },
    { path: '/problem', name: 'problem', component: TrainingView, meta: { mode: 'problem' } },
    { path: '/members', name: 'members', component: MembersView },
    { path: '/collection', name: 'collection', component: CollectionView },
    { path: '/:pathMatch(.*)*', redirect: '/multiple' },
  ],
});
