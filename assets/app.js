// ApiClient + 购物车管理（localStorage）
// 说明：把此文件放到 src/main/resources/static/assets/app.js，覆盖原有 app.js

class ApiClient {
    constructor(base = '') {
        this.base = base;
        this.cartKey = 'shop_demo_cart_v1'; // localStorage key
    }

    // ---------------------
    // 网络请求基础
    // ---------------------
    async request(path, opts = {}) {
        const url = this.base + path;
        let res;
        try {
            res = await fetch(url, opts);
        } catch (networkErr) {
            throw new Error('网络错误或无法连接到后端: ' + networkErr.message);
        }

        const text = await res.text().catch(() => '');
        let json = null;
        try {
            json = text ? JSON.parse(text) : null;
        } catch (e) {
            if (!res.ok) throw new Error(`请求失败: ${res.status} ${res.statusText}`);
            throw new Error('后端返回不可解析的 JSON');
        }

        if (!res.ok) {
            const message = (json && json.msg) ? json.msg : `${res.status} ${res.statusText}`;
            const err = new Error(message);
            err.status = res.status;
            err.payload = json;
            throw err;
        }

        return json; // 按后端约定 {code, msg, data}
    }

    // ---------------------
    // 产品 / 订单 API
    // ---------------------
    async getAllProducts() {
        return this.request('/product/all', { method: 'GET' });
    }

    async getProductById(id) {
        return this.request('/product/' + encodeURIComponent(id), { method: 'GET' });
    }

    async addProduct(product) {
        return this.request('/product/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(product)
        });
    }

    async createOrder(orderPayload) {
        return this.request('/order/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(orderPayload)
        });
    }

    async getOrdersByUserId(userId) {
        return this.request('/order/user/' + encodeURIComponent(userId), { method: 'GET' });
    }

    // ---------------------
    // 购物车逻辑（客户端实现，存 localStorage）
    // 结构： [{ productId, productName, price, stock, qty }]
    // ---------------------
    _readCart() {
        try {
            const raw = localStorage.getItem(this.cartKey);
            return raw ? JSON.parse(raw) : [];
        } catch (e) {
            console.error('读取购物车失败', e);
            return [];
        }
    }

    _writeCart(arr) {
        try {
            localStorage.setItem(this.cartKey, JSON.stringify(arr));
            // 触发 badge 更新事件（全局）
            document.dispatchEvent(new CustomEvent('cart:updated'));
        } catch (e) {
            console.error('写入购物车失败', e);
        }
    }

    getCart() {
        return this._readCart();
    }

    clearCart() {
        localStorage.removeItem(this.cartKey);
        document.dispatchEvent(new CustomEvent('cart:updated'));
    }

    addToCart(item) {
        // item: { productId, productName, price, stock, qty }
        if (!item || !item.productId) return false;
        const cart = this._readCart();
        const idx = cart.findIndex(ci => String(ci.productId) === String(item.productId));
        if (idx >= 0) {
            // 累加数量（但不超过库存）
            cart[idx].qty = Math.min((cart[idx].qty || 0) + (item.qty || 1), item.stock ?? Number.MAX_SAFE_INTEGER);
        } else {
            cart.push({
                productId: item.productId,
                productName: item.productName,
                price: item.price,
                stock: item.stock ?? 0,
                qty: item.qty || 1
            });
        }
        this._writeCart(cart);
        return true;
    }

    updateCartQty(productId, qty) {
        const cart = this._readCart();
        const idx = cart.findIndex(ci => String(ci.productId) === String(productId));
        if (idx >= 0) {
            cart[idx].qty = Math.max(0, Math.min(qty, cart[idx].stock ?? Number.MAX_SAFE_INTEGER));
            if (cart[idx].qty === 0) cart.splice(idx, 1);
            this._writeCart(cart);
        }
    }

    removeFromCart(productId) {
        const cart = this._readCart();
        const idx = cart.findIndex(ci => String(ci.productId) === String(productId));
        if (idx >= 0) {
            cart.splice(idx, 1);
            this._writeCart(cart);
        }
    }

    cartTotalCount() {
        const cart = this._readCart();
        return cart.reduce((s, it) => s + (it.qty || 0), 0);
    }

    cartTotalPrice() {
        const cart = this._readCart();
        return cart.reduce((s, it) => s + (Number(it.price || 0) * Number(it.qty || 0)), 0);
    }

    // 批量下单（当前实现为逐项调用后端 createOrder）
    // items: [{ productId, qty }]
    // userId: number
    // address: object (仅作记录，后端不使用)
    // 返回：{ success: [], failed: [] }
    async checkoutItems(userId, items) {
        const result = { success: [], failed: [] };
        for (const it of items) {
            try {
                const payload = {
                    userId: userId,
                    productId: it.productId,
                    num: it.qty
                };
                const r = await this.createOrder(payload);
                // 认为返回 {code:200, data:orderId}
                if (r && r.code === 200) {
                    result.success.push({ productId: it.productId, orderId: r.data });
                } else {
                    result.failed.push({ productId: it.productId, reason: r ? r.msg : '未知错误' });
                }
            } catch (e) {
                result.failed.push({ productId: it.productId, reason: e.message || '网络错误' });
            }
        }
        return result;
    }
}

// ---------------------
// 辅助函数
// ---------------------
function escapeHtml(s) {
    if (s === null || s === undefined) return '';
    return String(s)
        .replaceAll('&','&amp;')
        .replaceAll('<','&lt;')
        .replaceAll('>','&gt;')
        .replaceAll('"','&quot;')
        .replaceAll("'","&#39;");
}

// 更新页面所有 .cart-badge 显示数量
function updateCartBadges(apiClient) {
    const count = apiClient.cartTotalCount();
    document.querySelectorAll('.cart-badge').forEach(el => {
        el.textContent = count > 0 ? String(count) : '';
        el.style.display = count > 0 ? 'inline-block' : 'none';
    });
}

// 初始化：监听 cart 更新事件
(function initCartBadge() {
    const api = new ApiClient();
    updateCartBadges(api);
    document.addEventListener('cart:updated', () => updateCartBadges(api));
})();