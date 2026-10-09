import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/orders/new' },
    { path: '/admin/products/new', redirect: '/admin/products' },
    {
      path: '/admin/products',
      name: 'product-management',
      component: () => import('../views/ProductManagementView.vue'),
    },
    {
      path: '/orders/new',
      name: 'order-new',
      component: () => import('../views/OrderNewView.vue'),
    },
  ],
})

export default router
