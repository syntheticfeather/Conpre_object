import request from '../utils/request';

export const login = (username, password) => {
    return request({
        url: '/user/login',
        method: 'post',
        data: {
            username,
            password
        }
    });
};

export const register = (username, password) => {
    return request({
        url: '/user/register',
        method: 'post',
        data: {
            username,
            password
        }
    });
};

export const getUserInfo = () => {
    return request({
        url: '/user/info',
        method: 'get'
    });
};

export const updateUserInfo = (userInfo) => {
    return request({
        url: '/user/update',
        method: 'put',
        data: userInfo
    });
};

export const updatePassword = (oldPWD, newPWD, renewPWD) => {
    return request({
        url: '/user/password',
        method: 'put',
        data: {
            oldPWD,
            newPWD,
            renewPWD
        }
    });
};

export const updateAvatar = (avatarUrl) => {
    return request({
        url: '/user/avatar',
        method: 'put',
        data: {
            avatarUrl
        }
    });
};
