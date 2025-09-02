<template>
    <div class="register-container">
        <el-card class="register-card">
            <h2>用户注册</h2>
            <el-form 
                ref="registerForm" 
                :model="registerForm" 
                :rules="registerRules" 
                label-width="80px"
            >
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="registerForm.username"></el-input>
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input 
                        v-model="registerForm.password" 
                        type="password" 
                        show-password
                    ></el-input>
                </el-form-item>
                <el-form-item label="确认密码" prop="rePassword">
                    <el-input 
                        v-model="registerForm.rePassword" 
                        type="password" 
                        show-password
                    ></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button 
                        type="primary" 
                        @click="handleRegister"
                        :loading="isLoading"
                    >
                        注册
                    </el-button>
                    <el-button @click="toLogin">已有账号</el-button>
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

const registerForm = ref({
    username: '',
    password: '',
    rePassword: ''
});

const isLoading = ref(false);

const registerRules = {
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { pattern: /^\S{5,16}$/, message: '用户名必须为5-16位非空字符', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { pattern: /^\S{5,16}$/, message: '密码必须为5-16位非空字符', trigger: 'blur' }
    ],
    rePassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        { validator: validatePassword, trigger: 'blur' }
    ]
};

const validatePassword = (rule, value, callback) => {
    if (value !== registerForm.value.password) {
        callback(new Error('两次输入密码不一致'));
    } else {
        callback();
    }
};

const handleRegister = async () => {
    try {
        isLoading.value = true;
        await userStore.register(registerForm.value.username, registerForm.value.password);
        router.push('/login');
    } catch (error) {
        console.error('注册失败:', error);
    } finally {
        isLoading.value = false;
    }
};

const toLogin = () => {
    router.push('/login');
};
</script>

<style scoped>
.register-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    background-color: #f5f5f5;
}
.register-card {
    width: 400px;
    padding: 20px;
}
</style>
