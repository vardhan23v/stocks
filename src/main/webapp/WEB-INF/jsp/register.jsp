<%-- Registration. required/minlength only help the user; AuthService.register() validates again. --%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="Create account">
    <div class="ss-auth">
        <div class="ss-card">
            <p class="ss-eyebrow">Free account</p>
            <h1 class="mb-1">Create your account</h1>
            <p class="ss-muted small mb-4">Track stocks, keep a private watchlist and read explainable recommendations.</p>
            <ss:messages/>

            <form action="${ctx}/register" method="post">
                <sec:csrfInput/>
                <div class="mb-3">
                    <label class="form-label" for="name">Full name</label>
                    <input class="form-control" id="name" name="name" value="${fn:escapeXml(name)}" autocomplete="name" required minlength="2" maxlength="100"/>
                </div>
                <div class="mb-3">
                    <label class="form-label" for="email">Email</label>
                    <input class="form-control" id="email" name="email" type="email" value="${fn:escapeXml(email)}" autocomplete="email" required maxlength="150"/>
                </div>
                <div class="mb-3">
                    <label class="form-label" for="password">Password</label>
                    <input class="form-control" id="password" name="password" type="password" autocomplete="new-password"
                           required minlength="8" aria-describedby="passwordHelp"/>
                    <div id="passwordHelp" class="form-text ss-faint">At least 8 characters, with a letter and a digit.</div>
                </div>
                <div class="mb-4">
                    <label class="form-label" for="confirmPassword">Confirm password</label>
                    <input class="form-control" id="confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" required minlength="8"/>
                </div>
                <button type="submit" class="btn btn-primary w-100">Create account</button>
            </form>

            <p class="mt-3 mb-0 small ss-muted">Already registered? <a href="${ctx}/login">Log in</a></p>
        </div>
    </div>
</ss:page>
