<%-- Position of a score between the lowest and highest possible score. --%>
<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="rec" required="true" type="java.lang.Object" %>
<div class="ss-score-track" role="img"
     aria-label="Score ${rec.recommendationScore} on a scale from minus ${rec.maxScore} to plus ${rec.maxScore}">
    <span class="ss-score-dot" style="left: ${rec.scorePosition}%;"></span>
</div>
