<%-- User accounts (AdminUserController). An admin cannot change their own account here. --%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="Users · Admin">
    <ss:messages/>

    <div class="ss-page-head">
        <div>
            <p class="ss-eyebrow">Administrator</p>
            <h1>User accounts</h1>
        </div>
        <div class="small ss-muted">${fn:length(users)} accounts &#183; ${activeCount} active &#183; ${adminCount} admin</div>
    </div>

    <div class="ss-card ss-card-flush">
        <div class="table-responsive">
            <table class="table table-hover ss-table">
                <thead>
                    <tr>
                        <th scope="col">Name</th>
                        <th scope="col" class="d-none d-md-table-cell">Email</th>
                        <th scope="col">Role</th>
                        <th scope="col">Status</th>
                        <th scope="col" class="num text-end d-none d-lg-table-cell">Watchlist</th>
                        <th scope="col" class="d-none d-lg-table-cell">Created</th>
                        <th scope="col" class="text-end">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="u" items="${users}">
                        <tr>
                            <td class="fw-semibold"><c:out value="${u.name}"/><div class="small ss-muted d-md-none"><c:out value="${u.email}"/></div></td>
                            <td class="d-none d-md-table-cell ss-muted"><c:out value="${u.email}"/></td>
                            <td><span class="ss-role-pill">${u.role}</span></td>
                            <td><span class="small ${u.active ? 'up' : 'down'}">${u.active ? 'Active' : 'Deactivated'}</span></td>
                            <td class="num text-end d-none d-lg-table-cell">${u.watchlistCount}</td>
                            <td class="d-none d-lg-table-cell small ss-muted">${fmt.date(u.createdAt)}</td>
                            <td class="text-end text-nowrap">
                                <c:choose>
                                    <c:when test="${u.id == currentUser.id}"><span class="small ss-faint">This is you</span></c:when>
                                    <c:otherwise>
                                        <form class="d-inline m-0" action="${ctx}/admin/users/${u.id}/active" method="post">
                                            <sec:csrfInput/>
                                            <input type="hidden" name="active" value="${not u.active}"/>
                                            <button type="submit" class="btn btn-sm ${u.active ? 'btn-ghost' : 'btn-success-soft'}">${u.active ? 'Deactivate' : 'Activate'}</button>
                                        </form>
                                        <form class="d-inline m-0" action="${ctx}/admin/users/${u.id}/role" method="post"
                                              onsubmit="return confirm(this.dataset.confirm);" data-confirm="Change the role of ${fn:escapeXml(u.name)}?">
                                            <sec:csrfInput/>
                                            <input type="hidden" name="role" value="${u.admin ? 'USER' : 'ADMIN'}"/>
                                            <button type="submit" class="btn btn-sm btn-ghost ms-1">${u.admin ? 'Make user' : 'Make admin'}</button>
                                        </form>
                                        <form class="d-inline m-0" action="${ctx}/admin/users/${u.id}/delete" method="post"
                                              onsubmit="return confirm(this.dataset.confirm);" data-confirm="Permanently delete ${fn:escapeXml(u.name)} and their watchlist?">
                                            <sec:csrfInput/>
                                            <button type="submit" class="btn btn-sm btn-danger-soft ms-1">Delete</button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
    <p class="small ss-faint mt-2">Deactivated accounts cannot log in; their data is kept. You cannot change your own account here.</p>
</ss:page>
