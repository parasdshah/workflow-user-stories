package com.workflow.service.integration;

import com.workflow.service.dto.ResolutionRequest;
import com.workflow.service.dto.ResolutionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAdapterClient {

    private final RestTemplate restTemplate;

    @Value("${workflow.user-adapter.url}")
    private String adapterUrl;

    public List<String> resolveUsers(ResolutionRequest request) {
        try {
            String url = adapterUrl + "/resolve-users";
            log.info("Calling User Adapter at: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ResolutionRequest> entity = new HttpEntity<>(request, headers);

            ResolutionResponse response = restTemplate.postForObject(url, entity, ResolutionResponse.class);

            if (response != null && response.getUserIds() != null) {
                return response.getUserIds();
            }
        } catch (Exception e) {
            log.error("Failed to resolve users via adapter: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public List<String> getRoleMembers(String role) {
        try {
            String url = adapterUrl + "/role-members?role=" + role;
            log.info("Calling User Adapter for Role Members at: {}", url);
            
            // Assuming response is List<String> directly
            @SuppressWarnings("unchecked")
            List<String> response = restTemplate.getForObject(url, List.class);
            
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to get role members via adapter: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public java.util.Map<String, String> searchUsers(List<String> userIds) {
        try {
            String url = adapterUrl + "/users/search";
            log.info("Calling User Adapter for Batch User Search at: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<String>> entity = new HttpEntity<>(userIds, headers);

            // Need custom response type or use List<Map>
            // Returning Map<UserId, FullName>
            List<java.util.Map<String, String>> response = restTemplate.postForObject(url, entity, List.class);
            
            if (response != null) {
                java.util.Map<String, String> names = new java.util.HashMap<>();
                for (java.util.Map<String, String> user : response) {
                    names.put(user.get("userId"), user.get("fullName"));
                }
                return names;
            }
        } catch (Exception e) {
            log.error("Failed to search users via adapter: {}", e.getMessage(), e);
        }
        return Collections.emptyMap();
    }
}
