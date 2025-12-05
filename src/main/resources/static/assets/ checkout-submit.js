/**
 * 完整的结算 -> 创建支付单 -> 跳转到支付网关 的前端处理脚本
 *
 * 说明：
 * - 依赖项目已有的 ApiClient（assets/app.js）并使用其 checkoutItems 方法下单。
 * - 2025-12-05 更新：已适配后端批量下单原子接口，所有商品在一个事务中提交。
 * - 在下单成功后调用后端 /payment/create 接口生成支付单。
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
            // 注意：后端应该校验此金额与订单总额是否一致，前端传此值主要用于记录或二次确认
            amount: amountStr,
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
            // STEP 1: 批量下单（原子操作：要么全成功，要么全失败）
            const toCheckout = items.map(it => ({ productId: it.productId, qty: it.qty }));

            // 调用新版 app.js 中的 checkoutItems，它现在只会发一次网络请求
            const orderResult = await api.checkoutItems(userId, toCheckout);

            // 检查结果：因为是原子事务，如果 success 为空，说明整个事务回滚了
            if (!orderResult || !orderResult.success || orderResult.success.length === 0) {
                // 失败处理
                if (orderResult.failed && orderResult.failed.length > 0) {
                    // 直接取出第一个失败原因即可（因为事务回滚通常是因为某一个商品库存不足导致整体失败）
                    resultEl.textContent = '提交失败：' + orderResult.failed[0].reason;
                } else {
                    resultEl.textContent = '未能创建订单，请重试';
                }
                return; // 流程终止
            }

            // STEP 2: 调用后端创建支付单
            const successOrderIds = orderResult.success.map(s => s.orderId);
            const paymentResp = await createPaymentOnServer(userId, successOrderIds, amountStr, 'ALIPAY', address);

            // STEP 3: 跳转支付网关
            if (paymentResp && paymentResp.paymentUrl) {
                window.location.href = paymentResp.paymentUrl;
                return;
            } else if (paymentResp && paymentResp.paymentNo) {
                window.location.href = '/payment/redirect/' + encodeURIComponent(paymentResp.paymentNo);
                return;
            } else {
                resultEl.textContent = '生成支付单失败，请稍后在“我的订单”中查看';
                console.warn('unexpected paymentResp', paymentResp);
                return;
            }

        } catch (err) {
            console.error('checkout/pay error', err);
            resultEl.textContent = '提交异常：' + (err.message || '未知错误');
        } finally {
            btn.disabled = false;
            btn.textContent = '提交订单并支付';
        }
    }

    // 绑定事件
    document.getElementById('btn-pay').addEventListener('click', handleCheckoutClick);
})();