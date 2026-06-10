package com.kangcode.Service;

import java.util.List;
import java.util.Map;

public interface AgentService {
    String chat(List<Map<String, String>> messages, Integer userId);
}
