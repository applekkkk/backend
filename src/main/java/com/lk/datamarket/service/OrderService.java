package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.DataProduct;
import com.lk.datamarket.domain.Order;
import com.lk.datamarket.domain.User;
import com.lk.datamarket.mapper.DataProductMapper;
import com.lk.datamarket.mapper.OrderMapper;
import com.lk.datamarket.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private static final String ORDER_PURCHASE_PREFIX = "购买数据:";
    private static final String ORDER_ADMIN_GRANT_PREFIX = "管理员授权购买:";
    private static final String ORDER_SALE_INCOME_PREFIX = "数据销售收入:";
    private static final String ORDER_ADMIN_REFUND_PREFIX = "管理员申诉退款:";
    private static final String ORDER_TASK_ACCEPT_PREFIX = "承接任务记录:";
    private static final String ORDER_TASK_PAYOUT_PREFIX = "任务结算支出:";
    private static final String ORDER_TASK_INCOME_PREFIX = "任务结算收入:";
    private static final String ORDER_AI_PROCESS_PREFIX = "AI数据处理:";

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DataProductMapper dataProductMapper;

    public Result<List<Order>> getUserOrders(Long buyerId) {
        List<Order> orders = orderMapper.findByBuyerId(buyerId);
        return Result.success(orders);
    }

    public Result<List<Order>> getAllOrders() {
        List<Order> orders = orderMapper.findAll();
        return Result.success(orders);
    }

    @Transactional
    public Result<String> createOrder(Order order) {
        return createOrderInternal(order, false);
    }

    @Transactional
    public Result<String> createOrderAllowNegative(Order order) {
        return createOrderInternal(order, true);
    }

    private Result<String> createOrderInternal(Order order, boolean allowNegative) {
        if (order == null || order.getBuyerId() == null) {
            return Result.error("用户不存在");
        }

        User buyer = userMapper.findById(order.getBuyerId());
        if (buyer == null) {
            return Result.error("用户不存在");
        }

        DataProduct product = null;
        if (order.getProductId() != null && order.getProductId() > 0) {
            product = dataProductMapper.findById(order.getProductId());
        }

        String orderName = order.getProductName() == null ? "" : order.getProductName().trim();
        boolean isDataPurchase = isDataPurchaseOrder(order, product, orderName);

        if (isDataPurchase) {
            if (product == null) {
                return Result.error("数据不存在");
            }
            if (product.getAuthorId() != null && product.getAuthorId().equals(order.getBuyerId())) {
                return Result.error("不能购买自己的数据");
            }
            int existed = orderMapper.countPurchasedByUserAndProduct(order.getBuyerId(), order.getProductId());
            if (existed > 0) {
                return Result.error("该数据已购买");
            }
        }

        int amount = order.getAmount() == null ? 0 : order.getAmount();
        int current = buyer.getPoints() == null ? 0 : buyer.getPoints();
        int next = current + amount;
        if (!allowNegative && next < 0) {
            return Result.error("积分不足");
        }

        order.setOrderNo(newOrderNo());
        order.setStatus(1);
        orderMapper.insert(order);

        buyer.setPoints(next);
        userMapper.update(buyer);

        if (isDataPurchase && product != null) {
            syncSellerIncomeForPurchase(product, order.getBuyerId(), amount, orderName);
        }
        return Result.success(order.getOrderNo());
    }

    @Transactional
    public Result<String> adminSetPurchaseStatus(Long buyerId, Long productId, Boolean purchased) {
        if (buyerId == null || productId == null || purchased == null) {
            return Result.error("参数错误");
        }

        User user = userMapper.findById(buyerId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        DataProduct product = dataProductMapper.findById(productId);
        if (product == null) {
            return Result.error("数据不存在");
        }

        if (Boolean.TRUE.equals(purchased)) {
            int existed = orderMapper.countPurchasedByUserAndProduct(buyerId, productId);
            if (existed > 0 || (product.getAuthorId() != null && product.getAuthorId().equals(buyerId))) {
                return Result.success("状态未变化");
            }

            Order grantOrder = new Order();
            grantOrder.setOrderNo(newOrderNo());
            grantOrder.setBuyerId(buyerId);
            grantOrder.setProductId(productId);
            grantOrder.setProductName(ORDER_ADMIN_GRANT_PREFIX + safeProductName(product));
            grantOrder.setAmount(0);
            grantOrder.setStatus(1);
            orderMapper.insert(grantOrder);
            return Result.success("已修改为已购买");
        }

        Integer sumAmount = orderMapper.sumActivePurchaseAmountByUserAndProduct(buyerId, productId);
        int affected = orderMapper.deactivatePurchaseByUserAndProduct(buyerId, productId);
        if (affected <= 0) {
            return Result.success("状态未变化");
        }

        int paidAmount = sumAmount == null ? 0 : sumAmount;
        int refund = Math.max(0, -paidAmount);
        if (refund > 0) {
            Order refundOrder = new Order();
            refundOrder.setBuyerId(buyerId);
            refundOrder.setProductId(productId);
            refundOrder.setProductName(ORDER_ADMIN_REFUND_PREFIX + safeProductName(product));
            refundOrder.setAmount(refund);
            Result<String> refundResult = createOrderInternal(refundOrder, true);
            if (refundResult.getCode() != 200) {
                return Result.error(refundResult.getMessage());
            }
        }

        rollbackSellerIncomeForPurchase(product, buyerId);
        if (refund > 0) {
            return Result.success("已修改为未购买，已退回" + refund + "积分");
        }
        return Result.success("已修改为未购买");
    }

    private boolean isDataPurchaseOrder(Order order, DataProduct product, String orderName) {
        if (order.getProductId() == null || order.getProductId() <= 0) {
            return false;
        }
        if (product == null) {
            return false;
        }
        if (orderName.startsWith(ORDER_PURCHASE_PREFIX) || orderName.startsWith(ORDER_ADMIN_GRANT_PREFIX)) {
            return true;
        }

        int amount = order.getAmount() == null ? 0 : order.getAmount();
        if (amount >= 0) {
            return false;
        }

        if (orderName.startsWith(ORDER_TASK_ACCEPT_PREFIX)
                || orderName.startsWith(ORDER_TASK_PAYOUT_PREFIX)
                || orderName.startsWith(ORDER_TASK_INCOME_PREFIX)
                || orderName.startsWith(ORDER_AI_PROCESS_PREFIX)
                || orderName.startsWith(ORDER_SALE_INCOME_PREFIX)
                || orderName.startsWith(ORDER_ADMIN_REFUND_PREFIX)) {
            return false;
        }
        return true;
    }

    private void syncSellerIncomeForPurchase(DataProduct product, Long buyerId, int buyerAmount, String orderName) {
        Long sellerId = product.getAuthorId();
        if (sellerId == null || sellerId.equals(buyerId)) {
            return;
        }

        int income = Math.max(0, -buyerAmount);
        if (income <= 0) {
            return;
        }

        User seller = userMapper.findById(sellerId);
        if (seller == null) {
            return;
        }

        Order sellerIncomeOrder = new Order();
        sellerIncomeOrder.setOrderNo(newOrderNo());
        sellerIncomeOrder.setBuyerId(sellerId);
        sellerIncomeOrder.setProductId(product.getId());
        sellerIncomeOrder.setProductName(buildSaleIncomeOrderName(orderName, buyerId, product));
        sellerIncomeOrder.setAmount(income);
        sellerIncomeOrder.setStatus(1);
        orderMapper.insert(sellerIncomeOrder);

        int sellerCurrent = seller.getPoints() == null ? 0 : seller.getPoints();
        seller.setPoints(sellerCurrent + income);
        userMapper.update(seller);
    }

    private void rollbackSellerIncomeForPurchase(DataProduct product, Long purchaseBuyerId) {
        Long sellerId = product.getAuthorId();
        if (sellerId == null || sellerId.equals(purchaseBuyerId)) {
            return;
        }

        User seller = userMapper.findById(sellerId);
        if (seller == null) {
            return;
        }

        Integer incomeSum = orderMapper.sumActiveSaleIncomeBySellerAndProduct(sellerId, product.getId(), purchaseBuyerId);
        int affected = orderMapper.deactivateSaleIncomeBySellerAndProduct(sellerId, product.getId(), purchaseBuyerId);
        if (affected <= 0) {
            return;
        }

        int income = incomeSum == null ? 0 : incomeSum;
        if (income <= 0) {
            return;
        }
        int sellerCurrent = seller.getPoints() == null ? 0 : seller.getPoints();
        seller.setPoints(sellerCurrent - income);
        userMapper.update(seller);
    }

    private String buildSaleIncomeOrderName(String purchaseOrderName, Long purchaseBuyerId, DataProduct product) {
        String productName = safeProductName(product);
        if (purchaseOrderName != null && purchaseOrderName.trim().startsWith(ORDER_PURCHASE_PREFIX)) {
            productName = purchaseOrderName.trim().substring(ORDER_PURCHASE_PREFIX.length()).trim();
            if (productName.isEmpty()) {
                productName = safeProductName(product);
            }
        }
        return ORDER_SALE_INCOME_PREFIX + productName + "[buyerId=" + purchaseBuyerId + "]";
    }

    private String safeProductName(DataProduct product) {
        String name = product.getName();
        return (name == null || name.trim().isEmpty()) ? "数据" : name.trim();
    }

    private String newOrderNo() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
