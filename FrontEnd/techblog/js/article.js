import { article } from './article.js';

// 文章详情页脚本
document.addEventListener('DOMContentLoaded', function () {
    // 代码高亮（实际项目中可使用Prism.js等库）
    document.querySelectorAll('pre code').forEach((block) => {
        block.style.padding = '20px';
        block.style.display = 'block';
        block.style.overflowX = 'auto';
    });

    // 评论表单提交
    const commentForm = document.querySelector('.comment-form form');
    if (commentForm) {
        commentForm.addEventListener('submit', function (e) {
            e.preventDefault();
            alert('评论已提交！审核通过后会显示');
            this.reset();
        });
    }

    // 回复按钮功能
    const replyButtons = document.querySelectorAll('.reply-btn');
    replyButtons.forEach(button => {
        button.addEventListener('click', function () {
            const commentAuthor = this.closest('.comment').querySelector('.comment-author').textContent;
            const textarea = document.getElementById('comment-content');
            textarea.value = `@${commentAuthor} `;
            textarea.focus();
        });
    });
});