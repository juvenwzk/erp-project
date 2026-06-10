package com.kangcode.Controller;

import com.kangcode.Service.AgentService;
import com.kangcode.pojo.Result;
import com.kangcode.utils.JwtUtils;
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

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, Object> request,
                       @RequestHeader("Authorization") String authHeader) {
        Integer userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtils.validateToken(token)) {
                userId = jwtUtils.getUserId(token);
            }
        }
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");
        String reply = agentService.chat(messages, userId);
        return Result.success(Map.of("reply", reply));
    }
}
