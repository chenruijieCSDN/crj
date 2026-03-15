package com.yupi.yuaiagent.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网页搜索工具
 */
public class WebSearchTool {

    // SearchAPI 的搜索接口地址
    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";
    /** 请求超时时间（毫秒），避免 searchWeb 长时间无响应导致步骤卡住 */
    private static final int HTTP_TIMEOUT_MS = 25_000;

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Search API key not configured. Set search-api.api-key in application-local.yml (see https://www.searchapi.io/).";
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            String response = HttpRequest.get(SEARCH_API_URL)
                    .form(paramMap)
                    .timeout(HTTP_TIMEOUT_MS)
                    .execute()
                    .body();
            JSONObject jsonObject = JSONUtil.parseObj(response);
            Object organicObj = jsonObject.get("organic_results");
            if (organicObj == null) {
                Object err = jsonObject.get("error");
                if (err != null) {
                    return "Search API error: " + err + ". Check your search-api.api-key and quota.";
                }
                return "No search results found.";
            }
            List<Object> items = new ArrayList<>();
            if (organicObj instanceof JSONArray) {
                JSONArray arr = (JSONArray) organicObj;
                for (int i = 0; i < arr.size(); i++) {
                    items.add(arr.get(i));
                }
            } else if (organicObj instanceof List) {
                for (Object o : (List<?>) organicObj) {
                    items.add(o);
                }
            } else {
                return "No search results found.";
            }
            if (items.isEmpty()) {
                return "No search results found.";
            }
            int size = Math.min(5, items.size());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size; i++) {
                Object item = items.get(i);
                if (item instanceof JSONObject) {
                    sb.append(((JSONObject) item).toString());
                } else if (item instanceof Map) {
                    sb.append(JSONUtil.toJsonStr(item));
                } else {
                    sb.append(item != null ? JSONUtil.toJsonStr(item) : "null");
                }
                if (i < size - 1) {
                    sb.append(",");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error searching Baidu: " + e.getMessage();
        }
    }
}
