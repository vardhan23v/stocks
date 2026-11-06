<%-- Shown by GlobalModelAdvice when the database cannot be reached. --%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="Database unavailable">
    <div class="ss-card ss-empty">
        <div class="ss-empty-icon"><i class="bi bi-database-x" aria-hidden="true"></i></div>
        <h1 class="h4">The database is not reachable</h1>
        <p>StockSense could not load its data. Check that MySQL is running and that DB_URL, DB_USER and DB_PASSWORD are correct, then try again.</p>
        <a class="btn btn-ghost" href="${ctx}/">Back to the start page</a>
    </div>
</ss:page>
