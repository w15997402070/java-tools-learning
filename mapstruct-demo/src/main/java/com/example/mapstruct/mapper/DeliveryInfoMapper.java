package com.example.mapstruct.mapper;

import com.example.mapstruct.dto.DeliveryInfoDTO;
import com.example.mapstruct.entity.Address;
import com.example.mapstruct.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 配送信息映射器（多源合并映射）
 *
 * <p>演示将多个对象（User + Address）的字段合并到一个 DTO 中。
 * 这是 MapStruct 区别于 BeanUtils 的核心能力之一。
 */
@Mapper
public interface DeliveryInfoMapper {

    DeliveryInfoMapper INSTANCE = Mappers.getMapper(DeliveryInfoMapper.class);

    /**
     * 多源映射：User + Address -> DeliveryInfoDTO
     *
     * <p>当方法有多个参数时，MapStruct 会尝试从每个参数中匹配目标字段。
     * 如果字段名在两个源对象中都存在（如 phone），需用 "参数名.字段名" 明确指定来源。
     *
     * <p>expression 示例：拼接完整地址字符串。
     */
    @Mapping(source = "user.realName", target = "recipientName")
    @Mapping(source = "user.phone", target = "phone")
    @Mapping(source = "address.province", target = "province")
    @Mapping(source = "address.city", target = "city")
    @Mapping(source = "address.district", target = "district")
    @Mapping(source = "address.detail", target = "detail")
    @Mapping(source = "address.zipCode", target = "zipCode")
    @Mapping(target = "fullAddress",
            expression = "java(buildFullAddress(address))")
    DeliveryInfoDTO toDTO(User user, Address address);

    /**
     * 构建完整地址字符串
     */
    default String buildFullAddress(Address address) {
        if (address == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (address.getProvince() != null) sb.append(address.getProvince());
        if (address.getCity() != null) sb.append(address.getCity());
        if (address.getDistrict() != null) sb.append(address.getDistrict());
        if (address.getDetail() != null) sb.append(address.getDetail());
        return sb.toString();
    }
}
