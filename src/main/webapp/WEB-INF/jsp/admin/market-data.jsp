<%-- Provider configuration, stored data, manual syncs and the sync log (AdminMarketDataController). The API key is never shown. --%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="Market data · Admin">
    <ss:messages/>

    <div class="ss-page-head">
        <div>
            <p class="ss-eyebrow">Administrator</p>
            <h1>Market data</h1>
        </div>
        <div class="d-flex gap-2">
            <a class="btn btn-ghost" href="${ctx}/admin/market-data">Refresh</a>
            <form action="${ctx}/admin/market-data/sync-all" method="post">
                <sec:csrfInput/>
                <button type="submit" class="btn btn-primary" ${market.bulkSyncRunning ? 'disabled' : ''}>Sync all stale stocks</button>
            </form>
        </div>
    </div>

    <div class="row g-3 mb-4">
        <div class="col-lg-5">
            <div class="ss-card">
                <h2 class="fs-6">Provider configuration</h2>
                <dl class="row small mb-3">
                    <dt class="col-6 ss-faint fw-normal">Configured provider</dt><dd class="col-6 mono"><c:out value="${market.configuredProvider}"/></dd>
                    <dt class="col-6 ss-faint fw-normal">API key</dt>
                    <dd class="col-6"><span class="${market.apiKeyConfigured ? 'up' : 'down'}">${market.apiKeyConfigured ? 'Configured (hidden)' : 'Not configured'}</span></dd>
                    <dt class="col-6 ss-faint fw-normal">Mode</dt><dd class="col-6">${market.demoMode ? 'Demo - sample data only' : 'Live provider (delayed EOD)'}</dd>
                    <dt class="col-6 ss-faint fw-normal">Cache lifetime</dt><dd class="col-6">${market.cacheMinutes} minutes</dd>
                    <dt class="col-6 ss-faint fw-normal">API calls today</dt><dd class="col-6">${callsToday} of ${market.dailyQuota} (free tier)</dd>
                    <dt class="col-6 ss-faint fw-normal">Background sync</dt><dd class="col-6">${market.bulkSyncRunning ? 'Running...' : 'Idle'}</dd>
                </dl>
                <c:if test="${market.demoMode}">
                    <p class="ss-disclaimer mb-0">
                        To use real delayed prices, get a free key at alphavantage.co, then set the environment variables
                        <span class="mono">MARKET_PROVIDER=ALPHA_VANTAGE</span> and <span class="mono">ALPHA_VANTAGE_API_KEY=...</span>
                        and restart. Keys are never stored in source code.
                    </p>
                </c:if>
            </div>
        </div>
        <div class="col-lg-7">
            <div class="ss-card ss-card-flush">
                <div class="ss-card-head"><h2>Stored price data by source</h2></div>
                <div class="table-responsive mt-2">
                    <table class="table ss-table">
                        <thead><tr><th scope="col">Source</th><th scope="col" class="num text-end">Rows</th><th scope="col" class="num text-end">Stocks</th><th scope="col">Newest bar</th><th scope="col">Last written</th></tr></thead>
                        <tbody>
                            <c:forEach var="row" items="${summary}">
                                <tr>
                                    <td><ss:dataBadge source="${row[0]}"/></td>
                                    <td class="num text-end">${row[1]}</td>
                                    <td class="num text-end">${row[2]}</td>
                                    <td class="small">${fmt.date(row[3])}</td>
                                    <td class="small ss-muted">${fmt.dateTime(row[4])}</td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <div class="ss-card ss-card-flush mb-4">
        <div class="ss-card-head"><h2>Stocks</h2><span class="small ss-faint">Sync respects the cache; "Force" ignores it</span></div>
        <div class="table-responsive mt-2 ss-scroll">
            <table class="table table-sm ss-table">
                <thead><tr><th scope="col">Stock</th><th scope="col">Provider symbol</th><th scope="col">Latest bar</th><th scope="col">Source in use</th><th scope="col" class="text-end">Actions</th></tr></thead>
                <tbody>
                    <c:forEach var="s" items="${stocks}">
                        <tr>
                            <td><span class="ss-symbol"><c:out value="${s.symbol}"/></span> <span class="small ss-faint"><c:out value="${s.exchange}"/></span></td>
                            <td class="mono"><c:out value="${s.apiSymbol}"/></td>
                            <td class="small">${fmt.date(s.lastUpdated)}</td>
                            <td><ss:dataBadge source="${s.dataSource}"/></td>
                            <td class="text-end text-nowrap">
                                <form class="d-inline m-0" action="${ctx}/admin/market-data/sync/${s.id}" method="post">
                                    <sec:csrfInput/>
                                    <button type="submit" class="btn btn-sm btn-ghost">Sync</button>
                                </form>
                                <form class="d-inline m-0" action="${ctx}/admin/market-data/sync/${s.id}?force=true" method="post">
                                    <sec:csrfInput/>
                                    <button type="submit" class="btn btn-sm btn-ghost ms-1">Force</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>

    <div class="ss-card ss-card-flush">
        <div class="ss-card-head"><h2>Data update history</h2><span class="small ss-faint">api_sync_logs, newest 60</span></div>
        <div class="table-responsive mt-2 ss-scroll">
            <table class="table table-sm ss-table">
                <thead><tr><th scope="col">When</th><th scope="col">Provider</th><th scope="col">Symbol</th><th scope="col">Status</th><th scope="col">Message</th></tr></thead>
                <tbody>
                    <c:forEach var="l" items="${logs}">
                        <tr>
                            <td class="num small ss-muted">${fmt.time(l.syncedAt)}</td>
                            <td class="small"><c:out value="${l.provider}"/></td>
                            <td class="mono"><c:out value="${l.symbol}"/></td>
                            <td><span class="small fw-semibold ${l.status == 'SUCCESS' ? 'up' : (l.status == 'ERROR' or l.status == 'RATE_LIMITED' or l.status == 'NO_DATA' ? 'down' : 'ss-muted')}"><c:out value="${l.status}"/></span></td>
                            <td class="small ss-muted"><c:out value="${l.responseMessage}"/></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</ss:page>
