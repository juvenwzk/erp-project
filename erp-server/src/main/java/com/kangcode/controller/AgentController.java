package com.kangcode.controller;

import com.kangcode.Service.AgentService;
import com.kangcode.pojo.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        String userIdStr = (String) httpRequest.getAttribute("userId");
        Integer userId = userIdStr != null ? Integer.parseInt(userIdStr) : null;
        if (userId == null) {
            return Result.error("请先登录后再使用 AI 助手");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");
        if (messages == null || messages.isEmpty()) {
            return Result.error("消息不能为空");
        }

        log.info("[Agent] chat userId={} messageCount={}", userId, messages.size());
        String reply = agentService.chat(messages, userId);
        return Result.success(Map.of("reply", reply));
    }
}
