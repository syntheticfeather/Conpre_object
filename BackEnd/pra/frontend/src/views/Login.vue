<template>
    <div class="login-container">
        <el-card class="login-card">
            <h2>用户登录</h2>
            <el-form 
                ref="loginForm" 
                :model="loginForm" 
                :rules="loginRules" 
                label-width="80px"
            >
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="loginForm.username"></el-input>
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input 
                        v-model="loginForm.password" 
                        type="password" 
                        show-password
                    ></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button 
                        type="primary" 
                        @click="handleLogin"
                        :loading="isLoading"
                    >
                        登录
                    </el-button>
                    <el-button @click="toRegister">注册</el-button>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/userStore';

const router = useRouter();
const userStore = useUserStore();

const loginForm = ref({
    username: '',
    password: ''
});

const isLoading = ref(false);

const loginRules = {
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { pattern: /^\S{5,16}$/, message: '用户名必须为5-16位非空字符', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { pattern: /^\S{5,16}$/, message: '密码必须为5-16位非空字符', trigger: 'blur' }
    ]
};

const handleLogin = async () => {
    try {
        isLoading.value = true;
        await userStore.login(loginForm.value.username, loginForm.value.password);
        router.push('/');
    } catch (error) {
        console.error('登录失败:', error);
    } finally {
        isLoading.value = false;
    }
};

const toRegister = () => {
    router.push('/register');
};
</script>

<style scoped>
.login-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    background-color: #f5f5f5;
}
.login-card {
    width: 400px;
    padding: 20px;
}
</style>
