<%-- User dashboard. Model from DashboardController / DashboardService. --%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="Dashboard">
    <ss:messages/>

    <div class="ss-page-head">
        <div>
            <p class="ss-eyebrow">Dashboard</p>
            <h1>Welcome, <c:out value="${currentUser.name}"/></h1>
        </div>
        <form class="d-flex gap-2" role="search" method="get" action="${ctx}/stocks" style="min-width: min(360px, 100%);">
            <label class="visually-hidden" for="dashSearch">Search stocks</label>
            <input id="dashSearch" class="form-control" type="search" name="q" placeholder="Search by ticker or company" maxlength="60"/>
            <button class="btn btn-primary" type="submit"><i class="bi bi-search" aria-hidden="true"></i><span class="visually-hidden">Search</span></button>
        </form>
    </div>

    <div class="row g-3 mb-4">
        <div class="col-6 col-lg-3"><div class="ss-stat">
            <div class="ss-stat-label">Tracked stocks</div>
            <div class="ss-stat-value">${trackedCount}</div>
            <div class="ss-stat-note">BSE and NASDAQ listings</div>
        </div></div>
        <div class="col-6 col-lg-3"><div class="ss-stat">
            <div class="ss-stat-label">Latest market data</div>
            <div class="ss-stat-value ss-stat-small">${not empty latestDataTimestamp ? fmt.date(latestDataTimestamp) : 'No data yet'}</div>
            <div class="ss-stat-note">End-of-day close, not real-time</div>
        </div></div>
        <div class="col-6 col-lg-3"><div class="ss-stat">
            <div class="ss-stat-label">Data provider</div>
            <div class="ss-stat-value ss-stat-small">${market.demoMode ? 'Demo mode' : 'Alpha Vantage'}</div>
            <div class="ss-stat-note"><c:out value="${market.providerLabel}"/></div>
        </div></div>
        <div class="col-6 col-lg-3"><div class="ss-stat">
            <div class="ss-stat-label">Your watchlist</div>
            <div class="ss-stat-value">${watchlistSize}</div>
            <div class="ss-stat-note"><a href="${ctx}/watchlist">Manage watchlist</a></div>
        </div></div>
    </div>

    <div class="row g-3 mb-4">
        <div class="col-lg-8">
            <div class="ss-card">
                <div class="ss-card-head">
                    <h2>3-month performance</h2>
                    <c:if test="${sampleData}"><span class="ss-badge-sample">Sample data</span></c:if>
                </div>
                <p class="small ss-muted mt-n2">Percentage change of ${watchlistSize > 0 ? 'your watchlist stocks' : 'a few tracked stocks'} since the start of the period, from stored closing prices.</p>
                <div class="ss-chart ss-chart-lg" data-chart-kind="performance" data-chart="${fn:escapeXml(chartJson)}"></div>
            </div>
        </div>
        <div class="col-lg-4">
            <div class="ss-card">
                <div class="ss-card-head">
                    <h2>Watchlist</h2>
                    <a class="small" href="${ctx}/watchlist">View all</a>
                </div>
                <c:if test="${empty watchlist}">
                    <div class="ss-empty">
                        <div class="ss-empty-icon"><i class="bi bi-star" aria-hidden="true"></i></div>
                        <p class="mb-2">Your watchlist is empty.</p>
                        <a class="btn btn-sm btn-ghost" href="${ctx}/stocks">Find stocks to watch</a>
                    </div>
                </c:if>
                <ul class="ss-list">
                    <c:forEach var="w" items="${watchlist}">
                        <li>
                            <div class="text-truncate">
                                <a class="ss-symbol" href="${ctx}/stocks/${w.stock.id}"><c:out value="${w.stock.symbol}"/></a>
                                <div class="ss-company text-truncate"><c:out value="${w.stock.companyName}"/></div>
                            </div>
                            <div class="text-end">
                                <div class="num fw-semibold">${w.stock.currencySymbol}${fmt.money(w.stock.latestPrice)}</div>
                                <ss:priceChange stock="${w.stock}" compact="true"/>
                            </div>
                        </li>
                    </c:forEach>
                </ul>
            </div>
        </div>
    </div>

    <div class="row g-3 mb-4">
        <c:forEach var="panel" items="up,down">
            <c:set var="list" value="${panel == 'up' ? gainers : losers}"/>
            <div class="col-md-6">
                <div class="ss-card">
                    <div class="ss-card-head">
                        <h2><i class="bi me-1 ${panel == 'up' ? 'bi-arrow-up-right up' : 'bi-arrow-down-right down'}" aria-hidden="true"></i>${panel == 'up' ? 'Top gainers' : 'Top decliners'}</h2>
                        <span class="small ss-faint">vs previous close</span>
                    </div>
                    <c:if test="${empty list}">
                        <p class="ss-muted small mb-0">No ${panel == 'up' ? 'rising' : 'falling'} stocks in the latest stored session.</p>
                    </c:if>
                    <ul class="ss-list">
                        <c:forEach var="s" items="${list}">
                            <li>
                                <div class="text-truncate">
                                    <a class="ss-symbol" href="${ctx}/stocks/${s.id}"><c:out value="${s.symbol}"/></a>
                                    <span class="ss-faint small ms-1"><c:out value="${s.exchange}"/></span>
                                    <div class="ss-company text-truncate"><c:out value="${s.companyName}"/></div>
                                </div>
                                <div class="text-end">
                                    <div class="num">${s.currencySymbol}${fmt.money(s.latestPrice)}</div>
                                    <ss:priceChange stock="${s}" compact="true"/>
                                </div>
                            </li>
                        </c:forEach>
                    </ul>
                </div>
            </div>
        </c:forEach>
    </div>

    <div class="ss-card-head">
        <h2>Latest recommendations</h2>
        <a class="small" href="${ctx}/recommendations">All recommendations</a>
    </div>
    <div class="row g-3 mb-4">
        <c:forEach var="r" items="${recommendations}">
            <div class="col-sm-6 col-xl-4"><ss:recCard r="${r}" allReasons="false"/></div>
        </c:forEach>
    </div>

    <div class="ss-card">
        <div class="ss-card-head"><h2>Recently viewed</h2></div>
        <c:if test="${empty recentlyViewed}"><p class="ss-muted small mb-0">Stocks you open will appear here.</p></c:if>
        <div class="d-flex flex-wrap gap-2">
            <c:forEach var="s" items="${recentlyViewed}">
                <a class="btn btn-ghost btn-sm d-inline-flex align-items-center gap-2" href="${ctx}/stocks/${s.id}">
                    <span class="fw-bold"><c:out value="${s.symbol}"/></span>
                    <ss:priceChange stock="${s}" compact="true"/>
                </a>
            </c:forEach>
        </div>
    </div>
</ss:page>
