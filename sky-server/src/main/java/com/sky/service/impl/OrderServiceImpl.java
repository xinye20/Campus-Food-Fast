package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WebSocketServer webSocketServer;

//    @Autowired
//    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户提交订单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        // 处理各种业务异常，地址簿为空、购物车为空···
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 向订单表中存入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
//        orders.setNumber(System.currentTimeMillis());
        orders.setNumber(UUID.randomUUID().toString());
        orders.setAddress(addressBook.getDetail());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);

        orderMapper.add(orders);

        // 向订单明细表中存入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            // 插入当前订单明细所属订单id
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.addBatch(orderDetailList);

        // 清空当前用户购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        // 封装返回结果
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .build();
    }

    /**
     * 历史订单分页查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 配置分页信息，获取分页结果
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<OrderVO> page = orderMapper.pageQuery(ordersPageQueryDTO);
        // 补充订单分页详情信息
        List<OrderVO> orders = page.getResult();
        for (OrderVO order : orders) {
            order.setOrderDetailList(orderDetailMapper.getByOrderId(order.getId()));
        }
        return new PageResult(page.getTotal(), orders);
    }

    /**
     * 查询订单详情信息
     *
     * @param id
     * @return
     */
    public OrderVO getOrderDetail(Long id) {
        // 根据id获取订单信息
        OrderVO orderVO = orderMapper.getById(id);
        // 查询补充订单详情信息
        List<OrderDetail> orderDetail = orderDetailMapper.getByOrderId(id);
        orderVO.setOrderDetailList(orderDetail);

        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    public void cancel(Long id) throws Exception {
        // 根据id查询订单
        OrderVO orderDB = orderMapper.getById(id);
        // 判断订单是否存在
        if (orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 判断订单状态，只有待付款和待接单的订单可以取消
        if (orderDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(orderDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
//            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    orderDB.getNumber(), //商户订单号
//                    orderDB.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

    }

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        // 获取用户id
        Long userId = BaseContext.getCurrentId();

        // 获取订单包含的菜品
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转化成购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 插入购物车数据
        shoppingCartMapper.addBatch(shoppingCartList);

    }

    /**
     * 查询各种状态的订单数量
     *
     * @return
     */
    public OrderStatisticsVO getStatistics() {
        // 获取各个状态订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED); //待接单
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);   // 待派送
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS); // 派送中

        //封装数据
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;
    }

    /**
     * 商家接单
     *
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        // 封装数据
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 商家拒绝接单
     *
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        // 获取订单信息
        Orders orderDB = orderMapper.getById(ordersRejectionDTO.getId());

        // 校验订单是否存在且状态为2(待接单)，否则抛出订单错误异常
        if (orderDB == null || !orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //封装数据，更新订单状态为6(已取消)
        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    public void complete(Long id) {
        // 根据id查询订单信息
        Orders OrdersDB = orderMapper.getById(id);
        // 校验订单是否存在且状态为4(派送中)，否则抛出订单错误异常
        if (OrdersDB == null || !OrdersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 封装数据，更新订单状态为5(已完成)
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 订单派送
     *
     * @param id
     */
    public void delivery(Long id) {
        // 根据id查询订单信息
        Orders OrdersDB = orderMapper.getById(id);
        // 校验订单是否存在且状态为3(已接单)，否则抛出订单错误异常
        if (OrdersDB == null || !OrdersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 封装数据，更新订单状态为4(派送中)
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .deliveryTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 商家取消订单
     *
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        // 根据id查询订单信息
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        // 获取支付状态，若已支付则需要退款
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
//            //用户已支付，需要退款
//            weChatPayUtil.refund(
//                ordersDB.getNumber(),
//                ordersDB.getNumber(),
//                new BigDecimal(0.01),
//                new BigDecimal(0.01));
            log.info("申请退款，订单id：{}", ordersCancelDTO.getId());
        }
        // 更新订单状态及信息
        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .payStatus(Orders.REFUND)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 订单支付（只实现支付的逻辑，未对接微信支付接口）
     * @param ordersPaymentDTO
     */
    public void payment(OrdersPaymentDTO ordersPaymentDTO) {
        // 根据订单号查询订单
        Orders orderDB = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders order = Orders.builder()
                .id(orderDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .payMethod(ordersPaymentDTO.getPayMethod())
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.update(order);

        // 通过webSocket向浏览器推送消息
        Map map = new HashMap();
        map.put("type", 1); // 1 表示来单提醒，2 表示客户催单
        map.put("orderId", orderDB.getId());
        map.put("content", "订单号"+ orderDB.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));

    }

    /**
     * 催单
     * @param id
     */
    public void reminder(Long id) {
        Orders orderDB = orderMapper.getById(id);

        if (orderDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 通过webSocket向浏览器推送消息
        Map map = new HashMap();
        map.put("type", 2); // 1 表示来单提醒，2 表示客户催单
        map.put("orderId", id);
        map.put("content", "订单号"+ orderDB.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }
}
