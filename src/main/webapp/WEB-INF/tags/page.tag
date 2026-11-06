<%--
  Page layout used by every JSP:
      <ss:page title="Dashboard"> ...page content... </ss:page>
  Renders <head>, the navigation bar (by role, via Spring Security tags),
  the SAMPLE DATA ribbon, the footer and the scripts around the body.
--%>
<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="title" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!DOCTYPE html>
<html lang="en" data-bs-theme="dark">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title><c:out value="${title}"/> &#183; StockSense</title>
    <meta name="theme-color" content="#0e1014"/>
    <link rel="icon" type="image/svg+xml" href="${ctx}/img/favicon.svg"/>
    <link rel="preconnect" href="https://fonts.googleapis.com"/>
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin="anonymous"/>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&amp;family=JetBrains+Mono:wght@500&amp;family=Space+Grotesk:wght@500;600;700&amp;display=swap"/>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"/>
    <link rel="stylesheet" href="${ctx}/css/style.css"/>
</head>
<body>
<a class="skip-link" href="#main">Skip to content</a>

<c:set var="p" value="${currentPath}"/>
<nav class="navbar navbar-expand-lg ss-navbar sticky-top" aria-label="Main">
    <div class="container-xl">
        <a class="ss-brand navbar-brand me-3"
           href="${ctx}${empty currentUser ? '/' : (currentUser.admin ? '/admin' : '/dashboard')}">
            <span class="ss-brand-mark" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor"
                     stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M3 17l5-5 4 4 8-9"/><path d="M15 7h5v5"/>
                </svg>
            </span>
            <span>StockSense</span>
        </a>

        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#mainNav"
                aria-controls="mainNav" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="mainNav">
            <ul class="navbar-nav me-auto gap-lg-1">
                <sec:authorize access="isAnonymous()">
                    <li class="nav-item"><a class="nav-link ${p == '/' ? 'is-active' : ''}" href="${ctx}/">Home</a></li>
                </sec:authorize>
                <sec:authorize access="hasRole('USER')">
                    <li class="nav-item"><a class="nav-link ${p == '/dashboard' ? 'is-active' : ''}" href="${ctx}/dashboard" aria-current="${p == '/dashboard' ? 'page' : 'false'}">Dashboard</a></li>
                    <li class="nav-item"><a class="nav-link ${fn:startsWith(p, '/stocks') ? 'is-active' : ''}" href="${ctx}/stocks" aria-current="${p == '/stocks' ? 'page' : 'false'}">Stocks</a></li>
                    <li class="nav-item"><a class="nav-link ${p == '/recommendations' ? 'is-active' : ''}" href="${ctx}/recommendations" aria-current="${p == '/recommendations' ? 'page' : 'false'}">Recommendations</a></li>
                    <li class="nav-item"><a class="nav-link ${p == '/watchlist' ? 'is-active' : ''}" href="${ctx}/watchlist" aria-current="${p == '/watchlist' ? 'page' : 'false'}">Watchlist</a></li>
                </sec:authorize>
                <sec:authorize access="hasRole('ADMIN')">
                    <li class="nav-item"><a class="nav-link ${p == '/admin' ? 'is-active' : ''}" href="${ctx}/admin" aria-current="${p == '/admin' ? 'page' : 'false'}">Overview</a></li>
                    <li class="nav-item"><a class="nav-link ${p == '/admin/users' ? 'is-active' : ''}" href="${ctx}/admin/users" aria-current="${p == '/admin/users' ? 'page' : 'false'}">Users</a></li>
                    <li class="nav-item"><a class="nav-link ${p == '/admin/stocks' ? 'is-active' : ''}" href="${ctx}/admin/stocks" aria-current="${p == '/admin/stocks' ? 'page' : 'false'}">Stocks</a></li>
                    <li class="nav-item"><a class="nav-link ${p == '/admin/rules' ? 'is-active' : ''}" href="${ctx}/admin/rules" aria-current="${p == '/admin/rules' ? 'page' : 'false'}">Rules</a></li>
                    <li class="nav-item"><a class="nav-link ${p == '/admin/market-data' ? 'is-active' : ''}" href="${ctx}/admin/market-data" aria-current="${p == '/admin/market-data' ? 'page' : 'false'}">Market data</a></li>
                    <li class="nav-item"><a class="nav-link ${fn:startsWith(p, '/stocks') ? 'is-active' : ''}" href="${ctx}/stocks">Explorer</a></li>
                </sec:authorize>
            </ul>

            <sec:authorize access="isAuthenticated()">
                <%-- A plain GET form: the result is a bookmarkable /stocks?q=... URL. --%>
                <form class="ss-nav-search me-lg-3" role="search" method="get" action="${ctx}/stocks">
                    <label class="visually-hidden" for="navSearch">Search stocks</label>
                    <input id="navSearch" class="form-control form-control-sm" type="search" name="q"
                           placeholder="Search ticker or company" maxlength="60"/>
                </form>

                <div class="dropdown">
                    <button class="btn p-0 border-0 d-flex align-items-center gap-2" type="button"
                            data-bs-toggle="dropdown" aria-expanded="false" aria-label="Account menu">
                        <span class="ss-avatar">${fmt.initials(currentUser.name)}</span>
                        <span class="d-lg-none"><c:out value="${currentUser.name}"/></span>
                    </button>
                    <div class="dropdown-menu dropdown-menu-end p-2" style="min-width: 220px;">
                        <div class="px-2 py-1">
                            <div class="fw-semibold"><c:out value="${currentUser.name}"/></div>
                            <div class="small ss-muted text-truncate"><c:out value="${currentUser.email}"/></div>
                            <span class="ss-role-pill mt-1 d-inline-block">${currentUser.role}</span>
                        </div>
                        <hr class="dropdown-divider"/>
                        <a class="dropdown-item rounded" href="${ctx}/profile"><i class="bi bi-person me-2" aria-hidden="true"></i>Profile</a>
                        <form action="${ctx}/logout" method="post" class="m-0">
                            <sec:csrfInput/>
                            <button type="submit" class="dropdown-item rounded"><i class="bi bi-box-arrow-right me-2" aria-hidden="true"></i>Log out</button>
                        </form>
                    </div>
                </div>
            </sec:authorize>

            <sec:authorize access="isAnonymous()">
                <div class="d-flex gap-2">
                    <a class="btn btn-sm btn-ghost" href="${ctx}/login">Log in</a>
                    <a class="btn btn-sm btn-primary" href="${ctx}/register">Create account</a>
                </div>
            </sec:authorize>
        </div>
    </div>
</nav>

<c:if test="${market.demoMode}">
<div class="ss-sample-ribbon" role="note">
    <div class="container-xl d-flex align-items-center gap-2">
        <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
        <span><strong>SAMPLE DATA mode.</strong> Prices, charts and recommendations use synthetic demonstration data, not real market prices.</span>
    </div>
</div>
</c:if>

<main class="ss-main" id="main"><div class="container-xl">
<jsp:doBody/>
</div></main>

<footer class="ss-footer">
    <div class="container-xl d-flex flex-wrap justify-content-between gap-2">
        <span>StockSense &#183; for education and information only &#8212; not investment advice. Past movements do not guarantee future results.</span>
        <span>Built with Spring Boot &#183; Spring MVC &#183; JSP + JSTL &#183; JdbcTemplate &#183; MySQL &#183; Data: <c:out value="${market.providerLabel}"/></span>
    </div>
</footer>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
<script src="${ctx}/js/charts.js"></script>
</body>
</html>
