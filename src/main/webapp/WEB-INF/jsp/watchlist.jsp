<%-- The logged-in user's private watchlist (WatchlistController). --%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="Watchlist">
    <ss:messages/>

    <div class="ss-page-head">
        <div>
            <p class="ss-eyebrow">Private to you</p>
            <h1>Your watchlist</h1>
        </div>
        <form class="d-flex gap-2 ss-add-form" action="${ctx}/watchlist/add" method="post">
            <sec:csrfInput/>
            <label class="visually-hidden" for="stockId">Add a stock</label>
            <select id="stockId" name="stockId" class="form-select">
                <option value="">Add a stock...</option>
                <c:forEach var="st" items="${availableStocks}">
                    <option value="${st.id}"><c:out value="${st.symbol} - ${st.companyName}"/></option>
                </c:forEach>
            </select>
            <input type="hidden" name="returnTo" value="${fn:escapeXml(currentUrl)}"/>
            <button type="submit" class="btn btn-primary">Add</button>
        </form>
    </div>

    <div class="row g-3 mb-3">
        <div class="col-4"><div class="ss-stat"><div class="ss-stat-label">Stocks</div><div class="ss-stat-value">${fn:length(entries)}</div></div></div>
        <div class="col-4"><div class="ss-stat"><div class="ss-stat-label">Rising</div><div class="ss-stat-value up">${gainersCount}</div></div></div>
        <div class="col-4"><div class="ss-stat"><div class="ss-stat-label">Falling</div><div class="ss-stat-value down">${decliningCount}</div></div></div>
    </div>

    <div class="ss-card ss-card-flush">
        <div class="ss-card-head flex-wrap">
            <h2>Price movement</h2>
            <nav class="ss-segmented" aria-label="Sort watchlist">
                <a href="${ctx}/watchlist?sort=change_desc" class="${sort == 'change_desc' ? 'is-active' : ''}">Biggest gain</a>
                <a href="${ctx}/watchlist?sort=change_asc" class="${sort == 'change_asc' ? 'is-active' : ''}">Biggest fall</a>
                <a href="${ctx}/watchlist?sort=symbol" class="${sort == 'symbol' ? 'is-active' : ''}">Ticker</a>
                <a href="${ctx}/watchlist?sort=added" class="${sort == 'added' ? 'is-active' : ''}">Recently added</a>
            </nav>
        </div>

        <c:choose>
            <c:when test="${empty entries}">
                <div class="ss-empty">
                    <div class="ss-empty-icon"><i class="bi bi-star" aria-hidden="true"></i></div>
                    <p>You are not watching any stocks yet.</p>
                    <a class="btn btn-ghost btn-sm" href="${ctx}/stocks">Explore stocks</a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-responsive mt-2">
                    <table class="table table-hover ss-table">
                        <thead>
                            <tr>
                                <th scope="col">Stock</th>
                                <th scope="col" class="num text-end">Latest close</th>
                                <th scope="col" class="text-end">Change</th>
                                <th scope="col" class="d-none d-md-table-cell">As of</th>
                                <th scope="col" class="d-none d-lg-table-cell">Added</th>
                                <th scope="col" class="text-end"><span class="visually-hidden">Actions</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="w" items="${entries}">
                                <tr>
                                    <td>
                                        <a class="ss-symbol" href="${ctx}/stocks/${w.stock.id}"><c:out value="${w.stock.symbol}"/></a>
                                        <span class="ss-faint small ms-1"><c:out value="${w.stock.exchange}"/></span>
                                        <div class="ss-company"><c:out value="${w.stock.companyName}"/></div>
                                        <c:if test="${not w.stock.active}"><span class="small down">No longer listed</span></c:if>
                                    </td>
                                    <td class="num text-end fw-semibold">
                                        <c:choose>
                                            <c:when test="${w.stock.hasPrice}">${w.stock.currencySymbol}${fmt.money(w.stock.latestPrice)}</c:when>
                                            <c:otherwise><span class="ss-faint">No data</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-end"><ss:priceChange stock="${w.stock}" compact="false"/></td>
                                    <td class="d-none d-md-table-cell small">
                                        <c:if test="${not empty w.stock.lastUpdated}">${fmt.date(w.stock.lastUpdated)}</c:if>
                                        <div><ss:dataBadge source="${w.stock.dataSource}"/></div>
                                    </td>
                                    <td class="d-none d-lg-table-cell small ss-muted">${fmt.date(w.addedAt)}</td>
                                    <td class="text-end text-nowrap">
                                        <a class="btn btn-sm btn-ghost me-1" href="${ctx}/stocks/${w.stock.id}">Details</a>
                                        <form action="${ctx}/watchlist/remove" method="post" class="d-inline m-0" onsubmit="return confirm(this.dataset.confirm);"
                                              data-confirm="Remove ${fn:escapeXml(w.stock.symbol)} from your watchlist?">
                                            <sec:csrfInput/>
                                            <input type="hidden" name="stockId" value="${w.stockId}"/>
                                            <input type="hidden" name="returnTo" value="${fn:escapeXml(currentUrl)}"/>
                                            <button type="submit" class="btn btn-sm btn-danger-soft">Remove</button>
                                        </form>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
    <p class="small ss-faint mt-2">Change compares the two most recent stored closes of each stock.</p>
</ss:page>
