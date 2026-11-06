<%-- Stock explorer: /stocks?q=&exchange=&sector=&sort= (StockController). --%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="Stocks">
    <ss:messages/>

    <div class="ss-page-head">
        <div>
            <p class="ss-eyebrow">Explore</p>
            <h1>Stocks</h1>
        </div>
        <c:if test="${anySample}"><span class="ss-badge-sample">Prices are sample data</span></c:if>
    </div>

    <%-- A plain GET form, so every search is a bookmarkable URL. --%>
    <form class="ss-card mb-3" method="get" action="${ctx}/stocks" data-autosubmit="true" role="search">
        <div class="row g-2 align-items-end">
            <div class="col-md-4">
                <label class="form-label" for="q">Company or ticker</label>
                <input id="q" class="form-control" type="search" name="q" value="${fn:escapeXml(q)}" placeholder="e.g. INFY or Apple" maxlength="60"/>
            </div>
            <div class="col-6 col-md-2">
                <label class="form-label" for="exchange">Exchange</label>
                <select id="exchange" name="exchange" class="form-select">
                    <option value="">All</option>
                    <c:forEach var="e" items="${exchanges}">
                        <option value="${fn:escapeXml(e)}" ${e == exchange ? 'selected' : ''}><c:out value="${e}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-6 col-md-3">
                <label class="form-label" for="sector">Sector</label>
                <select id="sector" name="sector" class="form-select">
                    <option value="">All sectors</option>
                    <c:forEach var="s" items="${sectors}">
                        <option value="${fn:escapeXml(s)}" ${s == sector ? 'selected' : ''}><c:out value="${s}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-8 col-md-2">
                <label class="form-label" for="sort">Sort by</label>
                <select id="sort" name="sort" class="form-select">
                    <option value="symbol" ${sort == 'symbol' ? 'selected' : ''}>Ticker A-Z</option>
                    <option value="name" ${sort == 'name' ? 'selected' : ''}>Company A-Z</option>
                    <option value="change_desc" ${sort == 'change_desc' ? 'selected' : ''}>Biggest gain</option>
                    <option value="change_asc" ${sort == 'change_asc' ? 'selected' : ''}>Biggest fall</option>
                    <option value="price" ${sort == 'price' ? 'selected' : ''}>Highest price</option>
                </select>
            </div>
            <div class="col-4 col-md-1 d-grid">
                <button class="btn btn-primary" type="submit">Go</button>
            </div>
        </div>
        <c:if test="${filtered}">
            <div class="small ss-muted mt-2">${fn:length(stocks)} result(s). <a href="${ctx}/stocks">Clear filters</a></div>
        </c:if>
    </form>

    <div class="ss-card ss-card-flush">
        <c:choose>
            <c:when test="${empty stocks}">
                <div class="ss-empty">
                    <div class="ss-empty-icon"><i class="bi bi-search" aria-hidden="true"></i></div>
                    <p class="mb-0">No stocks match your search.</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table table-hover ss-table">
                        <thead>
                            <tr>
                                <th scope="col">Stock</th>
                                <th scope="col" class="d-none d-md-table-cell">Exchange</th>
                                <th scope="col" class="d-none d-lg-table-cell">Sector</th>
                                <th scope="col" class="num text-end">Latest close</th>
                                <th scope="col" class="text-end">Change</th>
                                <th scope="col" class="d-none d-lg-table-cell">As of</th>
                                <th scope="col" class="text-end"><span class="visually-hidden">Actions</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="s" items="${stocks}">
                                <tr>
                                    <td>
                                        <a class="ss-symbol" href="${ctx}/stocks/${s.id}"><c:out value="${s.symbol}"/></a>
                                        <div class="ss-company"><c:out value="${s.companyName}"/></div>
                                    </td>
                                    <td class="d-none d-md-table-cell"><span class="ss-badge-source"><c:out value="${s.exchange}"/></span></td>
                                    <td class="d-none d-lg-table-cell ss-muted small"><c:out value="${s.sector}"/><div class="ss-faint"><c:out value="${s.industry}"/></div></td>
                                    <td class="num text-end fw-semibold">
                                        <c:choose>
                                            <c:when test="${s.hasPrice}">${s.currencySymbol}${fmt.money(s.latestPrice)}</c:when>
                                            <c:otherwise><span class="ss-faint">No data</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-end"><ss:priceChange stock="${s}" compact="false"/></td>
                                    <td class="d-none d-lg-table-cell small">
                                        <c:if test="${not empty s.lastUpdated}">${fmt.date(s.lastUpdated)}</c:if>
                                        <div><ss:dataBadge source="${s.dataSource}"/></div>
                                    </td>
                                    <td class="text-end text-nowrap">
                                        <c:choose>
                                            <c:when test="${watchedIds.contains(s.id)}">
                                                <span class="small up"><i class="bi bi-star-fill" aria-hidden="true"></i> Watching</span>
                                            </c:when>
                                            <c:otherwise>
                                                <form action="${ctx}/watchlist/add" method="post" class="m-0">
                                                    <sec:csrfInput/>
                                                    <input type="hidden" name="stockId" value="${s.id}"/>
                                                    <input type="hidden" name="returnTo" value="${fn:escapeXml(currentUrl)}"/>
                                                    <button type="submit" class="btn btn-sm btn-ghost">Watch</button>
                                                </form>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
    <p class="small ss-faint mt-2">Change compares the two most recent stored closes. Prices are end-of-day values with their source - never real-time quotes.</p>
</ss:page>
