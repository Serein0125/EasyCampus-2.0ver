package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.entity.*;
import com.campus.backend.dto.MemberVO;
import com.campus.backend.dto.JoinRequestVO;
import com.campus.backend.service.impl.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v2/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService orgService;

    @PostMapping
    public Result<Organization> createOrganization(@RequestBody Organization org) {
        return Result.success(orgService.createOrganization(org, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public Result<Organization> getOrganization(@PathVariable Long id) {
        return Result.success(orgService.getOrganization(id));
    }

    @GetMapping("/my")
    public Result<List<Organization>> getMyOrganizations() {
        return Result.success(orgService.getMyOrganizations(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping
    public Result<Map<String, Object>> getOrganizationList(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int size,
                                                            @RequestParam(required = false) String keyword) {
        List<Organization> list;
        int total;
        if (keyword != null && !keyword.isEmpty()) {
            list = orgService.searchOrganizations(keyword, page, size);
            total = list.size();
        } else {
            list = orgService.getOrganizationList(page, size);
            total = orgService.getOrganizationCount();
        }
        return Result.success(Map.of("list", list, "page", page, "total", total));
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approveOrganization(@PathVariable Long id) {
        orgService.approveOrganization(id, SecurityUtils.getCurrentUserId());
        return Result.success(null);
    }

    @PutMapping("/{id}/reject")
    public Result<Void> rejectOrganization(@PathVariable Long id) {
        orgService.rejectOrganization(id, SecurityUtils.getCurrentUserId());
        return Result.success(null);
    }

    @PostMapping("/{orgId}/invite")
    public Result<OrgInvitation> inviteMember(@PathVariable Long orgId, @RequestBody Map<String, Long> body) {
        return Result.success(orgService.inviteMember(orgId, SecurityUtils.getCurrentUserId(), body.get("inviteeId")));
    }

    @PostMapping("/invitations/{code}/accept")
    public Result<Void> acceptInvitation(@PathVariable String code) {
        orgService.acceptInvitation(code, SecurityUtils.getCurrentUserId());
        return Result.success(null);
    }

    @PostMapping("/invitations/{code}/reject")
    public Result<Void> rejectInvitation(@PathVariable String code) {
        orgService.rejectInvitation(code, SecurityUtils.getCurrentUserId());
        return Result.success(null);
    }

    @GetMapping("/invitations/my")
    public Result<List<OrgInvitation>> getMyInvitations() {
        return Result.success(orgService.getMyInvitations(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/{orgId}/apply")
    public Result<Void> applyToJoin(@PathVariable Long orgId, @RequestBody Map<String, String> body) {
        orgService.applyToJoin(orgId, SecurityUtils.getCurrentUserId(), body.getOrDefault("message", ""));
        return Result.success(null);
    }

    @GetMapping("/{orgId}/requests")
    public Result<List<JoinRequestVO>> getPendingRequests(@PathVariable Long orgId) {
        return Result.success(orgService.getPendingRequests(orgId, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/requests/{requestId}/approve")
    public Result<Void> approveJoinRequest(@PathVariable Long requestId) {
        orgService.approveJoinRequest(requestId, SecurityUtils.getCurrentUserId());
        return Result.success(null);
    }

    @PutMapping("/requests/{requestId}/reject")
    public Result<Void> rejectJoinRequest(@PathVariable Long requestId) {
        orgService.rejectJoinRequest(requestId, SecurityUtils.getCurrentUserId());
        return Result.success(null);
    }

    @GetMapping("/{orgId}/members")
    public Result<List<MemberVO>> getMembers(@PathVariable Long orgId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return Result.success(orgService.getMembers(orgId, page, size));
    }

    @DeleteMapping("/{orgId}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long orgId, @PathVariable Long userId) {
        orgService.removeMember(orgId, SecurityUtils.getCurrentUserId(), userId);
        return Result.success(null);
    }

    @PutMapping("/{orgId}/members/{userId}/role")
    public Result<Void> changeMemberRole(@PathVariable Long orgId, @PathVariable Long userId,
                                         @RequestBody Map<String, String> body) {
        orgService.changeMemberRole(orgId, SecurityUtils.getCurrentUserId(), userId, body.get("role"));
        return Result.success(null);
    }

    @GetMapping("/{orgId}/my-role")
    public Result<OrgMember> getMyRole(@PathVariable Long orgId) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrgMember role = orgService.getMyRole(orgId, userId);
        return Result.success(role);
    }

    @PostMapping("/{orgId}/leave")
    public Result<Void> leaveOrganization(@PathVariable Long orgId) {
        orgService.leaveOrganization(orgId, SecurityUtils.getCurrentUserId());
        return Result.success(null);
    }

    @GetMapping("/{orgId}/audit-logs")
    public Result<List<OrgAuditLog>> getAuditLogs(@PathVariable Long orgId,
                                                   @RequestParam(defaultValue = "20") int limit) {
        return Result.success(orgService.getAuditLogs(orgId, limit));
    }
}
