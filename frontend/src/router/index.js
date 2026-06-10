import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'PollList',
    component: () => import('../views/PollList.vue')
  },
  {
    path: '/poll/:id',
    name: 'PollDetail',
    component: () => import('../views/PollDetail.vue')
  },
  {
    path: '/create',
    name: 'CreatePoll',
    component: () => import('../views/CreatePoll.vue')
  },
  {
    path: '/verify',
    name: 'VerifyPoll',
    component: () => import('../views/VerifyPoll.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue')
  }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
