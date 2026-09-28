package com.fitness.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色权限分配请求
 */
@Data
public class PermissionAssignRequest {

    @NotNull(message = "权限列表不能为空")
    private List<Long> permissionIds;
}
