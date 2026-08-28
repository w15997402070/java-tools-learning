package com.example.mockito.service;

/**
 * 库存服务客户端接口。
 */
public interface InventoryClient {

    /**
     * 锁定指定商品的库存。
     *
     * @param sku 商品 SKU
     * @param quantity 数量
     * @return 是否锁定成功
     */
    boolean lockStock(String sku, int quantity);

    /**
     * 释放库存。
     *
     * @param sku 商品 SKU
     * @param quantity 数量
     */
    void releaseStock(String sku, int quantity);
}
