import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue')
  },
  {
    path: '/accounts',
    name: 'Accounts',
    component: () => import('../views/AccountList.vue')
  },
  {
    path: '/strategies',
    name: 'Strategies',
    component: () => import('../views/StrategyList.vue')
  },
  {
    path: '/strategies/:id',
    name: 'StrategyDetail',
    component: () => import('../views/StrategyDetail.vue')
  },
  {
    path: '/strategies/:id/assets',
    name: 'StrategyAssets',
    component: () => import('../views/StrategyDetail.vue')
  },
  {
    path: '/strategies/:id/trade',
    name: 'StrategyTrade',
    component: () => import('../views/StrategyDetail.vue')
  },
  {
    path: '/strategies/:id/viewpoints',
    name: 'StrategyViewpoints',
    component: () => import('../views/StrategyDetail.vue')
  },
  {
    path: '/assets/:id',
    name: 'AssetDetail',
    component: () => import('../views/AssetDetail.vue')
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
