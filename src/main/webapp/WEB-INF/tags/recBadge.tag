<%-- BUY / HOLD / SELL / Insufficient data badge. --%>
<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="rec" type="java.lang.Object" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:choose>
    <c:when test="${not empty rec}"><span class="ss-rec ${rec.recommendationType}"><c:out value="${rec.label}"/></span></c:when>
    <c:otherwise><span class="ss-rec INSUFFICIENT_DATA">Not evaluated</span></c:otherwise>
</c:choose>
