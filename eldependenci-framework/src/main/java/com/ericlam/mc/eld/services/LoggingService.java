package com.ericlam.mc.eld.services;


import com.ericlam.mc.eld.misc.DebugLogger;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 日誌輸出服務
 */
public interface LoggingService {

    /**
     * 獲取日誌輸出
     * @param cls 物件類別
     * @return debug 日誌輸出
     */
    DebugLogger getLogger(Class<?> cls);

    /**
     * 獲取日誌輸出
     * @param name 日誌名稱
     * @return debug 日誌輸出
     */
    DebugLogger getLogger(String name);

    /**
     * 更改調試模式並保存配置，並對目前的 DebugLogger 即時生效。
     *
     * @param debugEnabled 是否啟用調試模式，true為啟用，false為禁用
     * @throws IOException 如果在保存配置時發生I/O錯誤
     */
    void changeDebugMode(boolean debugEnabled) throws IOException;

}
