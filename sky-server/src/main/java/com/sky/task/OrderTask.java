package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单（超时未支付）
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void processTimeoutOrder() {
        log.info("处理超时订单：{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().minusMinutes(15);

        // 获取超时订单
        List<Orders> list = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT, time);

        if (list != null && list.size() > 0) {
            for (Orders order : list) {
                // 更新订单状态为取消
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时，自动取消！");
                order.setCancelTime(LocalDateTime.now());

                orderMapper.update(order);
            }
        }
    }

    /**
     * 自动完成派送中订单（商家忘记点完成）
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行(打烊之后)
    public void autoCompleteDeliveringOrder() {
        List<Orders> list = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().minusMinutes(60));
        if (list != null && list.size() > 0) {
            for (Orders order : list) {
                // 更新订单状态为完成
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }

    }


}
