<%-- Provenance of a price: SAMPLE DATA or the provider. Never "live". --%>
<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="source" type="java.lang.Object" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:choose>
    <c:when test="${empty source}"><span class="ss-badge-source">No data</span></c:when>
    <c:when test="${source == 'SAMPLE'}"><span class="ss-badge-sample" title="Synthetic demonstration data - not a real market price">Sample data</span></c:when>
    <c:otherwise><span class="ss-badge-source" title="Delayed end-of-day data from the configured provider"><c:out value="${source == 'ALPHA_VANTAGE' ? 'Alpha Vantage · delayed' : source}"/></span></c:otherwise>
</c:choose>
