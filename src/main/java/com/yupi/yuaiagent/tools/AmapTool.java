package com.yupi.yuaiagent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 高德地图工具：根据地点/地址返回「地址文本 + 静态地图图片 URL」
 * 使用高德 Web 服务：地理编码 + 静态图接口
 */
public class AmapTool {

    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";
    private static final String STATIC_MAP_BASE = "https://restapi.amap.com/v3/staticmap";

    private final String apiKey;

    public AmapTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "根据地点名称或地址查询高德地图，返回详细地址和静态地图图片链接。输入可以是：地址（如北京市朝阳区阜通东大街6号）、地名（如天安门）、商圈等。")
    public String getMapAddressAndImage(
            @ToolParam(description = "要查询的地点名称或结构化地址") String address) {
        if (apiKey == null || apiKey.isBlank()) {
            return "高德地图未配置 API Key，请在配置中设置 amap.web-api-key。";
        }
        try {
            // 1. 地理编码：地址 -> 经纬度 + 格式化地址
            Map<String, Object> geoParam = new HashMap<>();
            geoParam.put("key", apiKey);
            geoParam.put("address", address);
            String geoResponse = HttpUtil.get(GEOCODE_URL, geoParam);
            JSONObject geoJson = JSONUtil.parseObj(geoResponse);
            if (!"1".equals(geoJson.getStr("status"))) {
                return "地理编码失败：" + geoJson.getStr("info", "未知错误");
            }
            var geocodes = geoJson.getJSONArray("geocodes");
            if (geocodes == null || geocodes.isEmpty()) {
                return "未找到该地址或地点，请换一个更具体的描述。";
            }
            JSONObject first = geocodes.getJSONObject(0);
            String location = first.getStr("location");   // "经度,纬度"
            String formattedAddress = first.getStr("formatted_address");
            if (location == null || location.isBlank()) {
                return "解析到的地址：" + formattedAddress + "，但无法获取坐标。";
            }

            // 2. 拼接静态图 URL（该 URL 可直接作为 <img src="..."> 使用）
            String staticMapUrl = STATIC_MAP_BASE + "?location=" + URLEncoder.encode(location, StandardCharsets.UTF_8)
                    + "&zoom=15&size=600*400&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

            return "地址：" + formattedAddress + "\n地图图片（可直接在浏览器或前端 img 中打开）：" + staticMapUrl;
        } catch (Exception e) {
            return "查询高德地图时出错：" + e.getMessage();
        }
    }
}
