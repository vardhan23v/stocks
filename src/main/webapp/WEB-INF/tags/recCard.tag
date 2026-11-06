<%-- A recommendation card. allReasons = list every reason, otherwise only the first. --%>
<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="r" required="true" type="java.lang.Object" %>
<%@ attribute name="allReasons" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ss" tagdir="/WEB-INF/tags" %>
<article class="ss-rec-card ${r.recommendationType}">
    <div class="d-flex justify-content-between align-items-start gap-2">
        <div class="text-truncate">
            <a class="ss-symbol fs-5" href="${ctx}/stocks/${r.stockId}"><c:out value="${r.symbol}"/></a>
            <span class="ss-faint small ms-1"><c:out value="${r.exchange}"/></span>
            <div class="ss-company text-truncate"><c:out value="${r.companyName}"/></div>
        </div>
        <ss:recBadge rec="${r}"/>
    </div>
    <c:if test="${not r.insufficient}">
        <div>
            <div class="d-flex justify-content-between small ss-faint mb-1"><span>SELL</span><span>Score <span class="num ss-muted">${r.recommendationScore}</span></span><span>BUY</span></div>
            <ss:scoreBar rec="${r}"/>
        </div>
    </c:if>
    <c:choose>
        <c:when test="${allReasons}">
            <ul class="ss-reasons">
                <c:forEach var="reason" items="${r.reasons}"><li><c:out value="${reason}"/></li></c:forEach>
            </ul>
        </c:when>
        <c:when test="${not empty r.reasons}">
            <p class="small ss-muted mb-0 text-truncate" title="${fn:escapeXml(r.reasons[0])}"><c:out value="${r.reasons[0]}"/></p>
        </c:when>
    </c:choose>
    <div class="small ss-faint mt-auto">
        Generated ${fmt.dateTime(r.generatedAt)}
        &#183; data of ${fmt.date(r.dataTimestamp)}
        <c:if test="${r.sampleData}"> &#183; <span class="ss-badge-sample">Sample</span></c:if>
    </div>
</article>
