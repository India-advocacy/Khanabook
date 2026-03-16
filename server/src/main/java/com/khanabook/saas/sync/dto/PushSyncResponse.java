package com.khanabook.saas.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PushSyncResponse {
    private List<Integer> successfulLocalIds;
    private List<Integer> failedLocalIds;
}
