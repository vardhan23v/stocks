<%-- Spring Boot's error view (404, 403 such as an expired form, 500). No stack traces are shown. --%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="${status == 404 ? 'Page not found' : 'Something went wrong'}">
    <div class="ss-card ss-empty">
        <div class="ss-empty-icon"><i class="bi bi-exclamation-circle" aria-hidden="true"></i></div>
        <h1 class="h4">${status == 404 ? 'Page not found' : (status == 403 ? 'This form has expired' : 'Something went wrong')}</h1>
        <c:choose>
            <c:when test="${status == 404}"><p>The page you asked for does not exist.</p></c:when>
            <c:when test="${status == 403}"><p>The page was open too long or your session ended. Go back, reload the page and try again.</p></c:when>
            <c:otherwise><p>An unexpected error occurred. Please try again.</p></c:otherwise>
        </c:choose>
        <a class="btn btn-ghost" href="${ctx}/">Back to the start page</a>
    </div>
</ss:page>
