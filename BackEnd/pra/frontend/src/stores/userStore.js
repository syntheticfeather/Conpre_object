import { defineStore } from 'pinia';
import { login as apiLogin, register as apiRegister, getUserInfo, updateUserInfo, updatePassword, updateAvatar } from '../api/user';

export const useUserStore = defineStore('user', {
    state: () => ({
        token: localStorage.getItem('token') || '',
        userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
        isLogin: !!localStorage.getItem('token')
    }),
    actions: {
        async login(username, password) {
            const res = await apiLogin(username, password);
            this.token = res.token;
            this.isLogin = true;
            localStorage.setItem('token', res.token);
            await this.getUserInfo();
            return true;
        },
        async register(username, password) {
            await apiRegister(username, password);
            return true;
        },
        async getUserInfo() {
            this.userInfo = await getUserInfo();
            localStorage.setItem('userInfo', JSON.stringify(this.userInfo));
        },
        async updateUserInfo(userInfo) {
            this.userInfo = await updateUserInfo(userInfo);
            localStorage.setItem('userInfo', JSON.stringify(this.userInfo));
            return true;
        },
        async updatePassword(oldPWD, newPWD, renewPWD) {
            await updatePassword(oldPWD, newPWD, renewPWD);
            return true;
        },
        async updateAvatar(avatarUrl) {
            this.userInfo.userPic = avatarUrl;
            await updateAvatar(avatarUrl);
            localStorage.setItem('userInfo', JSON.stringify(this.userInfo));
        },
        logout() {
            this.token = '';
            this.userInfo = null;
            this.isLogin = false;
            localStorage.removeItem('token');
            localStorage.removeItem('userInfo');
        }
    }
});
