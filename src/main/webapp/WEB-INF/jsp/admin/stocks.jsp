<%--
  Stock records (AdminStockController). The add/edit form uses Spring's
  form tags: <form:form modelAttribute="form"> binds to a Stock object and
  adds the CSRF token by itself.
--%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="Stocks · Admin">
    <ss:messages/>

    <div class="ss-page-head">
        <div>
            <p class="ss-eyebrow">Administrator</p>
            <h1>Stock records</h1>
        </div>
    </div>

    <div class="d-flex flex-wrap gap-2 mb-3">
        <form class="d-flex gap-2" method="get" action="${ctx}/admin/stocks" role="search">
            <label class="visually-hidden" for="q">Search</label>
            <input class="form-control" id="q" name="q" type="search" value="${fn:escapeXml(q)}" placeholder="Search ticker or company" style="max-width: 320px;"/>
            <button type="submit" class="btn btn-ghost">Search</button>
        </form>
        <c:if test="${empty form}"><a class="btn btn-primary ms-auto" href="${ctx}/admin/stocks?new">+ Add stock</a></c:if>
    </div>

    <c:if test="${not empty form}">
        <form:form modelAttribute="form" servletRelativeAction="/admin/stocks" method="post" cssClass="ss-card mb-3">
            <h2 class="fs-6 mb-3">
                <c:choose>
                    <c:when test="${form.id > 0}">Edit <c:out value="${form.symbol}"/></c:when>
                    <c:otherwise>New stock</c:otherwise>
                </c:choose>
            </h2>
            <form:hidden path="id"/>
            <div class="row g-3">
                <div class="col-md-6">
                    <label class="form-label" for="companyName">Company name</label>
                    <form:input path="companyName" cssClass="form-control" required="required" maxlength="150"/>
                </div>
                <div class="col-6 col-md-3">
                    <label class="form-label" for="symbol">Ticker symbol</label>
                    <form:input path="symbol" cssClass="form-control text-uppercase" required="required" maxlength="20"/>
                </div>
                <div class="col-6 col-md-3">
                    <label class="form-label" for="exchange">Exchange</label>
                    <form:input path="exchange" cssClass="form-control text-uppercase" required="required" maxlength="20" placeholder="NASDAQ, BSE, NSE"/>
                </div>
                <div class="col-6 col-md-3">
                    <label class="form-label" for="apiSymbol">Provider symbol</label>
                    <form:input path="apiSymbol" cssClass="form-control" maxlength="40" placeholder="e.g. RELIANCE.BSE"/>
                </div>
                <div class="col-6 col-md-3">
                    <label class="form-label" for="currency">Currency</label>
                    <form:input path="currency" cssClass="form-control text-uppercase" required="required" maxlength="3" placeholder="USD"/>
                </div>
                <div class="col-md-3">
                    <label class="form-label" for="sector">Sector</label>
                    <form:input path="sector" cssClass="form-control" maxlength="80"/>
                </div>
                <div class="col-md-3">
                    <label class="form-label" for="industry">Industry</label>
                    <form:input path="industry" cssClass="form-control" maxlength="120"/>
                </div>
                <div class="col-12">
                    <label class="form-label" for="description">Description</label>
                    <form:textarea path="description" cssClass="form-control" rows="3" maxlength="2000"/>
                </div>
                <div class="col-12">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="active" name="active" value="true" ${form.active ? 'checked' : ''}/>
                        <label class="form-check-label" for="active">Active (visible to users)</label>
                    </div>
                </div>
            </div>
            <div class="d-flex gap-2 mt-3">
                <button type="submit" class="btn btn-primary">${form.id > 0 ? 'Save changes' : 'Add stock'}</button>
                <a class="btn btn-ghost" href="${ctx}/admin/stocks">Cancel</a>
            </div>
        </form:form>
    </c:if>

    <div class="ss-card ss-card-flush">
        <div class="table-responsive">
            <table class="table table-hover ss-table">
                <thead>
                    <tr>
                        <th scope="col">Stock</th>
                        <th scope="col">Exchange</th>
                        <th scope="col" class="d-none d-lg-table-cell">Provider symbol</th>
                        <th scope="col" class="d-none d-md-table-cell">Sector</th>
                        <th scope="col" class="d-none d-md-table-cell">Latest data</th>
                        <th scope="col">Status</th>
                        <th scope="col" class="text-end">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="s" items="${stocks}">
                        <tr>
                            <td>
                                <a class="ss-symbol" href="${ctx}/stocks/${s.id}"><c:out value="${s.symbol}"/></a>
                                <div class="ss-company"><c:out value="${s.companyName}"/></div>
                            </td>
                            <td><span class="ss-badge-source"><c:out value="${s.exchange}"/></span> <span class="small ss-faint"><c:out value="${s.currency}"/></span></td>
                            <td class="d-none d-lg-table-cell mono"><c:out value="${s.apiSymbol}"/></td>
                            <td class="d-none d-md-table-cell small ss-muted"><c:out value="${s.sector}"/></td>
                            <td class="d-none d-md-table-cell small">
                                <c:if test="${not empty s.lastUpdated}">${fmt.date(s.lastUpdated)}</c:if>
                                <ss:dataBadge source="${s.dataSource}"/>
                            </td>
                            <td><span class="small ${s.active ? 'up' : 'down'}">${s.active ? 'Active' : 'Inactive'}</span></td>
                            <td class="text-end text-nowrap">
                                <a class="btn btn-sm btn-ghost" href="${ctx}/admin/stocks?edit=${s.id}">Edit</a>
                                <form class="d-inline m-0" action="${ctx}/admin/stocks/${s.id}/active" method="post">
                                    <sec:csrfInput/>
                                    <input type="hidden" name="active" value="${not s.active}"/>
                                    <button type="submit" class="btn btn-sm ms-1 ${s.active ? 'btn-ghost' : 'btn-success-soft'}">${s.active ? 'Deactivate' : 'Activate'}</button>
                                </form>
                                <form class="d-inline m-0" action="${ctx}/admin/stocks/${s.id}/delete" method="post"
                                      onsubmit="return confirm(this.dataset.confirm);"
                                      data-confirm="Permanently delete ${fn:escapeXml(s.symbol)} with all its prices, recommendations and watchlist entries? Deactivating keeps the history.">
                                    <sec:csrfInput/>
                                    <button type="submit" class="btn btn-sm btn-danger-soft ms-1">Delete</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
    <p class="small ss-faint mt-2">New stocks have no prices until a market data sync stores them. Deactivating hides a stock from users but keeps its history.</p>
</ss:page>
