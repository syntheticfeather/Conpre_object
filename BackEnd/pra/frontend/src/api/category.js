import request from '../utils/request';

export const getCategories = () => {
    return request({
        url: '/category/list',
        method: 'get'
    });
};

export const addCategory = (category) => {
    return request({
        url: '/category/add',
        method: 'post',
        data: category
    });
};

export const updateCategory = (category) => {
    return request({
        url: '/category/update',
        method: 'put',
        data: category
    });
};

export const deleteCategory = (id) => {
    return request({
        url: `/category/delete/${id}`,
        method: 'delete'
    });
};
