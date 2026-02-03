package com.workflow.service.listener;

import com.workflow.service.dto.ResolutionRequest;
import com.workflow.service.integration.UserAdapterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;
import org.flowable.common.engine.api.delegate.Expression;

import java.util.List;
import java.math.BigDecimal;

@Component("matrixAssignmentListener")
@RequiredArgsConstructor
@Slf4j
public class MatrixAssignmentListener implements TaskListener {

    private final UserAdapterClient userAdapterClient;
    private final com.workflow.service.service.CalendarService calendarService;

    // Injected via Field Extension
    private Expression role;

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            String roleCode = (String) role.getValue(delegateTask);
            log.info("Executing Matrix Assignment for Role: {}", roleCode);

            // Build Request from Process Variables
            ResolutionRequest req = new ResolutionRequest();
            req.setRole(roleCode); // Fixed: String setter

            // Region
            Object scopeRegion = delegateTask.getVariable("scopeRegion");
            if (scopeRegion != null) {
                req.setRegion(scopeRegion.toString()); // Fixed: String setter
            }

            // Product
            Object product = delegateTask.getVariable("product");
            if (product != null) {
                req.setProduct(product.toString()); // Fixed: String setter
            }

            // Amount
            Object amount = delegateTask.getVariable("amount");
            if (amount != null) {
                try {
                    req.setAmount(new BigDecimal(amount.toString()));
                } catch (Exception e) {
                    log.warn("Invalid amount variable", e);
                }
            }

            List<String> candidates = userAdapterClient.resolveUsers(req);

            if (candidates == null || candidates.isEmpty()) {
                log.warn("No candidates found for Matrix Rule: {}", roleCode);
                return;
            }

            // DELEGATION LOGIC: Check substitutes for all candidates
            List<String> effectiveCandidates = candidates.stream()
                    .map(calendarService::getEffectiveAssignee)
                    .distinct()
                    .toList();

            if (effectiveCandidates.size() == 1) {
                delegateTask.setAssignee(effectiveCandidates.get(0));
            } else {
                delegateTask.addCandidateUsers(effectiveCandidates);
            }
            log.info("Matrix Assignment: Found {} candidates (Delegation applied)", effectiveCandidates.size());

        } catch (Exception e) {
            log.error("Failed to execute Matrix assignment", e);
        }
    }
}
