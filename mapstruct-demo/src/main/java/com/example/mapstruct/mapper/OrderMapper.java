package com.example.mapstruct.mapper;

import com.example.mapstruct.dto.OrderDTO;
import com.example.mapstruct.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单对象映射器（进阶版本）
 *
 * <p>演示：
 * <ul>
 *   <li>嵌套对象展平映射（order.user.username -> orderDTO.username）</li>
 *   <li>数值单位转换（分 -> 元：Long -> BigDecimal）</li>
 *   <li>字段重命名（id -> orderId）</li>
 *   <li>调用自定义 default 方法进行状态码转换</li>
 *   <li>uses 属性引用其他 Mapper</li>
 * </ul>
 */
@Mapper
public interface OrderMapper {

    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    /**
     * Order 实体 -> OrderDTO
     *
     * <p>嵌套映射语法：source = "user.username" 表示从 order.getUser().getUsername() 取值。
     * 注意：若 user 为 null，MapStruct 不会抛 NPE，目标字段为 null（需自行处理空对象）。
     */
    @Mapping(source = "id", target = "orderId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.phone", target = "userPhone")
    @Mapping(target = "unitPriceYuan", expression = "java(centToYuan(order.getUnitPrice()))")
    @Mapping(target = "statusDesc", expression = "java(orderStatusToDesc(order.getStatus()))")
    OrderDTO toDTO(Order order);

    /**
     * 批量映射
     */
    List<OrderDTO> toDTOList(List<Order> orders);

    /**
     * 分 -> 元转换（Long -> BigDecimal）
     *
     * <p>金额在数据库通常以分（Long/Integer）存储，展示时需要转换为元（BigDecimal）。
     * 使用 BigDecimal 是为了避免浮点精度问题。
     */
    default BigDecimal centToYuan(Long cent) {
        if (cent == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cent).divide(new BigDecimal("100"));
    }

    /**
     * 订单状态码 -> 描述文字
     */
    default String orderStatusToDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1: return "待付款";
            case 2: return "待发货";
            case 3: return "已发货";
            case 4: return "已完成";
            case 5: return "已取消";
            default: return "未知状态(" + status + ")";
        }
    }
}
