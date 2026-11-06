<%-- Change between the two newest stored closes. compact = percentage only. --%>
<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="stock" required="true" type="java.lang.Object" %>
<%@ attribute name="compact" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:choose>
    <c:when test="${empty stock.percentChange}">
        <span class="ss-chip flat" title="Not enough stored prices to compare">&#8212;</span>
    </c:when>
    <c:otherwise>
        <span class="ss-chip ${stock.direction}">
            <i class="bi ${stock.direction == 'up' ? 'bi-caret-up-fill' : (stock.direction == 'down' ? 'bi-caret-down-fill' : 'bi-dash')}" aria-hidden="true"></i>
            <c:choose>
                <c:when test="${compact}"><span>${fmt.percent(stock.percentChange)}</span></c:when>
                <c:otherwise>${fmt.signed(stock.priceChange)} <span>(${fmt.percent(stock.percentChange)})</span></c:otherwise>
            </c:choose>
        </span>
    </c:otherwise>
</c:choose>
