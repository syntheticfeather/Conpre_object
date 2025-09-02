import request from '../utils/request';

export const getArticleList = (pageNum, pageSize, categoryId, state) => {
    return request({
        url: '/article/list',
        method: 'get',
        params: {
            pageNum,
            pageSize,
            categoryId,
            state
        }
    });
};

export const getArticleDetail = (id) => {
    return request({
        url: `/article/detail/${id}`,
        method: 'get'
    });
};

export const addArticle = (article) => {
    return request({
        url: '/article/add',
        method: 'post',
        data: article
    });
};

export const updateArticle = (article) => {
    return request({
        url: '/article/update',
        method: 'put',
        data: article
    });
};

export const deleteArticle = (id) => {
    return request({
        url: `/article/delete/${id}`,
        method: 'delete'
    });
};
