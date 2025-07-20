// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function () {
    // 移动端菜单切换
    const mobileMenuBtn = document.querySelector('.mobile-menu-btn');
    const navbar = document.querySelector('.navbar');
    const closeMobileMenu = document.getElementById('closeMobileMenu');
    const overlay = document.getElementById('overlay');

    // 打开移动菜单
    function openMobileMenu() {
        mobileMenu.classList.add('active');
        overlay.classList.add('active');
        document.body.style.overflow = 'hidden'; // 防止背景滚动
    }

    // 关闭移动菜单
    function closeMobileMenuFn() {
        mobileMenu.classList.remove('active');
        overlay.classList.remove('active');
        document.body.style.overflow = ''; // 恢复滚动
    }

    if (mobileMenuBtn) {
        mobileMenuBtn.addEventListener('click', openMobileMenu);
    }

    if (closeMobileMenu) {
        closeMobileMenu.addEventListener('click', closeMobileMenuFn);
    }

    if (overlay) {
        overlay.addEventListener('click', closeMobileMenuFn);
    }

    // 点击菜单项也关闭菜单
    const mobileMenuItems = document.querySelectorAll('.mobile-navbar a');
    mobileMenuItems.forEach(item => {
        item.addEventListener('click', closeMobileMenuFn);
    });

    if (mobileMenuBtn && navbar) {
        mobileMenuBtn.addEventListener('click', function () {
            navbar.classList.toggle('active');
        });
    }

    // 搜索功能模拟
    const searchForm = document.querySelector('.search-bar');
    if (searchForm) {
        searchForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const searchInput = this.querySelector('input');
            const searchTerm = searchInput.value.trim();

            if (searchTerm) {
                alert(`搜索: ${searchTerm}`);
                searchInput.value = '';
            }
        });
    }

    // 订阅功能
    const subscribeForm = document.querySelector('.subscribe-form');
    if (subscribeForm) {
        subscribeForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const emailInput = this.querySelector('input[type="email"]');
            const email = emailInput.value.trim();

            if (email && validateEmail(email)) {
                alert(`感谢订阅! 我们将发送更新到: ${email}`);
                emailInput.value = '';
            } else {
                alert('请输入有效的邮箱地址');
            }
        });
    }

    // 邮箱验证函数
    function validateEmail(email) {
        const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        return re.test(String(email).toLowerCase());
    }

    // 阅读更多按钮动画
    const readMoreButtons = document.querySelectorAll('.read-more');
    readMoreButtons.forEach(button => {
        button.addEventListener('mouseenter', function () {
            this.querySelector('i').style.transform = 'translateX(3px)';
        });

        button.addEventListener('mouseleave', function () {
            this.querySelector('i').style.transform = 'translateX(0)';
        });
    });
});