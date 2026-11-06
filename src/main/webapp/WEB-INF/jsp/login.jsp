<%-- Login form. Spring Security processes the POST to /login (see SecurityConfig). --%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="Log in">
    <div class="ss-auth">
        <div class="ss-card">
            <p class="ss-eyebrow">Welcome back</p>
            <h1 class="mb-4">Log in to StockSense</h1>
            <ss:messages/>

            <form action="${ctx}/login" method="post">
                <sec:csrfInput/>
                <div class="mb-3">
                    <label class="form-label" for="email">Email</label>
                    <input class="form-control" id="email" name="email" type="email" autocomplete="username" required autofocus/>
                </div>
                <div class="mb-4">
                    <label class="form-label" for="password">Password</label>
                    <input class="form-control" id="password" name="password" type="password" autocomplete="current-password" required/>
                </div>
                <button type="submit" class="btn btn-primary w-100">Log in</button>
            </form>

            <p class="mt-3 mb-0 small ss-muted">New here? <a href="${ctx}/register">Create an account</a></p>
        </div>

        <div class="ss-demo-box mt-3">
            <strong>Demo accounts</strong> (from sample-data.sql, see README):<br/>
            Admin: <span class="mono">admin@stocksense.test</span> / <span class="mono">Admin@123</span><br/>
            User: <span class="mono">priya@stocksense.test</span> / <span class="mono">User@1234</span>
        </div>
    </div>
</ss:page>
