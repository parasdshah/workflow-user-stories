package com.workflow.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphDTO {

    @Builder.Default
    private List<NodeDTO> nodes = new ArrayList<>();

    @Builder.Default
    private List<EdgeDTO> edges = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeDTO {
        private String id;
        private String label;
        private String type; // start, end, userTask, callActivity, group
        private String parentId; // For grouping (SubFlow)
        private Object data; // Extra metadata
        private Position position;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeDTO {
        private String id;
        private String source;
        private String target;
        private String label;
        private String type; // sequence, flow, error
        private Boolean animated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private double x;
        private double y;
    }
}
