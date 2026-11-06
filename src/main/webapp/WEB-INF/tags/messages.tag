<%-- Flash / model messages: success, warning, error. --%>
<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${not empty success or not empty error or not empty warning}">
<ul class="ss-messages">
    <c:if test="${not empty success}"><li class="ss-alert ss-alert-info"><c:out value="${success}"/></li></c:if>
    <c:if test="${not empty warning}"><li class="ss-alert ss-alert-warn"><c:out value="${warning}"/></li></c:if>
    <c:if test="${not empty error}"><li class="ss-alert ss-alert-error" role="alert"><c:out value="${error}"/></li></c:if>
</ul>
</c:if>
