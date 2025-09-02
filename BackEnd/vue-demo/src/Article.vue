<script setup lang="ts">
import { ref } from 'vue';
import axios from 'axios';
const articleList = ref([]);
const SearchCondition = ref();
axios.get('http://localhost:8080/article/All')
    .then(r => {
        articleList.value = r.data.data.items;
    })
    .catch(e => {
        console.log(e);
    });


function Search() {
    axios.get('http://localhost:8080/article/detail', {
        params: {
            id: SearchCondition.value
        }
    })
        .then(r => {
            articleList.value = [r.data.data];
        })
        .catch(e => {
            console.log(e);
        })
}
</script>

<template>
    <div>
        <input type="text" placeholder="文章ID" v-model="SearchCondition">
        <button @click="Search">搜索</button>
        <table>
            <tr>
                <th>ID</th>
                <th>标题</th>
                <th>内容</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
            </tr>
            <tr v-for="article in articleList" :key="article.id">
                <td>{{ article.id }}</td>
                <td>{{ article.title }}</td>
                <td>{{ article.content }}</td>
                <td>{{ article.state }}</td>
                <td>{{ article.createTime }}</td>
                <td>
                    <button>编辑</button>
                    <button>删除</button>
                </td>
            </tr>
        </table>
    </div>
</template>
