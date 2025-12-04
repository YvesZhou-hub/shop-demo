/**
 * 完整的结算 -> 创建支付单 -> 跳转到支付网关 的前端处理脚本
 *
 * 说明：
 * - 依赖项目已有的 ApiClient（assets/app.js）并使用其 checkoutItems 方法逐项下单。
 * - 在下单成功（result.success 非空）后调用后端 /payment/create 接口生成支付单（paymentUrl 或 paymentNo）。
 * - 若后端返回 paymentUrl，则直接跳转到该 URL；若只返回 paymentNo，则跳转到后端重定向接口 /payment/redirect/{paymentNo}。
 * - 金额使用字符串表示（保留2位小数）以避免 JS 浮点精度问题；如果你后端需要分（cents）请调整 payload。
 *
 * 集成方法：
 * 1. 把此文件保存为 src/main/resources/static/assets/checkout-submit.js
 * 2. 在 checkout.html 中在引入 assets/app.js 之后加入：
 *    <script src="assets/checkout-submit.js"></script>
 * 3. 该脚本会自动绑定页面上 id 为 'btn-pay' 的按钮事件（与之前 checkout.html 中的按钮保持一致）。
 *
 * 注意（重要）：
 * - 当前流程是“先创建订单（逐项）-> 再创建支付单 -> 跳转支付网关”。这是可行的但并非最优（如果支付创建失败或用户放弃支付，订单已创建）。
 * - 推荐后端改造：实现一次性“购物车下单并创建支付单”的原子接口（在同一事务内创建订单与 payment），并直接返回支付 URL。
 */

(async function () {
    // 简单初始化：若页面没有相关元素，脚本不做任何事
    if (!document.getElementById('btn-pay')) return;

    const api = new ApiClient();

    function getSelectedItemsFromSessionOrCart() {
        const selRaw = sessionStorage.getItem('checkout_selection');
        let selectedIds = [];
        try {
            selectedIds = selRaw ? JSON.parse(selRaw) : [];
        } catch (e) {
            selectedIds = [];
        }
        const cart = api.getCart();
        // 保证 productId 字符/数字都可以匹配
        return cart.filter(ci =>
            selectedIds.includes(String(ci.productId)) || selectedIds.includes(ci.productId)
        );
    }

    function computeTotalString(items) {
        // 以分为单位累加，避免浮点问题
        const totalCents = items.reduce((sum, it) => {
            const price = Number(it.price || 0);
            const qty = Number(it.qty || 0);
            const cents = Math.round(price * 100) * qty;
            return sum + cents;
        }, 0);
        return (totalCents / 100).toFixed(2); // 返回字符串 "123.45"
    }

    async function createPaymentOnServer(userId, orderIds, amountStr, provider = 'ALIPAY', address = '') {
        const payload = {
            userId: userId,
            orderIds: orderIds,
            amount: amountStr, // 建议使用字符串 "123.45"
            provider: provider,
            address: address
        };

        const resp = await fetch('/payment/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        // 如果后端返回非 JSON，handle accordingly
        const text = await resp.text();
        let json = null;
        try {
            json = text ? JSON.parse(text) : null;
        } catch (e) {
            throw new Error('支付下单返回非 JSON: ' + text);
        }

        if (!resp.ok) {
            const msg = (json && json.msg) ? json.msg : `请求失败 ${resp.status}`;
            const err = new Error(msg);
            err.payload = json;
            throw err;
        }
        return json; // 期望 { paymentNo, paymentUrl }
    }

    async function handleCheckoutClick(e) {
        e.preventDefault();
        const btn = document.getElementById('btn-pay');
        const resultEl = document.getElementById('checkout-result');
        resultEl.textContent = '';

        // 读取用户输入与选中项
        const userId = Number(document.getElementById('checkout-userId').value || 0);
        const address = document.getElementById('checkout-address').value || '';
        if (!userId || userId <= 0) {
            alert('请输入有效用户 ID');
            return;
        }

        const items = getSelectedItemsFromSessionOrCart();
        if (!items || items.length === 0) {
            alert('没有可结算的商品');
            return;
        }

        // 计算金额（字符串）
        const amountStr = computeTotalString(items);

        // 禁用按钮防重复提交
        btn.disabled = true;
        btn.textContent = '提交中…';

        try {
            // STEP 1: 逐项下单（调用现有 API：createOrder via api.checkoutItems）
            // 注意：api.checkoutItems 会逐条调用后端 /order/add 并返回 { success: [{productId, orderId}], failed: [...] }
            const toCheckout = items.map(it => ({ productId: it.productId, qty: it.qty }));
            const orderResult = await api.checkoutItems(userId, toCheckout);

            if (!orderResult || (!orderResult.success || orderResult.success.length === 0)) {
                // 没有成功下单项
                if (orderResult && orderResult.failed && orderResult.failed.length > 0) {
                    resultEl.textContent = '提交失败：' + orderResult.failed.map(f => `${f.productId}:${f.reason}`).join('; ');
                } else {
                    resultEl.textContent = '未能创建任何订单，请重试';
                }
                return;
            }

            // STEP 2: 调用后端创建支付单（将成功的 orderId 列表传给后端）
            const successOrderIds = orderResult.success.map(s => s.orderId);
            // 调用后端创建 payment（后端会返回 paymentUrl 或 paymentNo）
            const paymentResp = await createPaymentOnServer(userId, successOrderIds, amountStr, 'ALIPAY', address);

            // STEP 3: 如果后端返回 paymentUrl，直接跳转到支付网关；否则跳转到后端 redirect 接口
            if (paymentResp && paymentResp.paymentUrl) {
                // 创建支付单成功，之后跳转到第三方支付页面（真实跳转）
                window.location.href = paymentResp.paymentUrl;
                return;
            } else if (paymentResp && paymentResp.paymentNo) {
                window.location.href = '/payment/redirect/' + encodeURIComponent(paymentResp.paymentNo);
                return;
            } else {
                // 后端没有返回支付 URL
                // 建议：如果创建 payment 成功但没有返回 URL，你可以保存 paymentNo 并跳转到后续的“支付状态查询/等待页面”
                resultEl.textContent = '生成支付单失败，请稍后在“我的订单”中查看或联系客服';
                console.warn('unexpected paymentResp', paymentResp);
                return;
            }
        } catch (err) {
            console.error('checkout/pay error', err);
            resultEl.textContent = '提交异常：' + (err.message || '未知错误');
            // 如果部分订单已创建但 payment 创建失败，建议提示用户并保留购物车/订单信息以便手工处理
        } finally {
            btn.disabled = false;
            btn.textContent = '提交订单并支付（真实跳转）';
        }
    }

    // 绑定事件
    document.getElementById('btn-pay').addEventListener('click', handleCheckoutClick);
})();