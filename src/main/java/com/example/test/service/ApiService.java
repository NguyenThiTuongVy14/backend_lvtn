package com.example.test.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ApiService {

    @Autowired
    private RestTemplate restTemplate;

    public Map<String, Object> postData(String url, JSONObject body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, request, Map.class
        );

        return response.getBody(); // Parsed into LinkedHashMap, List, etc.
    }

}
