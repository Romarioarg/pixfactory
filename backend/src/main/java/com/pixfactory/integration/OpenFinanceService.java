package com.pixfactory.integration;

import java.util.List;
import java.util.Map;

public interface OpenFinanceService {
    List<Map<String, Object>> institutions();
    Map<String, Object> snapshot(String institutionCode);
}
