import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '../stores/userStore';

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('../views/Register.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/articles',
        name: 'ArticleList',
        component: () => import('../views/ArticleList.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/articles/:id',
        name: 'ArticleDetail',
        component: () => import('../views/ArticleDetail.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/articles/edit/:id?',
        name: 'ArticleEdit',
        component: () => import('../views/ArticleEdit.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/categories',
        name: 'CategoryList',
        component: () => import('../views/CategoryList.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { requiresAuth: true }
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore();
    
    if (to.meta.requiresAuth && !userStore.isLogin) {
        next({ name: 'Login' });
    } else if ((to.name === 'Login' || to.name === 'Register') && userStore.isLogin) {
        next({ name: 'Home' });
    } else {
        next();
    }
});

export default router;
