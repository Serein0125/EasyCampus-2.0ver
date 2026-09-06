package com.campus.backend.service.impl;

import com.campus.backend.entity.*;
import com.campus.backend.dto.MemberVO;
import com.campus.backend.dto.JoinRequestVO;
import com.campus.backend.mapper.*;
import com.campus.backend.common.ErrorCode;
import com.campus.backend.exception.BusinessException;
import com.campus.backend.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationMapper orgMapper;
    private final OrgMemberMapper memberMapper;
    private final OrgInvitationMapper invitationMapper;
    private final OrgJoinRequestMapper joinRequestMapper;
    private final OrgAuditLogMapper auditLogMapper;
    private final UserMapper userMapper;

    @Transactional
    public Organization createOrganization(Organization org, Long userId) {
        if (org.getJoinType() == null || (!"INVITE".equals(org.getJoinType()) && !"APPLY".equals(org.getJoinType()))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "必须选择加入方式: INVITE(仅邀请) 或 APPLY(可申请加入)");
        }
        org.setFounderId(userId);
        org.setStatus("APPROVED");
        orgMapper.insert(org);

        OrgMember founder = new OrgMember();
        founder.setOrgId(org.getId());
        founder.setUserId(userId);
        founder.setRole("ADMIN");
        memberMapper.insert(founder);
        orgMapper.incrementMemberCount(org.getId());

        auditLog(org.getId(), userId, "CREATE_ORG", null, org.getName());
        log.info("组织创建: id={}, name={}, founder={}", org.getId(), org.getName(), userId);
        return org;
    }

    public Organization getOrganization(Long orgId) {
        Organization org = orgMapper.selectById(orgId);
        if (org == null) throw new NotFoundException("组织", orgId);
        return org;
    }

    public List<Organization> getMyOrganizations(Long userId) {
        return orgMapper.selectByUserId(userId);
    }

    public List<Organization> getOrganizationList(int page, int size) {
        int offset = (page - 1) * size;
        return orgMapper.selectApproved(offset, size);
    }

    public int getOrganizationCount() {
        return orgMapper.countApproved();
    }

    public List<Organization> searchOrganizations(String keyword, int page, int size) {
        return orgMapper.search(keyword, (page - 1) * size, size);
    }

    @Transactional
    public void approveOrganization(Long orgId, Long adminId) {
        orgMapper.updateStatus(orgId, "APPROVED");
        auditLog(orgId, adminId, "APPROVE_ORG", null, "管理员审核通过");
    }

    @Transactional
    public void rejectOrganization(Long orgId, Long adminId) {
        orgMapper.updateStatus(orgId, "REJECTED");
        auditLog(orgId, adminId, "REJECT_ORG", null, "管理员审核拒绝");
    }

    @Transactional
    public OrgInvitation inviteMember(Long orgId, Long inviterId, Long inviteeId) {
        Organization org = getOrganization(orgId);

        OrgMember inviter = memberMapper.selectByOrgAndUser(orgId, inviterId);
        if (inviter == null || (!"ADMIN".equals(inviter.getRole()) && !"MODERATOR".equals(inviter.getRole()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可以发送邀请");
        }

        OrgMember existing = memberMapper.selectByOrgAndUser(orgId, inviteeId);
        if (existing != null) throw new BusinessException(ErrorCode.BAD_REQUEST, "该用户已是组织成员");

        // 检查是否已存在待处理的邀请，防止重复发送
        OrgInvitation pendingInvitation = invitationMapper.selectPendingByOrgAndUser(orgId, inviteeId);
        if (pendingInvitation != null) throw new BusinessException(ErrorCode.BAD_REQUEST, "已向该用户发送过邀请，请等待处理");

        OrgInvitation inv = new OrgInvitation();
        inv.setOrgId(orgId);
        inv.setInviterId(inviterId);
        inv.setInviteeId(inviteeId);
        inv.setInviteCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        invitationMapper.insert(inv);

        auditLog(orgId, inviterId, "INVITE", inviteeId, inv.getInviteCode());
        return inv;
    }

    @Transactional
    public void acceptInvitation(String inviteCode, Long userId) {
        OrgInvitation inv = invitationMapper.selectByCode(inviteCode);
        if (inv == null || !"PENDING".equals(inv.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邀请码无效或已过期");
        }
        if (!inv.getInviteeId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "此邀请不属于您");
        }
        invitationMapper.respondByCode(inviteCode, "ACCEPTED");

        // 检查是否已是成员，防止重复加入（如接受了同一组织的多个邀请）
        OrgMember existingMember = memberMapper.selectByOrgAndUser(inv.getOrgId(), userId);
        if (existingMember != null) {
            auditLog(inv.getOrgId(), userId, "ACCEPT_INVITE", null, inviteCode + "(已是成员，跳过加入)");
            return;
        }

        OrgMember member = new OrgMember();
        member.setOrgId(inv.getOrgId());
        member.setUserId(userId);
        member.setRole("MEMBER");
        memberMapper.insert(member);
        orgMapper.incrementMemberCount(inv.getOrgId());

        auditLog(inv.getOrgId(), userId, "ACCEPT_INVITE", null, inviteCode);
    }

    @Transactional
    public void rejectInvitation(String inviteCode, Long userId) {
        OrgInvitation inv = invitationMapper.selectByCode(inviteCode);
        if (inv == null || !"PENDING".equals(inv.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邀请码无效或已过期");
        }
        if (!inv.getInviteeId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "此邀请不属于您");
        }
        invitationMapper.respondByCode(inviteCode, "REJECTED");
        auditLog(inv.getOrgId(), userId, "REJECT_INVITE", null, inviteCode);
    }

    @Transactional
    public void applyToJoin(Long orgId, Long userId, String message) {
        Organization org = getOrganization(orgId);
        if (!"APPLY".equals(org.getJoinType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该组织不支持申请加入模式");
        }

        OrgMember existing = memberMapper.selectByOrgAndUser(orgId, userId);
        if (existing != null) throw new BusinessException(ErrorCode.BAD_REQUEST, "您已是该组织成员");

        OrgJoinRequest pending = joinRequestMapper.selectPendingByOrgAndUser(orgId, userId);
        if (pending != null) throw new BusinessException(ErrorCode.BAD_REQUEST, "您已提交申请，请等待审核");

        OrgJoinRequest req = new OrgJoinRequest();
        req.setOrgId(orgId);
        req.setUserId(userId);
        req.setMessage(message);
        joinRequestMapper.insert(req);
    }

    @Transactional
    public void approveJoinRequest(Long requestId, Long reviewerId) {
        OrgJoinRequest req = joinRequestMapper.selectById(requestId);
        if (req == null || !"PENDING".equals(req.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "申请不存在或已处理");
        }

        OrgMember reviewer = memberMapper.selectByOrgAndUser(req.getOrgId(), reviewerId);
        if (reviewer == null || (!"ADMIN".equals(reviewer.getRole()) && !"MODERATOR".equals(reviewer.getRole()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可以审批申请");
        }

        joinRequestMapper.updateStatus(requestId, "APPROVED", reviewerId);

        OrgMember member = new OrgMember();
        member.setOrgId(req.getOrgId());
        member.setUserId(req.getUserId());
        member.setRole("MEMBER");
        memberMapper.insert(member);
        orgMapper.incrementMemberCount(req.getOrgId());

        auditLog(req.getOrgId(), reviewerId, "APPROVE_JOIN", req.getUserId(), null);
    }

    @Transactional
    public void rejectJoinRequest(Long requestId, Long reviewerId) {
        OrgMember reviewer = memberMapper.selectByOrgAndUser(
                joinRequestMapper.selectById(requestId).getOrgId(), reviewerId);
        if (reviewer == null || (!"ADMIN".equals(reviewer.getRole()) && !"MODERATOR".equals(reviewer.getRole()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可以审批申请");
        }
        joinRequestMapper.updateStatus(requestId, "REJECTED", reviewerId);
    }

    public List<JoinRequestVO> getPendingRequests(Long orgId, Long userId) {
        OrgMember member = memberMapper.selectByOrgAndUser(orgId, userId);
        if (member == null || (!"ADMIN".equals(member.getRole()) && !"MODERATOR".equals(member.getRole()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可查看申请列表");
        }
        return joinRequestMapper.selectPendingByOrgId(orgId);
    }

    public List<MemberVO> getMembers(Long orgId, int page, int size) {
        return memberMapper.selectByOrgId(orgId, (page - 1) * size, size);
    }

    @Transactional
    public void removeMember(Long orgId, Long actorId, Long targetUserId) {
        OrgMember actor = memberMapper.selectByOrgAndUser(orgId, actorId);
        if (actor == null || (!"ADMIN".equals(actor.getRole()) && !"MODERATOR".equals(actor.getRole()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可以移除成员");
        }
        OrgMember target = memberMapper.selectByOrgAndUser(orgId, targetUserId);
        if (target == null) throw new NotFoundException("成员");
        if ("ADMIN".equals(target.getRole())) throw new BusinessException(ErrorCode.FORBIDDEN, "不能移除组织创建者");

        memberMapper.delete(orgId, targetUserId);
        orgMapper.decrementMemberCount(orgId);
        auditLog(orgId, actorId, "KICK", targetUserId, null);
    }

    @Transactional
    public void changeMemberRole(Long orgId, Long actorId, Long targetUserId, String newRole) {
        OrgMember actor = memberMapper.selectByOrgAndUser(orgId, actorId);
        if (actor == null || !"ADMIN".equals(actor.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅组织创建者可以更改角色");
        }
        memberMapper.updateRole(orgId, targetUserId, newRole);
        auditLog(orgId, actorId, "PROMOTE".equals(newRole) ? "PROMOTE" : "DEMOTE", targetUserId, newRole);
    }

    public OrgMember getMyRole(Long orgId, Long userId) {
        return memberMapper.selectByOrgAndUser(orgId, userId);
    }

    /**
     * 普通成员退出组织
     * 创建者(ADMIN)不能退出，只能解散组织
     */
    @Transactional
    public void leaveOrganization(Long orgId, Long userId) {
        OrgMember member = memberMapper.selectByOrgAndUser(orgId, userId);
        if (member == null) {
            throw new NotFoundException("您不是该组织成员");
        }
        if ("ADMIN".equals(member.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "创建者不能退出组织，如需解散请联系管理员");
        }
        memberMapper.delete(orgId, userId);
        orgMapper.decrementMemberCount(orgId);
        auditLog(orgId, userId, "LEAVE", userId, null);
    }

    public List<OrgInvitation> getMyInvitations(Long userId) {
        return invitationMapper.selectByInviteeId(userId);
    }

    public List<OrgAuditLog> getAuditLogs(Long orgId, int limit) {
        return auditLogMapper.selectByOrgId(orgId, limit);
    }

    private void auditLog(Long orgId, Long actorId, String action, Long targetId, String detail) {
        try {
            OrgAuditLog log = new OrgAuditLog();
            log.setOrgId(orgId);
            log.setActorId(actorId);
            log.setAction(action);
            log.setTargetId(targetId);
            log.setDetail(detail);
            auditLogMapper.insert(log);
        } catch (Exception ignored) {}
    }
}
