<%-- Own account: details, display name, password (ProfileController). --%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="Profile">
    <ss:messages/>

    <div class="ss-page-head">
        <div>
            <p class="ss-eyebrow">Account</p>
            <h1>Your profile</h1>
        </div>
    </div>

    <div class="row g-3">
        <div class="col-lg-4">
            <div class="ss-card">
                <div class="d-flex align-items-center gap-3 mb-3">
                    <span class="ss-avatar" style="width: 52px; height: 52px; font-size: 1.1rem;">${fmt.initials(profile.name)}</span>
                    <div>
                        <div class="fw-semibold fs-5"><c:out value="${profile.name}"/></div>
                        <span class="ss-role-pill">${profile.role}</span>
                    </div>
                </div>
                <dl class="row small mb-0">
                    <dt class="col-5 ss-faint fw-normal">Email</dt><dd class="col-7 text-break"><c:out value="${profile.email}"/></dd>
                    <dt class="col-5 ss-faint fw-normal">Member since</dt><dd class="col-7">${fmt.date(profile.createdAt)}</dd>
                    <dt class="col-5 ss-faint fw-normal">Watchlist</dt><dd class="col-7">${profile.watchlistCount} stock(s)</dd>
                    <dt class="col-5 ss-faint fw-normal">Status</dt><dd class="col-7 mb-0">${profile.active ? 'Active' : 'Deactivated'}</dd>
                </dl>
            </div>
        </div>

        <div class="col-lg-4">
            <div class="ss-card">
                <h2 class="fs-6 mb-3">Display name</h2>
                <form action="${ctx}/profile/name" method="post">
                    <sec:csrfInput/>
                    <div class="mb-3">
                        <label class="form-label" for="name">Full name</label>
                        <input class="form-control" id="name" name="name" value="${fn:escapeXml(profile.name)}" required minlength="2" maxlength="100"/>
                    </div>
                    <button type="submit" class="btn btn-primary">Save name</button>
                </form>
            </div>
        </div>

        <div class="col-lg-4">
            <div class="ss-card">
                <h2 class="fs-6 mb-3">Change password</h2>
                <form action="${ctx}/profile/password" method="post">
                    <sec:csrfInput/>
                    <div class="mb-2">
                        <label class="form-label" for="currentPassword">Current password</label>
                        <input class="form-control" id="currentPassword" name="currentPassword" type="password" autocomplete="current-password" required/>
                    </div>
                    <div class="mb-2">
                        <label class="form-label" for="newPassword">New password</label>
                        <input class="form-control" id="newPassword" name="newPassword" type="password" autocomplete="new-password" required minlength="8"/>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="confirmPassword">Confirm new password</label>
                        <input class="form-control" id="confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" required minlength="8"/>
                    </div>
                    <button type="submit" class="btn btn-ghost">Change password</button>
                </form>
            </div>
        </div>
    </div>
</ss:page>
