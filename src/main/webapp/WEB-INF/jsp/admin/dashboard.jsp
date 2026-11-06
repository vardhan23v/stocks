<%-- Administrator overview (AdminDashboardController). --%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="Admin overview">
    <ss:messages/>

    <div class="ss-page-head">
        <div>
            <p class="ss-eyebrow">Administrator</p>
            <h1>System overview</h1>
        </div>
        <div class="d-flex gap-2 flex-wrap">
            <a class="btn btn-ghost btn-sm" href="${ctx}/admin/stocks">Manage stocks</a>
            <a class="btn btn-primary btn-sm" href="${ctx}/admin/market-data">Market data</a>
        </div>
    </div>

    <div class="row g-3 mb-4">
        <div class="col-6 col-lg-4 col-xl-2"><a class="d-block text-reset" href="${ctx}/admin/users"><div class="ss-stat">
            <div class="ss-stat-label">Registered users</div><div class="ss-stat-value">${userCount}</div><div class="ss-stat-note">Manage accounts</div></div></a></div>
        <div class="col-6 col-lg-4 col-xl-2"><a class="d-block text-reset" href="${ctx}/admin/stocks"><div class="ss-stat">
            <div class="ss-stat-label">Active stocks</div><div class="ss-stat-value">${stockCount}</div><div class="ss-stat-note">Manage listings</div></div></a></div>
        <div class="col-6 col-lg-4 col-xl-2"><div class="ss-stat">
            <div class="ss-stat-label">Watchlist entries</div><div class="ss-stat-value">${watchlistCount}</div><div class="ss-stat-note">Across all users</div></div></div>
        <div class="col-6 col-lg-4 col-xl-2"><a class="d-block text-reset" href="${ctx}/admin/rules"><div class="ss-stat">
            <div class="ss-stat-label">Recommendations</div><div class="ss-stat-value">${recommendationCount}</div><div class="ss-stat-note">Generated in total</div></div></a></div>
        <div class="col-6 col-lg-4 col-xl-2"><div class="ss-stat">
            <div class="ss-stat-label">Latest market data</div>
            <div class="ss-stat-value ss-stat-small">${not empty latestBar ? fmt.date(latestBar) : 'None'}</div>
            <div class="ss-stat-note"><c:out value="${empty latestBarSource ? '-' : latestBarSource}"/> &#183; ${priceRowCount} price rows</div></div></div>
        <div class="col-6 col-lg-4 col-xl-2"><a class="d-block text-reset" href="${ctx}/admin/market-data"><div class="ss-stat">
            <div class="ss-stat-label">API status</div>
            <div class="ss-stat-value ss-stat-small ${market.demoMode ? '' : 'up'}">${market.demoMode ? 'Demo mode' : 'Configured'}</div>
            <div class="ss-stat-note">${callsToday} of ${market.dailyQuota} calls today</div></div></a></div>
    </div>

    <div class="row g-3">
        <div class="col-lg-7">
            <div class="ss-card ss-card-flush">
                <div class="ss-card-head"><h2>System activity</h2></div>
                <div class="table-responsive mt-2">
                    <table class="table table-sm ss-table">
                        <thead><tr><th scope="col">When</th><th scope="col">Who</th><th scope="col">Action</th><th scope="col">Details</th></tr></thead>
                        <tbody>
                            <c:forEach var="a" items="${activity}">
                                <tr>
                                    <td class="num small ss-muted">${fmt.time(a.createdAt)}</td>
                                    <td class="small"><c:out value="${empty a.userName ? 'System' : a.userName}"/></td>
                                    <td><span class="mono small"><c:out value="${a.action}"/></span></td>
                                    <td class="small ss-muted"><c:out value="${a.details}"/></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        <div class="col-lg-5">
            <div class="ss-card">
                <div class="ss-card-head">
                    <h2>Market data integration</h2>
                    <a class="small" href="${ctx}/admin/market-data">Details</a>
                </div>
                <p class="small ss-muted"><c:out value="${market.providerLabel}"/></p>
                <dl class="row small">
                    <dt class="col-6 ss-faint fw-normal">Last successful sync</dt>
                    <dd class="col-6">${not empty latestSuccess ? fmt.dateTime(latestSuccess.syncedAt) : 'Never'}</dd>
                    <dt class="col-6 ss-faint fw-normal">Last attempt</dt>
                    <dd class="col-6"><c:out value="${not empty latestSync ? latestSync.status : 'None'}"/></dd>
                </dl>
                <ul class="ss-list">
                    <c:forEach var="l" items="${syncLogs}">
                        <li class="small">
                            <span><span class="mono"><c:out value="${l.symbol}"/></span> <span class="ss-faint"><c:out value="${l.provider}"/></span></span>
                            <span class="${l.status == 'SUCCESS' ? 'up' : (l.status == 'ERROR' or l.status == 'RATE_LIMITED' ? 'down' : 'ss-muted')}"><c:out value="${l.status}"/></span>
                        </li>
                    </c:forEach>
                </ul>
            </div>
        </div>
    </div>
</ss:page>
