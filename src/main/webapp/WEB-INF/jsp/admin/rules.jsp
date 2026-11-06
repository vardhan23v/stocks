<%--
  Recommendation rules (AdminRulesController). The rules table is one
  <form:form> bound to RulesForm: rules[0].weight, rules[0].enabled, ...
--%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="Recommendation rules · Admin">
    <ss:messages/>

    <div class="ss-page-head">
        <div>
            <p class="ss-eyebrow">Administrator</p>
            <h1>Recommendation rules</h1>
        </div>
        <form action="${ctx}/admin/rules/regenerate" method="post">
            <sec:csrfInput/>
            <button type="submit" class="btn btn-ghost">Re-evaluate all stocks</button>
        </form>
    </div>

    <form:form modelAttribute="rulesForm" servletRelativeAction="/admin/rules" method="post">
        <div class="ss-card ss-card-flush mb-4">
            <div class="ss-card-head">
                <h2>Scoring rules</h2>
                <span class="small ss-faint">Signal (+1 / 0 / -1) x weight, summed</span>
            </div>
            <div class="table-responsive mt-2">
                <table class="table ss-table">
                    <thead>
                        <tr>
                            <th scope="col">Rule</th>
                            <th scope="col" style="width: 110px;">Weight</th>
                            <th scope="col" style="width: 120px;">Low param</th>
                            <th scope="col" style="width: 120px;">High param</th>
                            <th scope="col">Enabled</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="rule" items="${rulesForm.rules}" varStatus="i">
                            <tr>
                                <td>
                                    <form:hidden path="rules[${i.index}].id"/>
                                    <div class="fw-semibold"><c:out value="${rule.name}"/> <span class="mono ss-faint"><c:out value="${rule.ruleKey}"/></span></div>
                                    <div class="small ss-muted"><c:out value="${rule.description}"/></div>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${rule.ruleKey == 'DECISION'}"><span class="ss-faint">-</span></c:when>
                                        <c:otherwise>
                                            <form:input path="rules[${i.index}].weight" cssClass="form-control form-control-sm num"
                                                        inputmode="decimal" aria-label="Weight of ${rule.name}"/>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:if test="${rule.ruleKey != 'MACD'}">
                                        <form:input path="rules[${i.index}].paramLow" cssClass="form-control form-control-sm num"
                                                    inputmode="decimal" aria-label="Low parameter of ${rule.name}"/>
                                    </c:if>
                                </td>
                                <td>
                                    <c:if test="${rule.ruleKey == 'RSI' or rule.ruleKey == 'DECISION'}">
                                        <form:input path="rules[${i.index}].paramHigh" cssClass="form-control form-control-sm num"
                                                    inputmode="decimal" aria-label="High parameter of ${rule.name}"/>
                                    </c:if>
                                </td>
                                <td>
                                    <c:if test="${rule.ruleKey != 'DECISION'}">
                                        <div class="form-check form-switch">
                                            <form:checkbox path="rules[${i.index}].enabled" cssClass="form-check-input"
                                                           aria-label="${rule.name} enabled"/>
                                        </div>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
            <div class="p-3 pt-0 d-flex flex-wrap gap-2 align-items-center">
                <button type="submit" class="btn btn-primary">Save rules and re-evaluate</button>
                <span class="small ss-faint">Weights 0-100. RSI: low &lt; high within 0-100. Decision: SELL threshold below 0, BUY threshold above 0.</span>
            </div>
        </div>
    </form:form>

    <div class="ss-card ss-card-flush">
        <div class="ss-card-head"><h2>Generated recommendations</h2><span class="small ss-faint">Newest 40</span></div>
        <div class="table-responsive mt-2 ss-scroll">
            <table class="table table-sm ss-table">
                <thead>
                    <tr>
                        <th scope="col">Stock</th>
                        <th scope="col">Result</th>
                        <th scope="col" class="num text-end">Score</th>
                        <th scope="col" class="d-none d-md-table-cell">Data of</th>
                        <th scope="col" class="d-none d-md-table-cell">Source</th>
                        <th scope="col" class="d-none d-lg-table-cell">Generated</th>
                        <th scope="col" class="text-end"><span class="visually-hidden">Actions</span></th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="r" items="${recent}">
                        <tr>
                            <td><span class="ss-symbol"><c:out value="${r.symbol}"/></span> <span class="small ss-faint"><c:out value="${r.exchange}"/></span></td>
                            <td><ss:recBadge rec="${r}"/></td>
                            <td class="num text-end">${r.recommendationScore}</td>
                            <td class="d-none d-md-table-cell small">${fmt.date(r.dataTimestamp)}</td>
                            <td class="d-none d-md-table-cell"><ss:dataBadge source="${r.dataSource}"/></td>
                            <td class="d-none d-lg-table-cell small ss-muted">${fmt.time(r.generatedAt)}</td>
                            <td class="text-end">
                                <form class="m-0" action="${ctx}/admin/recommendations/${r.id}/delete" method="post"
                                      onsubmit="return confirm('Delete this recommendation? A new one is generated when the stock is next viewed.');">
                                    <sec:csrfInput/>
                                    <button type="submit" class="btn btn-sm btn-danger-soft">Delete</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</ss:page>
